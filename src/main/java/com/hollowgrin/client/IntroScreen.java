package com.hollowgrin.client;

import com.hollowgrin.IntroTimeline;
import com.hollowgrin.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/**
 * The intro: pure black, two hollow eyes with small red eyeballs fade in, he speaks (subtitled), the eyes vanish,
 * only his laugh is left, then the black fades away and the normal game is there.
 * It is a normal Screen, so keys/mouse cannot move you and ESC does nothing; the game itself is not paused.
 */
public class IntroScreen extends Screen {
    private int ticks;
    private boolean started;
    private boolean cursorHidden;

    public IntroScreen() {
        super(Component.empty());
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        if (this.minecraft != null && !this.cursorHidden) {
            GLFW.glfwSetInputMode(this.minecraft.getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
            this.cursorHidden = true;
        }
        if (!this.started) {
            this.started = true;
            play(ModSounds.INTRO_MUSIC.get());
        }
    }

    @Override
    public void removed() {
        if (this.cursorHidden && this.minecraft != null) {
            GLFW.glfwSetInputMode(this.minecraft.getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
            this.cursorHidden = false;
        }
    }

    private void play(SoundEvent sound) {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F, 1.0F));
        }
    }

    @Override
    public void tick() {
        this.ticks++;
        if (this.minecraft == null || this.minecraft.player == null || this.minecraft.level == null) {
            this.onClose();
            return;
        }
        if (this.ticks == IntroTimeline.VOICE_START) {
            play(ModSounds.INTRO_VOICE.get());
        }
        if (this.ticks == IntroTimeline.LAUGH_START) {
            play(ModSounds.INTRO_LAUGH.get());
        }
        if (this.ticks >= IntroTimeline.TOTAL_TICKS) {
            this.onClose();
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        float time = this.ticks + partial;
        int w = this.width;
        int h = this.height;

        float fadeStart = IntroTimeline.TOTAL_TICKS - IntroTimeline.FADE_OUT_TICKS;
        float bg = time > fadeStart ? 1.0F - Mth.clamp((time - fadeStart) / IntroTimeline.FADE_OUT_TICKS, 0.0F, 1.0F) : 1.0F;
        if (bg > 0.004F) {
            g.fill(0, 0, w, h, (Mth.clamp((int) (bg * 255.0F), 0, 255) << 24));
        }

        drawEyes(g, w, h, time);
        drawSubtitle(g, w, h, time);
    }

    // ------------------------------------------------------------------ eyes
    private static float eyeVisibility(float time) {
        if (time < IntroTimeline.EYES_IN_START) {
            return 0.0F;
        }
        if (time < IntroTimeline.EYES_IN_END) {
            float p = (time - IntroTimeline.EYES_IN_START) / (IntroTimeline.EYES_IN_END - IntroTimeline.EYES_IN_START);
            float flicker = 0.7F + 0.3F * Mth.sin(time * 1.9F) * Mth.sin(time * 0.67F + 1.0F);
            return p * p * flicker;
        }
        if (time < IntroTimeline.EYES_OUT_START) {
            return 0.88F + 0.12F * Mth.sin(time * 0.09F);
        }
        if (time < IntroTimeline.EYES_OUT_END) {
            float p = (time - IntroTimeline.EYES_OUT_START) / (IntroTimeline.EYES_OUT_END - IntroTimeline.EYES_OUT_START);
            return (1.0F - p) * Math.abs(Mth.sin(time * 2.3F));
        }
        return 0.0F;
    }

    private static float smooth(float x) {
        x = Mth.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    private void drawEyes(GuiGraphics g, int w, int h, float time) {
        float vis = eyeVisibility(time);
        if (vis <= 0.004F) {
            return;
        }
        float grow = 1.0F + 0.30F * smooth((time - IntroTimeline.EYES_IN_END) / (IntroTimeline.EYES_OUT_START - IntroTimeline.EYES_IN_END));
        float cx = w / 2.0F + Mth.sin(time * 0.045F) * h * 0.004F;
        float cy = h * 0.42F + Mth.sin(time * 0.031F + 2.0F) * h * 0.005F;
        float spread = h * 0.085F * grow;
        float rx = h * 0.045F * grow;
        float ry = h * 0.058F * grow;

        // happy squint while he giggles
        float squint = 1.0F;
        if (time >= IntroTimeline.SQUINT_START - 6 && time <= IntroTimeline.SQUINT_END + 6) {
            float in = smooth((time - (IntroTimeline.SQUINT_START - 6)) / 6.0F);
            float out = 1.0F - smooth((time - IntroTimeline.SQUINT_END) / 6.0F);
            squint = 1.0F - 0.72F * Math.min(in, out);
        }
        // slow blink
        float cycle = time % 83.0F;
        float blink = cycle < 5.0F ? Math.abs(Mth.cos(cycle / 5.0F * Mth.HALF_PI)) : 1.0F;
        float lid = squint * Math.max(0.08F, blink);

        float lookX = Mth.sin(time * 0.037F) * rx * 0.22F;
        float lookY = Mth.sin(time * 0.053F + 1.0F) * ry * 0.12F;

        for (int side = -1; side <= 1; side += 2) {
            drawEye(g, cx + side * spread, cy, rx, ry * lid, lookX, lookY * lid, vis);
        }
    }

    private static void drawEye(GuiGraphics g, float x, float y, float rx, float ry, float lookX, float lookY, float vis) {
        // faint red haze around the dark socket
        for (int i = 6; i >= 1; i--) {
            float k = 1.0F + i * 0.30F;
            ellipse(g, x, y, rx * k, ry * k, 0xFF1010, vis * 0.035F);
        }
        // hollow socket: a dark, slightly reddish hole
        ellipse(g, x, y, rx * 1.08F, ry * 1.08F, 0x4A0A0A, vis * 0.55F);
        ellipse(g, x, y, rx, ry, 0x120303, vis * 0.97F);
        // the small red eyeball floating inside
        float er = rx * 0.34F;
        float ey = Math.min(ry, er * 1.1F);
        for (int i = 3; i >= 1; i--) {
            ellipse(g, x + lookX, y + lookY, er * (1.0F + i * 0.45F), ey * (1.0F + i * 0.45F), 0xFF2020, vis * 0.10F);
        }
        ellipse(g, x + lookX, y + lookY, er, ey, 0xC81010, vis);
        ellipse(g, x + lookX, y + lookY, er * 0.55F, ey * 0.55F, 0xFF5A4A, vis);
    }

    /** Filled ellipse, drawn as horizontal strips (alpha-blended). */
    private static void ellipse(GuiGraphics g, float cx, float cy, float rx, float ry, int rgb, float alpha) {
        int a = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        if (a <= 0 || rx < 0.5F || ry < 0.5F) {
            return;
        }
        int color = (a << 24) | (rgb & 0xFFFFFF);
        int top = (int) Math.floor(cy - ry);
        int bottom = (int) Math.ceil(cy + ry);
        for (int yy = top; yy < bottom; yy++) {
            float dy = (yy + 0.5F - cy) / ry;
            float s = 1.0F - dy * dy;
            if (s <= 0.0F) {
                continue;
            }
            float dx = rx * (float) Math.sqrt(s);
            g.fill((int) Math.floor(cx - dx), yy, (int) Math.ceil(cx + dx), yy + 1, color);
        }
    }

    // ------------------------------------------------------------------ subtitles
    private void drawSubtitle(GuiGraphics g, int w, int h, float time) {
        IntroTimeline.Line[] lines = IntroTimeline.LINES;
        for (int i = 0; i < lines.length; i++) {
            IntroTimeline.Line l = lines[i];
            int hideAt = l.end() + 24;
            if (i + 1 < lines.length) {
                hideAt = Math.min(hideAt, lines[i + 1].start());
            }
            if (time < l.start() || time >= hideAt) {
                continue;
            }
            String full = I18n.get(l.key());
            float typeSpan = Math.max(1.0F, (l.end() - l.start()) * 0.85F);
            int shown = Mth.clamp((int) (full.length() * ((time - l.start()) / typeSpan)) + 1, 0, full.length());
            float fadeOut = Mth.clamp((hideAt - time) / 8.0F, 0.0F, 1.0F);
            int alpha = (int) (235.0F * fadeOut);
            if (alpha < 12) {
                return;
            }
            int color = (alpha << 24) | 0xE6CFCF;
            g.drawCenteredString(this.font, full.substring(0, shown), w / 2, (int) (h * 0.80F), color);
            return;
        }
    }
}
