package com.hollowgrin.games;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.UUID;

/** One running mini game for one player. The manager (MiniGames) calls tick() once per server tick. */
abstract class HgGame {
    final UUID id;
    final ResourceKey<Level> dim;
    int age;

    HgGame(ServerPlayer p) {
        this.id = p.getUUID();
        this.dim = p.level().dimension();
    }

    /** @return true when the game is over (won, lost or timed out) and must be removed. */
    abstract boolean tick(ServerPlayer p);

    /** Always called exactly once when the game is removed (boss bar, snowballs...). */
    void cleanup() {}
}
