package com.hollowgrin.entity;

import com.hollowgrin.HollowConfig;
import com.hollowgrin.HollowGrinMod;
import com.hollowgrin.registry.ModSounds;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerDataHolder;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Mr. Hollowgrin - "The Gracious Host".
 *
 * One entity, three bodies:
 *   DISGUISE / REVEAL  -> a villager that is slightly wrong
 *   HOST / STUNNED / RETREAT -> his true fat, top-hatted, grinning body
 *   CHASE / GRAB       -> the Maw: jaw unhinged, long gloved arms reaching out of the mouth
 *
 * Nothing here reads or writes any file; the entity does not even save its own state
 * (after a reload it simply re-acquires the nearest player or quietly disappears).
 */
public class HollowGrinEntity extends Monster implements VillagerDataHolder {
    public static final int DISGUISE = 0;
    public static final int REVEAL = 1;
    public static final int HOST = 2;
    public static final int CHASE = 3;
    public static final int GRAB = 4;
    public static final int STUNNED = 5;
    public static final int RETREAT = 6;
    /** after you break free: he dissolves into the true body, applauds, throws a diamond, tips his hat, vanishes. */
    public static final int FAREWELL = 7;

    private static final EntityDataAccessor<Integer> STATE =
            SynchedEntityData.defineId(HollowGrinEntity.class, EntityDataSerializers.INT);

    /** the villager look he copies (biome clothes + profession + level badge), synced to clients. */
    private static final EntityDataAccessor<VillagerData> VILLAGER =
            SynchedEntityData.defineId(HollowGrinEntity.class, EntityDataSerializers.VILLAGER_DATA);

    /** false (normal): villager body with an opening mouth. true: optional fat top-hatted body (command only). */
    private static final EntityDataAccessor<Boolean> TRUE_FORM =
            SynchedEntityData.defineId(HollowGrinEntity.class, EntityDataSerializers.BOOLEAN);

    /** 0..100: how close the victim is to wrenching free (synced for the HUD bar). */
    private static final EntityDataAccessor<Integer> STRUGGLE =
            SynchedEntityData.defineId(HollowGrinEntity.class, EntityDataSerializers.INT);

