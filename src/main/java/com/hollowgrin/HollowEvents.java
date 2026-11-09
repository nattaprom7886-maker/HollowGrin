package com.hollowgrin;

import com.hollowgrin.entity.HollowGrinEntity;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server-side glue: "stay in a village long enough" timer, anti-dismount while held, test commands. */
public class HollowEvents {
    /** per-player village time in ticks (kept in memory only - nothing is ever written to disk). */
    private final Map<UUID, Integer> villageTicks = new HashMap<>();

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        if (!HollowConfig.ENABLED.get() || !HollowConfig.NATURAL_SPAWN.get()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (level.dimension() != Level.OVERWORLD || level.getDifficulty() == Difficulty.PEACEFUL
                || player.isCreative() || player.isSpectator()) {
            return;
        }

        int v = this.villageTicks.getOrDefault(player.getUUID(), 0);
        int villagers = level.getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(40.0D, 20.0D, 40.0D)).size();
        if (villagers >= 2) {
            v += 20;
        } else if (v > 0) {
            v = Math.max(0, v - 10);
        }
        if (v >= HollowConfig.VILLAGE_SECONDS.get() * 20) {
            if (HollowSpawner.spawnFor(player, false)) {
                v = -HollowConfig.COOLDOWN_SECONDS.get() * 20;
            }
        }
        this.villageTicks.put(player.getUUID(), v);
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        this.villageTicks.remove(event.getEntity().getUUID());
    }

    /** While he is holding you, pressing shift does nothing. Never blocks dismounting when anything is dying/removed. */
    @SubscribeEvent
    public void onMount(EntityMountEvent event) {
        if (event.isDismounting()
                && event.getEntityBeingMounted() instanceof HollowGrinEntity host
                && event.getEntityMounting() instanceof Player rider
                && host.shouldHoldPassenger()
                && rider.isAlive() && !rider.isRemoved()
                && !rider.level().isClientSide) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("hollowgrin")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("summon").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    boolean ok = HollowSpawner.spawnFor(p, true);
                    ctx.getSource().sendSuccess(() -> Component.literal(ok
                            ? "Mr. Hollowgrin is somewhere nearby. He looks like a villager."
                            : "Could not place him (is he already here, or is there no room?)"), true);
                    return ok ? 1 : 0;
                }).then(Commands.literal("host").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    boolean ok = HollowSpawner.spawnFor(p, true, true);
                    ctx.getSource().sendSuccess(() -> Component.literal(ok
                            ? "The Host (true form) has arrived."
                            : "Could not place him (is he already here, or is there no room?)"), true);
                    return ok ? 1 : 0;
                })))
                .then(Commands.literal("clear").executes(ctx -> {
                    List<HollowGrinEntity> found = new ArrayList<>();
                    for (Entity e : ctx.getSource().getLevel().getAllEntities()) {
                        if (e instanceof HollowGrinEntity h) {
                            found.add(h);
                        }
                    }
                    for (HollowGrinEntity h : found) {
                        h.vanish(false);
                    }
                    int n = found.size();
                    ctx.getSource().sendSuccess(() -> Component.literal("Removed " + n + " Hollowgrin."), true);
                    return n;
                })));
    }
}
