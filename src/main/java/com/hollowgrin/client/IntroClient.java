package com.hollowgrin.client;

import net.minecraft.client.Minecraft;

/** Client entry point for the intro (only ever called on the client through DistExecutor). */
public final class IntroClient {
    private IntroClient() {}

    public static void start() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen instanceof IntroScreen) {
            return;
        }
        mc.setScreen(new IntroScreen());
    }
}
