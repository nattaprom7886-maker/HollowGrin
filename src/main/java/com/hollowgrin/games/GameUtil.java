package com.hollowgrin.games;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/** Tiny helpers shared by the mini games. Everything is sent only to the one player who plays. */
final class GameUtil {
    static MutableComponent t(String key, Object... args) {
        return Component.translatable(key, args);
    }

    static void title(ServerPlayer p, Component title, Component sub, int fadeIn, int stay, int fadeOut) {
        p.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
        p.connection.send(new ClientboundSetSubtitleTextPacket(sub));
        p.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    static void actionBar(ServerPlayer p, Component text) {
        p.displayClientMessage(text, true);
    }

    static void sound(ServerPlayer p, SoundEvent event, SoundSource source, float volume, float pitch) {
        p.playNotifySound(event, source, volume, pitch);
    }

    private GameUtil() {}
}
