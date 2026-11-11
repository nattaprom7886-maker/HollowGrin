package com.hollowgrin;

import com.hollowgrin.network.IntroMessage;
import com.hollowgrin.network.ModNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * World-entry intro (server side). Decides ONCE per player per world, at the very first join:
 *   - overworld, brand-new world, Survival (not Hardcore / Creative), normal world (not Superflat / Debug)
 * then tells the client to show the black "red eyes" screen. While it plays the player cannot be hurt.
 * The only thing saved is one boolean in the player's own data (the normal Minecraft way) - no files are touched.
 */
public class IntroEvents {
    private static final String PERSISTED = "PlayerPersisted";
    private static final String SEEN = "hollowgrin_intro_seen";

    private static final Map<UUID, Integer> PENDING = new HashMap<>();
    private static final Map<UUID, Integer> ACTIVE = new HashMap<>();

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CompoundTag root = player.getPersistentData();
        CompoundTag persisted = root.getCompound(PERSISTED);
        if (persisted.getBoolean(SEEN)) {
            return;                                   // already decided for this world
        }
        persisted.putBoolean(SEEN, true);             // decide only once, whatever the answer is
        root.put(PERSISTED, persisted);
        if (shouldPlay(player)) {
            PENDING.put(player.getUUID(), IntroTimeline.START_DELAY_TICKS);
        }
    }

    private static boolean shouldPlay(ServerPlayer player) {
        if (!HollowConfig.INTRO_ENABLED.get()) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        if (level.dimension() != Level.OVERWORLD) {
            return false;
        }
        if (level.getGameTime() > IntroTimeline.NEW_WORLD_TICKS) {
            return false;                             // an old world: never surprise it
        }
        if (player.gameMode.getGameModeForPlayer() != GameType.SURVIVAL || level.getLevelData().isHardcore()) {
            return false;
        }
        ChunkGenerator gen = level.getChunkSource().getGenerator();
        return !(gen instanceof FlatLevelSource) && !(gen instanceof DebugLevelSource);
    }

    /** Starts the intro for this player right now (also used by /hollowgrin intro). */
    public static void start(ServerPlayer player) {
        ACTIVE.put(player.getUUID(), IntroTimeline.TOTAL_TICKS + 60);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new IntroMessage());
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER
                || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        UUID id = player.getUUID();
        Integer wait = PENDING.get(id);
        if (wait != null) {
            if (wait <= 1) {
                PENDING.remove(id);
                start(player);
            } else {
                PENDING.put(id, wait - 1);
            }
        }
        Integer left = ACTIVE.get(id);
        if (left != null) {
            if (left <= 1) {
                ACTIVE.remove(id);
            } else {
                ACTIVE.put(id, left - 1);
            }
        }
    }

    /** Nothing can hurt the player while the black screen is up (in memory only, never saved). */
    @SubscribeEvent
    public void onAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && ACTIVE.containsKey(p.getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING.remove(event.getEntity().getUUID());
        ACTIVE.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        PENDING.clear();
        ACTIVE.clear();
    }
}
