package com.hollowgrin.client;

import com.hollowgrin.entity.HollowGrinEntity;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/** While held: screen slowly goes black (fully black = death) and a red struggle bar is drawn on top of it. */
public final class GrabOverlay {
    private static float shown;
    private static long lastMillis;

    private GrabOverlay() {}

    public static void render(ForgeGui gui, GuiGraphics g, float partialTick, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        long now = Util.getMillis();
        float dt = lastMillis == 0L ? 0.0F : Math.min(0.25F, (now - lastMillis) / 1000.0F);
        lastMillis = now;

        HollowGrinEntity host = null;
        if (mc.player != null && mc.player.getVehicle() instanceof HollowGrinEntity e
                && e.getState() == HollowGrinEntity.GRAB) {
            host = e;
        }

        if (host != null) {
            float gt = host.getStateTicks() + partialTick;
            float span = HollowGrinEntity.GRAB_KILL_AT - HollowGrinEntity.GRAB_SQUEEZE_FROM;
            float t = Mth.clamp((gt - HollowGrinEntity.GRAB_SQUEEZE_FROM) / span, 0.0F, 1.0F);
            shown = (float) Math.pow(t, 1.2D);
        } else {
            shown = Math.max(0.0F, shown - dt * 1.5F);      // light comes back quickly once you are free
        }

        if (shown > 0.002F) {
            int a = (int) (255.0F * Mth.clamp(shown * 1.02F, 0.0F, 1.0F));
            g.fill(0, 0, w, h, a << 24);
            // tunnel vision: top and bottom close in first
            int band = (int) (shown * h * 0.45F);
            if (band > 1) {
                g.fillGradient(0, 0, w, band, 0xFF000000, 0x00000000);
                g.fillGradient(0, h - band, w, h, 0x00000000, 0xFF000000);
            }
        }

        if (host != null) {
            int progress = Mth.clamp(host.getStruggle(), 0, 100);
            int bw = 140;
            int bh = 10;
            int x = w / 2 - bw / 2;
            int y = (int) (h * 0.68F);
            boolean pulse = (mc.player.tickCount / 3) % 2 == 0;
            g.fill(x - 2, y - 2, x + bw + 2, y + bh + 2, 0xFFB0B0B0);
            g.fill(x - 1, y - 1, x + bw + 1, y + bh + 1, 0xFF1A0000);
            int fillW = (int) (bw * (progress / 100.0F));
            if (fillW > 0) {
                g.fill(x, y, x + fillW, y + bh, (pulse && progress < 40) ? 0xFFFF3030 : 0xFFD01010);
            }
            Component text = Component.translatable("hud.hollowgrin.struggle", mc.options.keyJump.getTranslatedKeyMessage());
            g.drawCenteredString(mc.font, text, w / 2, y - 14, 0xFFFFFFFF);
        }
    }
}
