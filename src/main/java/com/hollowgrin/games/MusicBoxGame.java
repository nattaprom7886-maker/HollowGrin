package com.hollowgrin.games;

import com.hollowgrin.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Random;

/**
 * Game 2 - Music Box (a memory game).
 * He plays a melody of chimes and shows the hotbar slot number of every note. Press the same hotbar
 * keys (1-9) in the same order. 5 rounds, the melody grows by one note each round.
 * A slot only counts when it stays selected for 4 ticks, so rolling the mouse wheel across other slots is safe.
 */
final class MusicBoxGame extends HgGame {
    static final String KEY = "musicbox";
    private static final int ROUNDS = 5;
    private static final int START_LEN = 3;
    private static final int HOLD_TICKS = 4;
    private static final float[] PITCH = new float[9];

    static {
        int[] semitones = {0, 2, 4, 7, 9, 12, 14, 16, 19};   // pentatonic: every combination sounds pleasant
        for (int i = 0; i < 9; i++) {
            PITCH[i] = (float) (0.5D * Math.pow(2.0D, semitones[i] / 12.0D));
        }
    }

    private enum Phase { INTRO, SHOW, INPUT, PAUSE }

    private final Random rnd = new Random();
    private final ServerBossEvent bar = new ServerBossEvent(GameUtil.t("game.hollowgrin.mb.bar", 1, ROUNDS),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);

    private Phase phase = Phase.INTRO;
    private int phaseLeft = 80;
    private int round;
    private int[] seq = new int[0];
    private int rest;
    private int showIdx;
    private int inputIdx;
    private int committed;
    private int candidate;
    private int candidateAge;

    MusicBoxGame(ServerPlayer p) {
        super(p);
        this.bar.setProgress(0.0F);
        this.bar.addPlayer(p);
        GameUtil.title(p, GameUtil.t("game.hollowgrin.mb.name").withStyle(ChatFormatting.LIGHT_PURPLE),
                GameUtil.t("game.hollowgrin.mb.intro"), 10, 65, 10);
        GameUtil.sound(p, ModSounds.HUM.get(), SoundSource.HOSTILE, 0.6F, 1.0F);
    }

