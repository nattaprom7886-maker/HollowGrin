package com.hollowgrin;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

/**
 * The dog trick (server side).
 *
 *  1. Your tamed dog sits (ordered to stay) inside a village house.          -> the dog is "marked"
 *  2. You walk far away (outside / mining) for a while and come back.          -> the dog is gone the moment you are not looking at it
 *  3. You go into the house, then step back outside to look for it.            -> Mr. Hollowgrin stands outside with his back to you
 *  4. His head turns, then his body, and he holds up your dog's head, then he vanishes (see HollowGrinEntity.DOG_*).
 *  5. From then on you can never tame a dog again.
 *
 * Only tiny markers are stored, in the dog's own data and in the player's own data (the normal Minecraft way).
 * No file is touched.
 */
public class DogEvents {
    private static final String PERSISTED = "PlayerPersisted";
    private static final String CURSE = "hollowgrin_no_dogs";
    private static final String STAGE = "hollowgrin_dog_stage";    // 0 none, 1 dog gone (wait for you to go in), 2 wait for you to come out
    private static final String HOUSE = "hollowgrin_dog_house";    // BlockPos of the empty seat
    private static final String W_MARK = "hg_dog_wait";
    private static final String W_AWAY = "hg_dog_away";

    private static final double AWAY_BLOCKS = 28.0D;
    private static final double RETURN_BLOCKS = 16.0D;

    private static CompoundTag persisted(ServerPlayer p) {
        CompoundTag root = p.getPersistentData();
        CompoundTag c = root.getCompound(PERSISTED);
        root.put(PERSISTED, c);
        return c;
    }

    public static boolean isCursed(ServerPlayer p) {
        return persisted(p).getBoolean(CURSE);
    }

    /** the player can never tame a dog again. */
    public static void curse(ServerPlayer p) {
        persisted(p).putBoolean(CURSE, true);
    }

    /** test command: undo everything. */
    public static void lift(ServerPlayer p) {
        CompoundTag c = persisted(p);
        c.remove(CURSE);
        c.remove(STAGE);
        c.remove(HOUSE);
    }

    /** a roofed place with a village (bed / workstation / bell...) close by = a village house. */
    private static boolean inVillageHouse(ServerLevel level, BlockPos pos) {
        if (level.canSeeSky(pos)) {
            return false;
        }
        return level.getPoiManager().findClosest(h -> h.is(PoiTypeTags.VILLAGE), pos, 24,
                PoiManager.Occupancy.ANY).isPresent();
    }

    private static boolean isWatching(ServerPlayer p, Wolf wolf) {
        Vec3 view = p.getViewVector(1.0F);
        Vec3 to = wolf.getEyePosition().subtract(p.getEyePosition());
        double len = to.length();
        if (len < 0.001D) {
            return true;
        }
        return view.dot(to.scale(1.0D / len)) > 0.4D && p.hasLineOfSight(wolf);
    }

    // ------------------------------------------------------------------ the dog

    @SubscribeEvent
    public void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Wolf wolf) || wolf.level().isClientSide || wolf.tickCount % 20 != 7) {
            return;
        }
        if (!(wolf.level() instanceof ServerLevel level)) {
            return;
        }
        CompoundTag data = wolf.getPersistentData();
        if (!HollowConfig.ENABLED.get() || !HollowConfig.DOG_TRICK.get()
                || level.dimension() != Level.OVERWORLD || level.getDifficulty() == Difficulty.PEACEFUL
                || !wolf.isTame() || !wolf.isOrderedToSit()) {
            if (data.contains(W_MARK)) {          // stood up again: forget
                data.remove(W_MARK);
                data.remove(W_AWAY);
            }
            return;
        }
        if (!(wolf.getOwner() instanceof ServerPlayer owner) || owner.level() != level
                || owner.isCreative() || owner.isSpectator() || isCursed(owner)) {
            return;
        }
        if (persisted(owner).getInt(STAGE) != 0) {
            return;                               // one trick at a time
        }

        BlockPos pos = wolf.blockPosition();
        if (!data.getBoolean(W_MARK)) {
            if (inVillageHouse(level, pos)) {
                data.putBoolean(W_MARK, true);
                data.putLong(W_AWAY, 0L);
            }
            return;
        }

        double d = wolf.distanceTo(owner);
        long now = level.getGameTime();
        long away = data.getLong(W_AWAY);
        if (d > AWAY_BLOCKS) {
            if (away == 0L) {
                data.putLong(W_AWAY, now);        // you left
            }
            return;
        }
        if (away == 0L) {
            return;
        }
        if (now - away < HollowConfig.DOG_AWAY_SECONDS.get() * 20L) {
            data.putLong(W_AWAY, 0L);             // you only stepped out for a moment
            return;
        }
        if (d > RETURN_BLOCKS || isWatching(owner, wolf)) {
            return;                               // he takes it only when you are close and not looking
        }

        CompoundTag pd = persisted(owner);
        pd.putInt(STAGE, 1);
        pd.putLong(HOUSE, pos.asLong());
        wolf.discard();                           // no sound, no smoke: the seat is simply empty
    }

    // ------------------------------------------------------------------ the player

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER
                || !(event.player instanceof ServerPlayer p) || p.tickCount % 10 != 3) {
            return;
        }
        CompoundTag root = p.getPersistentData();
        if (!root.contains(PERSISTED) || root.getCompound(PERSISTED).getInt(STAGE) == 0) {
            return;
        }
        CompoundTag pd = root.getCompound(PERSISTED);
        ServerLevel level = p.serverLevel();
        if (level.dimension() != Level.OVERWORLD || !p.isAlive() || p.isCreative() || p.isSpectator()
                || !HollowConfig.ENABLED.get() || !HollowConfig.DOG_TRICK.get()) {
            return;
        }
        BlockPos house = BlockPos.of(pd.getLong(HOUSE));
        double d = Math.sqrt(p.blockPosition().distSqr(house));
        boolean roofed = !level.canSeeSky(p.blockPosition());
        int stage = pd.getInt(STAGE);
        if (stage == 1) {
            if (roofed && d <= 16.0D) {
                pd.putInt(STAGE, 2);              // you went in to look for it
            }
            return;
        }
        if (!roofed && d >= 2.0D && d <= 64.0D && HollowSpawner.spawnDogScene(p)) {
            pd.putInt(STAGE, 0);                  // you came out: he is waiting
        }
    }

    // ------------------------------------------------------------------ the curse

    @SubscribeEvent
    public void onTame(AnimalTameEvent event) {
        if (event.getAnimal() instanceof Wolf && event.getTamer() instanceof ServerPlayer p && isCursed(p)) {
            event.setCanceled(true);
            p.displayClientMessage(Component.translatable("dog.hollowgrin.refuse"), true);
        }
    }
}