    private static final ResourceKey<DamageType> SQUEEZE =
            ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(HollowGrinMod.MODID, "squeeze"));

    // timings (ticks)
    private static final int REVEAL_TICKS = 30;
    private static final int HOST_TICKS = 45;
    private static final int GRAB_GOTCHA_AT = 14;
    public static final int GRAB_SQUEEZE_FROM = 36;   // screen starts to darken
    public static final int GRAB_KILL_AT = 140;       // screen fully black = dead
    private static final float STRUGGLE_GAIN = 6.0F;  // per Spacebar press (max ~10 presses/s accepted)
    private static final float STRUGGLE_DECAY = 0.55F;// per tick, after a short grace period

    private int stateTicks;          // ticks spent in current state (both sides)
    private int stare;               // accumulated "being looked at" ticks
    private int closeTicks;          // ticks spent right next to the player while disguised
    private int lungeCooldown;
    private int noTargetTicks;
    private int lifeTicks;
    private float grabDamageTaken;
    private boolean releasing;
    private boolean variantSet;
    private float struggleF;
    private int lastStrugglePress = -10;
    private UUID targetId;

    public HollowGrinEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.setPersistenceRequired();
        if (this.getNavigation() instanceof GroundPathNavigation nav) {
            nav.setCanOpenDoors(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 120.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.26D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D);
    }

    // ------------------------------------------------------------------ data / basics

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STATE, DISGUISE);
        this.entityData.define(TRUE_FORM, false);
        this.entityData.define(STRUGGLE, 0);
        this.entityData.define(VILLAGER, new VillagerData(VillagerType.PLAINS, VillagerProfession.NONE, 1));
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (STATE.equals(key)) {
            this.stateTicks = 0;
        }
        if (STATE.equals(key) || TRUE_FORM.equals(key)) {
            this.refreshDimensions();
        }
    }

    @Override
    public VillagerData getVillagerData() {
        return this.entityData.get(VILLAGER);
    }

    @Override
    public void setVillagerData(VillagerData data) {
        this.entityData.set(VILLAGER, data);
        this.variantSet = true;
    }

    public int getState() {
        return this.entityData.get(STATE);
    }

    private void setState(int state) {
        if (getState() != state) {
            this.entityData.set(STATE, state);
            this.stateTicks = 0;
            if (state != GRAB) {
                this.struggleF = 0.0F;
                this.entityData.set(STRUGGLE, 0);
            }
        }
    }

    public int getStateTicks() {
        return this.stateTicks;
    }

    public int getStruggle() {
        return this.entityData.get(STRUGGLE);
    }

    /** called (server side) for every Spacebar press of the held player. */
    public void onStruggle(ServerPlayer p) {
        if (getState() != GRAB || this.releasing || p.getVehicle() != this || this.stateTicks < 8) {
            return;
        }
        if (this.tickCount - this.lastStrugglePress < 2) {
            return;      // ignore macros / auto-clickers faster than 10 presses per second
        }
        this.lastStrugglePress = this.tickCount;
        this.struggleF = Math.min(100.0F, this.struggleF + STRUGGLE_GAIN);
        this.entityData.set(STRUGGLE, (int) this.struggleF);
        if (this.struggleF >= 100.0F) {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 1));   // a few seconds to run
            breakGrab();
        }
    }

    public boolean isTrueForm() {
        return this.entityData.get(TRUE_FORM);
    }

    /** optional second body: starts already revealed. */
    public void startAsTrueForm() {
        this.entityData.set(TRUE_FORM, true);
        setState(HOST);
    }

    public boolean isDisguised() {
        int s = getState();
        return s == DISGUISE || s == REVEAL;
    }

    public void setTargetPlayer(Player player) {
        this.targetId = player.getUUID();
    }

    @Override
    public EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose) {
        return isTrueForm() ? EntityDimensions.scalable(1.0F, 2.9F) : EntityDimensions.scalable(0.6F, 1.95F);
    }

    @Override
    protected float getStandingEyeHeight(net.minecraft.world.entity.Pose pose, EntityDimensions dims) {
        return dims.height * 0.85F;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new OpenDoorGoal(this, false));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.45D) {
            @Override
            public boolean canUse() {
                return getState() == DISGUISE && targetId == null && super.canUse();
            }
        });
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tick() {
        super.tick();
        this.stateTicks++;
        if (this.level().isClientSide) {
            int s = getState();
            if (isTrueForm() && (s == HOST || s == CHASE || s == GRAB) && this.random.nextInt(6) == 0) {
                this.level().addParticle(ParticleTypes.SMOKE, getRandomX(0.4D), getY() + getBbHeight() * 0.6D,
                        getRandomZ(0.4D), 0.0D, 0.02D, 0.0D);
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        serverLogic();
    }

    private boolean validTarget(Player p) {
        return p instanceof ServerPlayer && p.isAlive() && !p.isCreative() && !p.isSpectator()
                && p.level() == this.level();
    }

    private ServerPlayer findTarget() {
        if (this.targetId != null) {
            Player p = this.level().getPlayerByUUID(this.targetId);
            if (p != null && validTarget(p)) {
                return (ServerPlayer) p;
            }
            this.targetId = null;
        }
        Player near = this.level().getNearestPlayer(this, 80.0D);
        if (near != null && validTarget(near)) {
            this.targetId = near.getUUID();
            return (ServerPlayer) near;
        }
        return null;
    }

    private void serverLogic() {
        this.lifeTicks++;
        if (!this.variantSet) {
            // no villager to copy (spawn egg): dress like the local villagers
            setVillagerData(new VillagerData(VillagerType.byBiome(this.level().getBiome(this.blockPosition())),
                    VillagerProfession.NONE, 1));
        }
        int s = getState();

        // safety: nobody may ride him unless he is really holding them
        if (s != GRAB && this.isVehicle()) {
            this.ejectPassengers();
        }

        ServerPlayer t = findTarget();
        if (t == null) {
            if (s == GRAB) {
                releaseNow();
                setState(RETREAT);
            }
            if (++this.noTargetTicks > 100) {
                vanish(false);
            }
            return;
        }
        this.noTargetTicks = 0;

        double dist = this.distanceTo(t);
        if (dist > 110.0D || this.lifeTicks > 20 * 60 * 25) {
            vanish(false);
            return;
        }

        switch (s) {
            case DISGUISE -> tickDisguise(t, dist);
            case REVEAL -> tickReveal(t);
            case HOST -> tickHost(t);
            case CHASE -> tickChase(t, dist);
            case GRAB -> tickGrab(t);
            case STUNNED -> tickStunned();
            case RETREAT -> tickRetreat();
            case FAREWELL -> tickFarewell(t);
            default -> setState(DISGUISE);
        }
    }

    // ------------------------------------------------------------------ DISGUISE

    private void tickDisguise(ServerPlayer t, double dist) {
        if (this.tickCount % 12 == 0) {
            this.getNavigation().moveTo(t, 0.42D * HollowConfig.CHASE_SPEED.get());
        }

        boolean looked = isBeingLookedAt(t, dist);
        if (looked) {
            this.stare++;
        } else if (this.stare > 0 && this.tickCount % 2 == 0) {
            this.stare--;
        }

        int limit = Math.max(30, (int) (HollowConfig.STARE_SECONDS.get() * 20 * Mth.clamp(dist / 20.0D, 0.4D, 1.0D)));

        // the closer you are to being caught, the louder your own heart gets
        if (this.stare > 15 && this.tickCount % 18 == 0) {
            float k = Mth.clamp((float) this.stare / (float) limit, 0.0F, 1.0F);
            t.playNotifySound(ModSounds.HEARTBEAT.get(), SoundSource.HOSTILE, 0.35F + 0.9F * k, 0.8F + 0.5F * k);
        }
        if (this.stare >= limit) {
            beginReveal();
            return;
        }

        // standing right behind you for too long is the same as being stared at
        if (dist < 2.4D) {
            if (++this.closeTicks > 50) {
                beginReveal();
                return;
            }
        } else {
            this.closeTicks = 0;
        }

        // tells: he hums a lullaby, and real villagers refuse to look at him
        if (this.tickCount % 90 == 0 && dist < 26.0D && this.random.nextBoolean()) {
            this.playSound(ModSounds.HUM.get(), 0.5F, 0.92F + this.random.nextFloat() * 0.16F);
        }
        if (this.tickCount % 50 == 0) {
            unsettleVillagers();
        }
    }

    private void unsettleVillagers() {
        for (Villager v : this.level().getEntitiesOfClass(Villager.class, this.getBoundingBox().inflate(9.0D))) {
            v.setUnhappyCounter(40);
            if (this.random.nextInt(3) == 0) {
                v.playSound(SoundEvents.VILLAGER_NO, 1.0F, 1.0F);
            }
        }
    }

    private boolean isBeingLookedAt(ServerPlayer p, double dist) {
        if (dist > 48.0D) {
            return false;
        }
        Vec3 view = p.getViewVector(1.0F);
        Vec3 to = new Vec3(this.getX() - p.getX(), this.getY() + this.getBbHeight() * 0.7D - p.getEyeY(),
                this.getZ() - p.getZ());
        double len = to.length();
        if (len < 0.001D) {
            return true;
        }
        double dot = view.dot(to.scale(1.0D / len));
        return dot > 0.965D && p.hasLineOfSight(this);   // roughly a 15 degree cone around the crosshair
    }

    // ------------------------------------------------------------------ REVEAL -> HOST -> CHASE

    private void beginReveal() {
        this.getNavigation().stop();
        setState(REVEAL);
        this.playSound(ModSounds.REVEAL.get(), 1.6F, 1.0F);
    }

    private void tickReveal(ServerPlayer t) {
        this.getNavigation().stop();
        this.getLookControl().setLookAt(t, 40.0F, 40.0F);
        if (this.level() instanceof ServerLevel sl && this.stateTicks % 5 == 0) {
            sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1.0D, getZ(), 3, 0.2D, 0.4D, 0.2D, 0.01D);
        }
        if (this.stateTicks >= REVEAL_TICKS) {
            // no transformation: the villager's own jaw simply unhinges
            setState(CHASE);
            this.playSound(ModSounds.JAW.get(), 1.8F, 0.9F);
            this.playSound(ModSounds.LAUGH.get(), 1.6F, 0.8F);
        }
    }

    private void tickHost(ServerPlayer t) {
        this.getNavigation().stop();
        this.getLookControl().setLookAt(t, 40.0F, 40.0F);
        if (this.stateTicks >= HOST_TICKS) {
            setState(CHASE);
            this.playSound(ModSounds.JAW.get(), 1.6F, 1.0F);
        }
    }

    // ------------------------------------------------------------------ CHASE

    private void tickChase(ServerPlayer t, double dist) {
        this.getLookControl().setLookAt(t, 40.0F, 40.0F);
        if (this.tickCount % 4 == 0) {
            this.getNavigation().moveTo(t, 1.0D * HollowConfig.CHASE_SPEED.get());
        }

        if (this.lungeCooldown > 0) {
            this.lungeCooldown--;
        } else if (dist < 7.0D && dist > 3.0D && this.onGround() && this.hasLineOfSight(t)) {
            Vec3 d = t.position().subtract(this.position()).normalize();
            this.setDeltaMovement(d.x * 0.75D, 0.32D, d.z * 0.75D);
            this.hasImpulse = true;
            this.lungeCooldown = 50;
        }

        if (dist < 2.9D && Math.abs(t.getY() - this.getY()) < 3.0D && this.hasLineOfSight(t)) {
            startGrab(t);
            return;
        }
        if (this.stateTicks > 20 * 40) {
            // you got away
            vanish(true);
        }
    }

    // ------------------------------------------------------------------ GRAB

    private void startGrab(ServerPlayer t) {
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        this.grabDamageTaken = 0.0F;
        this.struggleF = 0.0F;
        this.entityData.set(STRUGGLE, 0);
        this.releasing = false;
        t.stopRiding();
        if (t.startRiding(this, true)) {
            setState(GRAB);
            this.playSound(ModSounds.JAW.get(), 1.6F, 0.9F);
            t.lookAt(EntityAnchorArgument.Anchor.EYES, this.getEyePosition());
        }
    }

    private void tickGrab(ServerPlayer t) {
        int g = this.stateTicks;
        this.getNavigation().stop();
        this.getLookControl().setLookAt(t, 60.0F, 60.0F);

        if (!t.isAlive()) {
            releaseNow();
            setState(RETREAT);
            return;
        }
        if (t.getVehicle() != this) {
            if (!this.releasing) {
                t.startRiding(this, true);     // he does not let go
            }
        }
        if (g == 2 || g % 25 == 0) {
            t.lookAt(EntityAnchorArgument.Anchor.EYES, this.getEyePosition());   // you will look at the grin
        }
        if (g == GRAB_GOTCHA_AT) {
            this.playSound(ModSounds.GOTCHA.get(), 2.2F, 1.0F);
        }
        if (g > 10 && this.struggleF > 0.0F) {
            this.struggleF = Math.max(0.0F, this.struggleF - STRUGGLE_DECAY);
            this.entityData.set(STRUGGLE, (int) this.struggleF);
        }
        if (g >= GRAB_SQUEEZE_FROM && (g - GRAB_SQUEEZE_FROM) % 14 == 0) {
            this.playSound(ModSounds.SQUEEZE.get(), 1.6F, 0.9F + this.random.nextFloat() * 0.2F);   // sound only: HP stays untouched
        }
        if (g >= GRAB_KILL_AT) {
            finishGrab(t);
        }
    }

    private void finishGrab(ServerPlayer t) {
        releaseNow();
        if (HollowConfig.LETHAL.get()) {
            t.hurt(squeezeSource(), Float.MAX_VALUE);
        } else {
            t.hurt(this.level().damageSources().mobAttack(this), 8.0F);
        }
        this.playSound(ModSounds.LAUGH.get(), 2.0F, 0.9F);
        setState(RETREAT);
    }

    private DamageSource squeezeSource() {
        try {
            return new DamageSource(this.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(SQUEEZE), this);
        } catch (RuntimeException ex) {
            return this.level().damageSources().genericKill();   // fallback if the datapack part is missing
        }
    }

    private void releaseNow() {
        this.releasing = true;
        this.ejectPassengers();
    }

    /** true while the Maw is really holding someone (used by the mount-event handler). */
    public boolean shouldHoldPassenger() {
        return this.isAlive() && !this.isRemoved() && getState() == GRAB && !this.releasing;
    }

    /** You wrenched free (or hurt him enough): he throws you off, dissolves into his true body and says goodbye. */
    private void breakGrab() {
        ServerPlayer t = findTarget();
        releaseNow();
        if (t != null) {
            Vec3 away = t.position().subtract(this.position());
            away = new Vec3(away.x, 0.0D, away.z);
            if (away.lengthSqr() < 1.0E-4D) {
                double yaw = Math.toRadians(this.getYRot());
                away = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
            }
            away = away.normalize();
            t.setDeltaMovement(away.x * 1.0D, 0.45D, away.z * 1.0D);   // thrown out of his hands
            t.hurtMarked = true;
            t.fallDistance = 0.0F;
        }
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.0D, getZ(), 40, 0.5D, 0.9D, 0.5D, 0.03D);
        }
        this.getNavigation().stop();
        this.entityData.set(TRUE_FORM, true);      // the villager shell dissolves
        setState(FAREWELL);
        this.playSound(ModSounds.REVEAL.get(), 1.6F, 1.2F);
    }

    // ------------------------------------------------------------------ FAREWELL (timeline in ticks)
    //   0-10 recoil | 12-60 applause | 62-88 throws a diamond (released at 80) | 88-150 tips his hat, bows
    //   140-160 smoke | 160 gone

    private void tickFarewell(ServerPlayer t) {
        this.getNavigation().stop();
        this.getLookControl().setLookAt(t, 40.0F, 40.0F);
        int k = this.stateTicks;
        if (k >= 12 && k <= 62 && (k - 10) % 6 == 2) {
            this.playSound(ModSounds.CLAP.get(), 1.2F, 0.95F + this.random.nextFloat() * 0.1F);
        }
        if (k == 80) {
            throwDiamond(t);
        }
        if (k == 140) {
            this.playSound(SoundEvents.FIRECHARGE_USE, 0.9F, 0.8F);
        }
        if (k >= 140 && k < 160 && k % 2 == 0 && this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                    6, 0.6D, 0.9D, 0.6D, 0.02D);
        }
        if (k >= 160) {
            vanish(true);
        }
    }

    private void throwDiamond(ServerPlayer t) {
        Vec3 from = new Vec3(getX(), getY() + getBbHeight() * 0.55D, getZ());
        Vec3 to = t.position().add(0.0D, t.getBbHeight() * 0.6D, 0.0D);
        Vec3 d = to.subtract(from);
        double horiz = Math.sqrt(d.x * d.x + d.z * d.z);
        Vec3 dir = horiz < 0.001D ? Vec3.ZERO : new Vec3(d.x / horiz, 0.0D, d.z / horiz);
        double speed = Mth.clamp(horiz * 0.09D, 0.15D, 0.5D);
        ItemEntity item = new ItemEntity(this.level(), from.x, from.y, from.z, new ItemStack(Items.DIAMOND, 1));
        item.setDeltaMovement(dir.x * speed, 0.22D, dir.z * speed);
        item.setPickUpDelay(15);
        this.level().addFreshEntity(item);
        this.playSound(SoundEvents.SNOWBALL_THROW, 1.0F, 0.9F);
    }

    // ------------------------------------------------------------------ STUNNED / RETREAT

    private void tickStunned() {
        this.getNavigation().stop();
        if (this.stateTicks >= 60) {
            setState(RETREAT);
            this.playSound(ModSounds.LAUGH.get(), 1.8F, 0.8F);
        }
    }

    private void tickRetreat() {
        this.getNavigation().stop();
        if (this.stateTicks >= 70) {
            vanish(true);
        }
    }

    public void vanish(boolean smoke) {
        if (smoke && this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                    30, 0.4D, 0.8D, 0.4D, 0.02D);
        }
        this.releasing = true;
        this.ejectPassengers();
        this.discard();
    }

    // ------------------------------------------------------------------ interaction / damage

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide) {
            return super.hurt(source, amount);
        }
        int before = getState();
        if (before == FAREWELL) {
            return false;
        }
        if (before == DISGUISE && source.getEntity() instanceof Player) {
            beginReveal();
        }
        boolean result = super.hurt(source, amount);
        if (result && before == GRAB) {
            this.grabDamageTaken += amount;
            if (this.grabDamageTaken >= HollowConfig.GRAB_BREAK_DAMAGE.get()) {
                breakGrab();
            }
        }
        return result;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isTrueForm() && getState() == DISGUISE) {
            // exactly what an unemployed villager does when you try to trade
            this.playSound(SoundEvents.VILLAGER_NO, 1.0F, 1.0F);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return InteractionResult.PASS;
    }

    // ------------------------------------------------------------------ riding

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        if (!this.hasPassenger(passenger)) {
            return;
        }
        double yaw = Math.toRadians(this.getYRot());
        double fx = -Math.sin(yaw);
        double fz = Math.cos(yaw);
        double lift = 0.2D + 1.0D * Math.min(1.0D, this.stateTicks / 20.0D);
        callback.accept(passenger, this.getX() + fx * (isTrueForm() ? 1.7D : 1.6D), this.getY() + lift, this.getZ() + fz * (isTrueForm() ? 1.7D : 1.6D));
    }

    @Override
    public boolean shouldRiderSit() {
        return false;   // the victim dangles instead of sitting
    }

    // ------------------------------------------------------------------ sounds

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return !isTrueForm() ? SoundEvents.VILLAGER_HURT : SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.LAUGH.get();
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }
}
