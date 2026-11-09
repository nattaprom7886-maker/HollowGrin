package com.hollowgrin.network;

import com.hollowgrin.entity.HollowGrinEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: "I pressed the jump key while being held". Carries no data. */
public final class StruggleMessage {
    public StruggleMessage() {}

    public static void encode(StruggleMessage msg, FriendlyByteBuf buf) {}

    public static StruggleMessage decode(FriendlyByteBuf buf) {
        return new StruggleMessage();
    }

    public static void handle(StruggleMessage msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer p = c.getSender();
            if (p != null && p.getVehicle() instanceof HollowGrinEntity host) {
                host.onStruggle(p);
            }
        });
        c.setPacketHandled(true);
    }
}
