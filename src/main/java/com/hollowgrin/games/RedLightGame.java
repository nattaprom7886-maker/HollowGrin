package com.hollowgrin.games;

import com.hollowgrin.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Random;

/**
 * Game 1 - Red Light, Green Light.
 * Mr. Hollowgrin calls GREEN (run) and RED (freeze). Run 80 blocks in total during the green phases.
 * Move during red (after a short reaction grace) and you are caught. Nothing here can hurt you:
 * losing only blinds you for two seconds.
 */
final class RedLightGame extends HgGame {
    static final String KEY = "redlight";
    private static final int GOAL = 80;
    private static final int LIMIT_TICKS = 20 * 150;
    private static final int GRACE_TICKS = 12;

    private enum Phase { INTRO, GREEN, RED }

    private final Random rnd = new Random();
    private final ServerBossEvent bar = new ServerBossEvent(GameUtil.t("game.hollowgrin.rl.bar", 0, GOAL),
            BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS);

    private Phase phase = Phase.INTRO;
    private int phaseLeft = 70;
    private int redAge;
    private int moveTicks;
    private double traveled;
    private int shownBlocks = -1;
    private double lx;
    private double lz;
    private boolean haveLast;

    RedLightGame(ServerPlayer p) {
        super(p);
        this.bar.setProgress(0.0F);
        this.bar.addPlayer(p);
        GameUtil.title(p, GameUtil.t("game.hollowgrin.rl.name").withStyle(ChatFormatting.GOLD),
                GameUtil.t("game.hollowgrin.rl.intro", GOAL), 10, 55, 10);
        GameUtil.sound(p, ModSounds.HUM.get(), SoundSource.HOSTILE, 0.6F, 1.0F);
    }

    @Override
    boolean tick(ServerPlayer p) {
        this.age++;
        double x = p.getX();
        double z = p.getZ();
        double d = this.haveLast ? Math.sqrt((x - this.lx) * (x - this.lx) + (z - this.lz) * (z - this.lz)) : 0.0D;
        this.lx = x;
        this.lz = z;
        this.haveLast = true;
        boolean walking = !p.isPassenger() && d < 2.0D;   // a teleport / elytra burst never counts

        if (this.age >= LIMIT_TICKS) {
            return this.lose(p, true);
        }
        if (this.age % 20 == 0) {
            GameUtil.actionBar(p, GameUtil.t("game.hollowgrin.rl.time", Math.max(0, (LIMIT_TICKS - this.age) / 20)));
        }

        this.phaseLeft--;
        switch (this.phase) {
            case INTRO -> {
                if (this.phaseLeft <= 0) {
                    this.setGreen(p);
                }
            }
            case GREEN -> {
                if (walking) {
                    this.traveled += d;
                    int blocks = (int) this.traveled;
                    if (blocks != this.shownBlocks) {
                        this.shownBlocks = blocks;
                        this.bar.setName(GameUtil.t("game.hollowgrin.rl.bar", Math.min(blocks, GOAL), GOAL));
                        this.bar.setProgress(Math.min(1.0F, (float) (this.traveled / GOAL)));
                    }
                    if (this.traveled >= GOAL) {
                        return this.win(p);
                    }
                }
                if (this.phaseLeft == 15) {
                    GameUtil.sound(p, ModSounds.HEARTBEAT.get(), SoundSource.HOSTILE, 0.8F, 1.1F);
                    GameUtil.actionBar(p, GameUtil.t("game.hollowgrin.rl.warn").withStyle(ChatFormatting.YELLOW));
                }
                if (this.phaseLeft <= 0) {
                    this.setRed(p);
                }
            }
            case RED -> {
                this.redAge++;
                if (this.redAge > GRACE_TICKS) {
                    if (walking && d > 0.035D) {
                        this.moveTicks++;
                    } else if (this.moveTicks > 0) {
                        this.moveTicks--;
                    }
                    if (this.moveTicks >= 3) {
                        return this.lose(p, false);
                    }
                }
                if (this.phaseLeft <= 0) {
                    this.setGreen(p);
                }
            }
        }
        return false;
    }

    private void setGreen(ServerPlayer p) {
        this.phase = Phase.GREEN;
        this.phaseLeft = 50 + this.rnd.nextInt(60);
        this.bar.setColor(BossEvent.BossBarColor.GREEN);
        GameUtil.title(p, GameUtil.t("game.hollowgrin.rl.green").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
                GameUtil.t("game.hollowgrin.rl.green.sub"), 0, 20, 8);
        GameUtil.sound(p, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.0F, 1.5F);
    }

    private void setRed(ServerPlayer p) {
        this.phase = Phase.RED;
        this.phaseLeft = 45 + this.rnd.nextInt(30);
        this.redAge = 0;
        this.moveTicks = 0;
        this.bar.setColor(BossEvent.BossBarColor.RED);
        GameUtil.title(p, GameUtil.t("game.hollowgrin.rl.red").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                GameUtil.t("game.hollowgrin.rl.red.sub"), 0, 25, 8);
        GameUtil.sound(p, SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 1.0F, 0.6F);
    }

    private boolean win(ServerPlayer p) {
        GameUtil.title(p, GameUtil.t("game.hollowgrin.rl.win").withStyle(ChatFormatting.GOLD),
                GameUtil.t("game.hollowgrin.rl.win.sub"), 5, 50, 15);
        GameUtil.sound(p, ModSounds.CLAP.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
        MiniGames.reward(p, KEY, new ItemStack(Items.DIAMOND, 1));
        return true;
    }

    private boolean lose(ServerPlayer p, boolean timeout) {
        if (timeout) {
            GameUtil.title(p, GameUtil.t("game.hollowgrin.rl.timeout").withStyle(ChatFormatting.GRAY),
                    GameUtil.t("game.hollowgrin.rl.timeout.sub"), 5, 45, 15);
            GameUtil.sound(p, ModSounds.LAUGH.get(), SoundSource.HOSTILE, 0.7F, 1.0F);
        } else {
            GameUtil.title(p, GameUtil.t("game.hollowgrin.rl.lose").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                    GameUtil.t("game.hollowgrin.rl.lose.sub"), 0, 45, 15);
            GameUtil.sound(p, ModSounds.GOTCHA.get(), SoundSource.HOSTILE, 0.9F, 1.0F);
            p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false));
        }
        return true;
    }

    @Override
    void cleanup() {
        this.bar.removeAllPlayers();
    }
}