    @Override
    boolean tick(ServerPlayer p) {
        this.age++;
        this.phaseLeft--;
        switch (this.phase) {
            case INTRO, PAUSE -> {
                if (this.phaseLeft <= 0) {
                    this.beginRound(p);
                }
            }
            case SHOW -> {
                if (this.phaseLeft <= 0) {
                    if (this.showIdx < this.seq.length) {
                        int slot = this.seq[this.showIdx++];
                        this.note(p, slot, 1.0F);
                        GameUtil.title(p, Component.literal(String.valueOf(slot + 1)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                                GameUtil.t("game.hollowgrin.mb.note", this.showIdx, this.seq.length), 0, 10, 4);
                        this.phaseLeft = this.showIdx < this.seq.length ? 18 : 26;
                    } else {
                        this.startInput(p);
                    }
                }
            }
            case INPUT -> {
                if (this.phaseLeft <= 0) {
                    return this.lose(p, true);
                }
                int sel = p.getInventory().selected;
                if (sel != this.candidate) {
                    this.candidate = sel;
                    this.candidateAge = 0;
                } else {
                    this.candidateAge++;
                }
                if (this.candidateAge == HOLD_TICKS && this.candidate != this.committed) {
                    this.committed = this.candidate;
                    if (this.committed != this.seq[this.inputIdx]) {
                        return this.lose(p, false);
                    }
                    this.note(p, this.committed, 0.9F);
                    this.inputIdx++;
                    if (this.inputIdx >= this.seq.length) {
                        if (this.round >= ROUNDS) {
                            return this.win(p);
                        }
                        this.roundDone(p);
                    } else {
                        GameUtil.actionBar(p, GameUtil.t("game.hollowgrin.mb.repeat", this.inputIdx, this.seq.length)
                                .withStyle(ChatFormatting.YELLOW));
                    }
                }
            }
        }
        return false;
    }

    private void beginRound(ServerPlayer p) {
        this.round++;
        int len = START_LEN + this.round - 1;
        this.seq = new int[len];
        int prev = -1;
        for (int i = 0; i < len; i++) {
            int v;
            do {
                v = this.rnd.nextInt(9);
            } while (v == prev);                  // never the same slot twice in a row
            this.seq[i] = v;
            prev = v;
        }
        this.rest = (this.seq[0] + 1) % 9;        // a resting slot that is never the first note
        this.forceSlot(p, this.rest);
        this.phase = Phase.SHOW;
        this.showIdx = 0;
        this.phaseLeft = 30;
        this.bar.setName(GameUtil.t("game.hollowgrin.mb.bar", this.round, ROUNDS));
        this.bar.setProgress((this.round - 1) / (float) ROUNDS);
        GameUtil.actionBar(p, GameUtil.t("game.hollowgrin.mb.listen", this.round, ROUNDS).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private void startInput(ServerPlayer p) {
        this.phase = Phase.INPUT;
        this.inputIdx = 0;
        this.forceSlot(p, this.rest);
        this.committed = this.rest;
        this.candidate = this.rest;
        this.candidateAge = 0;
        this.phaseLeft = 20 * (6 + 2 * this.seq.length);
        GameUtil.title(p, GameUtil.t("game.hollowgrin.mb.go").withStyle(ChatFormatting.GREEN),
                GameUtil.t("game.hollowgrin.mb.go.sub", this.seq.length), 0, 25, 8);
    }

    private void roundDone(ServerPlayer p) {
        this.phase = Phase.PAUSE;
        this.phaseLeft = 45;
        this.bar.setProgress(this.round / (float) ROUNDS);
        GameUtil.title(p, GameUtil.t("game.hollowgrin.mb.ok").withStyle(ChatFormatting.GREEN),
                GameUtil.t("game.hollowgrin.mb.ok.sub"), 0, 25, 10);
        GameUtil.sound(p, SoundEvents.PLAYER_LEVELUP, SoundSource.HOSTILE, 0.5F, 1.3F);
    }

    private boolean win(ServerPlayer p) {
        this.bar.setProgress(1.0F);
        GameUtil.title(p, GameUtil.t("game.hollowgrin.mb.win").withStyle(ChatFormatting.GOLD),
                GameUtil.t("game.hollowgrin.mb.win.sub"), 5, 55, 15);
        GameUtil.sound(p, ModSounds.CLAP.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
        MiniGames.reward(p, KEY, new ItemStack(Items.EMERALD, 4));
        return true;
    }

    private boolean lose(ServerPlayer p, boolean timeout) {
        if (timeout) {
            GameUtil.title(p, GameUtil.t("game.hollowgrin.mb.timeout").withStyle(ChatFormatting.GRAY),
                    GameUtil.t("game.hollowgrin.mb.timeout.sub"), 5, 45, 15);
        } else {
            GameUtil.title(p, GameUtil.t("game.hollowgrin.mb.lose").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                    GameUtil.t("game.hollowgrin.mb.lose.sub", this.seq[this.inputIdx] + 1), 0, 55, 15);
        }
        GameUtil.sound(p, SoundEvents.VILLAGER_NO, SoundSource.HOSTILE, 0.8F, 0.7F);
        GameUtil.sound(p, ModSounds.LAUGH.get(), SoundSource.HOSTILE, 0.6F, 1.0F);
        return true;
    }

    private void note(ServerPlayer p, int slot, float volume) {
        GameUtil.sound(p, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, volume, PITCH[slot]);
    }

    private void forceSlot(ServerPlayer p, int slot) {
        p.getInventory().selected = slot;
        p.connection.send(new ClientboundSetCarriedItemPacket(slot));
    }

    @Override
    void cleanup() {
        this.bar.removeAllPlayers();
    }
}
