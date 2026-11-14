package com.hollowgrin.games;

import com.hollowgrin.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Game 3 - Snowball Dodge.
 * Mr. Hollowgrin throws snowballs at you from all around, faster and faster, for 45 seconds.
 * You have 3 lives; every snowball that touches you costs one. Snowballs deal NO damage and are removed on contact.
 * The balls fly straight and are aimed a little ahead of where you are running, so change direction!
 */
final class DodgeGame extends HgGame {
    static final String KEY = "dodge";
    static final String TAG = "hollowgrin_snowball";
    private static final int LIVES = 3;
    private static final int INTRO_TICKS = 70;
    private static final int PLAY_TICKS = 20 * 45;
    private static final double SPEED = 0.8D;

    private final Random rnd = new Random();
    private final List<Snowball> balls = new ArrayList<>();
    private final ServerBossEvent bar = new ServerBossEvent(GameUtil.t("game.hollowgrin.dg.bar", LIVES),
            BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);

    private int lives = LIVES;
    private int throwCooldown = 30;
    private boolean lost;
    private Vec3 prevPos;
    private Vec3 vel = Vec3.ZERO;

    DodgeGame(ServerPlayer p) {
        super(p);
        this.bar.setProgress(1.0F);
        this.bar.addPlayer(p);
        GameUtil.title(p, GameUtil.t("game.hollowgrin.dg.name").withStyle(ChatFormatting.AQUA),
                GameUtil.t("game.hollowgrin.dg.intro", LIVES, PLAY_TICKS / 20), 10, 55, 10);
        GameUtil.sound(p, ModSounds.HUM.get(), SoundSource.HOSTILE, 0.6F, 1.0F);
    }

    /** called by MiniGames when one of our snowballs touches the player. */
    void hit(Snowball ball) {
        if (this.lost) {
            return;
        }
        this.balls.remove(ball);
        this.lives--;
        this.bar.setName(GameUtil.t("game.hollowgrin.dg.bar", Math.max(0, this.lives)));
        if (this.lives <= 0) {
            this.lost = true;
        }
    }

    private int lastLives = LIVES;

    @Override
    boolean tick(ServerPlayer p) {
        this.age++;
        Vec3 pos = p.position();
        if (this.prevPos != null) {
            Vec3 step = new Vec3(pos.x - this.prevPos.x, 0.0D, pos.z - this.prevPos.z);
            if (step.lengthSqr() < 4.0D) {                       // ignore teleports
                this.vel = this.vel.scale(0.6D).add(step.scale(0.4D));
            }
        }
        this.prevPos = pos;

        if (this.lives != this.lastLives) {                       // a ball got us this tick
            this.lastLives = this.lives;
            if (this.lives > 0) {
                GameUtil.sound(p, SoundEvents.SNOW_BREAK, SoundSource.HOSTILE, 1.0F, 0.8F);
                GameUtil.actionBar(p, GameUtil.t("game.hollowgrin.dg.hit", this.lives).withStyle(ChatFormatting.RED));
            }
        }
        if (this.lost) {
            GameUtil.title(p, GameUtil.t("game.hollowgrin.dg.lose").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                    GameUtil.t("game.hollowgrin.dg.lose.sub"), 0, 50, 15);
            GameUtil.sound(p, ModSounds.GOTCHA.get(), SoundSource.HOSTILE, 0.9F, 1.0F);
            return true;
        }

        int t = this.age - INTRO_TICKS;
        if (t >= PLAY_TICKS) {
            GameUtil.title(p, GameUtil.t("game.hollowgrin.dg.win").withStyle(ChatFormatting.GOLD),
                    GameUtil.t("game.hollowgrin.dg.win.sub"), 5, 55, 15);
            GameUtil.sound(p, ModSounds.CLAP.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
            MiniGames.reward(p, KEY, new ItemStack(Items.GOLDEN_APPLE, 2));
            return true;
        }
        if (t >= 0) {
            float left = 1.0F - t / (float) PLAY_TICKS;
            this.bar.setProgress(Math.max(0.0F, left));
            if (this.age % 20 == 0) {
                GameUtil.actionBar(p, GameUtil.t("game.hollowgrin.dg.time", (PLAY_TICKS - t) / 20));
            }
            if (--this.throwCooldown <= 0) {
                float k = t / (float) PLAY_TICKS;               // 0 -> 1 : it gets faster
                this.throwOne(p);
                if (k > 0.5F && this.rnd.nextFloat() < 0.35F) {
                    this.throwOne(p);
                }
                this.throwCooldown = Math.round(26.0F - 16.0F * k);
            }
        }

        for (int i = this.balls.size() - 1; i >= 0; i--) {        // forget finished balls, never let one live too long
            Snowball b = this.balls.get(i);
            if (b.isRemoved()) {
                this.balls.remove(i);
            } else if (b.tickCount > 90) {
                b.discard();
                this.balls.remove(i);
            }
        }
        return false;
    }

    private void throwOne(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 body = p.position().add(0.0D, 0.9D, 0.0D);
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = this.rnd.nextDouble() * Math.PI * 2.0D;
            double r = 10.0D + this.rnd.nextDouble() * 4.0D;
            Vec3 from = new Vec3(p.getX() + Math.cos(angle) * r, p.getY() + 0.9D + this.rnd.nextDouble() * 1.2D,
                    p.getZ() + Math.sin(angle) * r);
            double flight = from.distanceTo(body) / SPEED;
            Vec3 lead = this.vel.scale(flight * 0.55D);
            if (lead.length() > 6.0D) {
                lead = lead.normalize().scale(6.0D);
            }
            Vec3 target = body.add(lead);
            ClipContext ctx = new ClipContext(from, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p);
            if (level.clip(ctx).getType() != HitResult.Type.MISS) {
                continue;                                         // a wall is in the way: try another direction
            }
            Snowball ball = new Snowball(level, from.x, from.y, from.z);
            ball.setNoGravity(true);
            ball.addTag(TAG);
            Vec3 d = target.subtract(from);
            ball.shoot(d.x, d.y, d.z, (float) SPEED, 0.0F);
            level.addFreshEntity(ball);
            this.balls.add(ball);
            GameUtil.sound(p, SoundEvents.SNOWBALL_THROW, SoundSource.HOSTILE, 0.7F, 0.7F);
            return;
        }
    }

    @Override
    void cleanup() {
        this.bar.removeAllPlayers();
        for (Snowball b : this.balls) {
            if (!b.isRemoved()) {
                b.discard();
            }
        }
        this.balls.clear();
    }
}
