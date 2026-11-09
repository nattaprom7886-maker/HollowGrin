package com.hollowgrin.client;

import com.hollowgrin.HollowGrinMod;
import com.hollowgrin.entity.HollowGrinEntity;
import com.hollowgrin.network.ModNetwork;
import com.hollowgrin.network.StruggleMessage;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Counts presses of the jump key (Spacebar unless rebound) while held, and reports each to the server. */
@Mod.EventBusSubscriber(modid = HollowGrinMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        if (mc.player.getVehicle() instanceof HollowGrinEntity host && host.getState() == HollowGrinEntity.GRAB) {
            while (mc.options.keyJump.consumeClick()) {
                ModNetwork.CHANNEL.sendToServer(new StruggleMessage());
            }
        }
    }
}
