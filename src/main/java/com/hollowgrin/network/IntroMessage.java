package com.hollowgrin.network;

import com.hollowgrin.client.IntroClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: "play the world-entry intro now". Carries no data (timing lives in IntroTimeline). */
public final class IntroMessage {
    public IntroMessage() {}

    public static void encode(IntroMessage msg, FriendlyByteBuf buf) {}

    public static IntroMessage decode(FriendlyByteBuf buf) {
        return new IntroMessage();
    }

    public static void handle(IntroMessage msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> IntroClient::start));
        c.setPacketHandled(true);
    }
}
