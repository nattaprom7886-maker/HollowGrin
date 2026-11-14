package com.hollowgrin.games;

import com.hollowgrin.HollowConfig;
import com.hollowgrin.entity.HollowGrinEntity;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mr. Hollowgrin's Games (1.8, the final update): "Shall we play, new friend?"
 *
 * Three mini games, started with /hollowgames (or /hgames) - anybody can play, no OP needed:
 *   redlight  Red Light, Green Light
 *   musicbox  Music Box (memory)
 *   dodge     Snowball Dodge
 *
 * Everything runs on the server and is sent to the player as titles / action bar / boss bar, so there is no new
 * client code. Nothing can hurt you, nothing is written to disk. If a game ever throws an error it is simply
 * ended and the error is logged - it can never take the world down with it.
 */
public class MiniGames {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, HgGame> ACTIVE = new HashMap<>();
    private static final Map<String, Long> LAST_REWARD = new HashMap<>();   // memory only

    // ------------------------------------------------------------------ prizes

    static void reward(ServerPlayer p, String key, ItemStack stack) {
        if (!HollowConfig.GAME_REWARDS.get() || p.isCreative() || p.isSpectator()) {
            return;
        }
        long now = p.serverLevel().getGameTime();
        long cooldown = HollowConfig.GAME_REWARD_COOLDOWN.get() * 20L;
        String k = p.getUUID() + ":" + key;
        Long last = LAST_REWARD.get(k);
        if (last != null && now >= last && now - last < cooldown) {
            long wait = (cooldown - (now - last)) / 20L + 1L;
            p.sendSystemMessage(GameUtil.t("game.hollowgrin.common.reward_wait", wait).withStyle(ChatFormatting.GRAY));
            return;
        }
        LAST_REWARD.put(k, now);
        Component what = Component.literal(stack.getCount() + " x ").append(stack.getHoverName());
        if (!p.getInventory().add(stack) && !stack.isEmpty()) {
            p.drop(stack, false);
        }
        p.sendSystemMessage(GameUtil.t("game.hollowgrin.common.reward", what).withStyle(ChatFormatting.GOLD));
    }

    // ------------------------------------------------------------------ start / stop

    private static int start(ServerPlayer p, String which) {
        if (!HollowConfig.GAMES_ENABLED.get()) {
            p.sendSystemMessage(GameUtil.t("game.hollowgrin.common.disabled").withStyle(ChatFormatting.RED));
            return 0;
        }
        if (ACTIVE.containsKey(p.getUUID())) {
            p.sendSystemMessage(GameUtil.t("game.hollowgrin.common.busy").withStyle(ChatFormatting.RED));
            return 0;
        }
        if (!p.isAlive() || p.isSpectator() || p.isPassenger() || p.isSleeping()) {
            p.sendSystemMessage(GameUtil.t("game.hollowgrin.common.cant").withStyle(ChatFormatting.RED));
            return 0;
        }
        HgGame game;
        switch (which) {
            case RedLightGame.KEY -> game = new RedLightGame(p);
            case MusicBoxGame.KEY -> game = new MusicBoxGame(p);
            case DodgeGame.KEY -> game = new DodgeGame(p);
            default -> {
                return 0;
            }
        }
        ACTIVE.put(p.getUUID(), game);
        return 1;
    }

    private static int stop(ServerPlayer p) {
        HgGame g = ACTIVE.remove(p.getUUID());
        if (g == null) {
            p.sendSystemMessage(GameUtil.t("game.hollowgrin.common.nogame").withStyle(ChatFormatting.GRAY));
            return 0;
        }
        g.cleanup();
        p.sendSystemMessage(GameUtil.t("game.hollowgrin.common.stopped").withStyle(ChatFormatting.GRAY));
        return 1;
    }

    private static int help(CommandSourceStack src) {
        src.sendSuccess(() -> GameUtil.t("game.hollowgrin.help.title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        src.sendSuccess(() -> link("game.hollowgrin.help.redlight", "/hollowgames redlight"), false);
        src.sendSuccess(() -> link("game.hollowgrin.help.musicbox", "/hollowgames musicbox"), false);
        src.sendSuccess(() -> link("game.hollowgrin.help.dodge", "/hollowgames dodge"), false);
        src.sendSuccess(() -> GameUtil.t("game.hollowgrin.help.stop").withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    /** a help line the player can click to start that game. */
    private static Component link(String key, String command) {
        return GameUtil.t(key).withStyle(s -> s.withColor(ChatFormatting.YELLOW)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)));
    }

    // ------------------------------------------------------------------ events

    @SubscribeEvent
    public void onCommands(RegisterCommandsEvent event) {
        LiteralCommandNode<CommandSourceStack> node = event.getDispatcher().register(Commands.literal("hollowgames")
                .executes(ctx -> help(ctx.getSource()))
                .then(Commands.literal(RedLightGame.KEY).executes(ctx -> start(player(ctx.getSource()), RedLightGame.KEY)))
                .then(Commands.literal(MusicBoxGame.KEY).executes(ctx -> start(player(ctx.getSource()), MusicBoxGame.KEY)))
                .then(Commands.literal(DodgeGame.KEY).executes(ctx -> start(player(ctx.getSource()), DodgeGame.KEY)))
                .then(Commands.literal("stop").executes(ctx -> stop(player(ctx.getSource())))));
        event.getDispatcher().register(Commands.literal("hgames")
                .executes(ctx -> help(ctx.getSource()))
                .redirect(node));
    }

    private static ServerPlayer player(CommandSourceStack src) throws CommandSyntaxException {
        return src.getPlayerOrException();
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ACTIVE.isEmpty()) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        for (Map.Entry<UUID, HgGame> e : new ArrayList<>(ACTIVE.entrySet())) {
            HgGame g = e.getValue();
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            boolean done;
            try {
                if (p == null || !p.isAlive() || p.level().dimension() != g.dim
                        || p.getVehicle() instanceof HollowGrinEntity) {
                    done = true;                      // died, left, changed dimension or was grabbed: the game just ends
                    if (p != null && p.isAlive()) {
                        GameUtil.actionBar(p, GameUtil.t("game.hollowgrin.common.aborted").withStyle(ChatFormatting.GRAY));
                    }
                } else {
                    done = g.tick(p);
                }
            } catch (RuntimeException ex) {
                LOGGER.error("Hollowgrin mini game crashed and was stopped", ex);
                done = true;
            }
            if (done) {
                ACTIVE.remove(e.getKey());
                try {
                    g.cleanup();
                } catch (RuntimeException ex) {
                    LOGGER.error("Hollowgrin mini game cleanup failed", ex);
                }
            }
        }
    }

    /** Snowball Dodge: our snowballs never hurt anything; a touch on the player costs one life. */
    @SubscribeEvent
    public void onImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof Snowball ball) || ball.level().isClientSide
                || !ball.getTags().contains(DodgeGame.TAG)) {
            return;
        }
        if (event.getRayTraceResult() instanceof EntityHitResult hit) {
            event.setCanceled(true);
            if (hit.getEntity() instanceof ServerPlayer p && ACTIVE.get(p.getUUID()) instanceof DodgeGame g) {
                g.hit(ball);
            }
            ball.discard();
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        HgGame g = ACTIVE.remove(event.getEntity().getUUID());
        if (g != null) {
            g.cleanup();
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        for (HgGame g : new ArrayList<>(ACTIVE.values())) {
            try {
                g.cleanup();
            } catch (RuntimeException ex) {
                LOGGER.error("Hollowgrin mini game cleanup failed", ex);
            }
        }
        ACTIVE.clear();
        LAST_REWARD.clear();
    }
}
