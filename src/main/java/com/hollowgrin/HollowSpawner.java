package com.hollowgrin;

import com.hollowgrin.entity.HollowGrinEntity;
import com.hollowgrin.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Finds a believable spot among the villagers and drops the disguised Host there. */
public final class HollowSpawner {

    private HollowSpawner() {}

    /**
     * @param force true for the command: allowed in view and without villagers around.
     * @return true if he was spawned.
     */
    public static boolean spawnFor(ServerPlayer player, boolean force) {
        ServerLevel level = player.serverLevel();

        // only ever one of him per player area
        if (!level.getEntitiesOfClass(HollowGrinEntity.class, player.getBoundingBox().inflate(200.0D)).isEmpty()) {
            return false;
        }

        RandomSource rnd = level.random;
        List<Villager> villagers = level.getEntitiesOfClass(Villager.class,
                player.getBoundingBox().inflate(48.0D, 24.0D, 48.0D));
        if (!force && villagers.size() < 2) {
            return false;
        }

        Vec3 view = player.getViewVector(1.0F);
        double viewLen = Math.max(0.001D, Math.sqrt(view.x * view.x + view.z * view.z));

        for (int i = 0; i < 24; i++) {
            BlockPos base = villagers.isEmpty()
                    ? player.blockPosition()
                    : villagers.get(rnd.nextInt(villagers.size())).blockPosition();
            double angle = rnd.nextDouble() * Math.PI * 2.0D;
            double radius = villagers.isEmpty() ? 14.0D + rnd.nextDouble() * 8.0D : 3.0D + rnd.nextDouble() * 7.0D;
            int x = Mth.floor(base.getX() + Math.cos(angle) * radius);
            int z = Mth.floor(base.getZ() + Math.sin(angle) * radius);

            BlockPos spot = findStandingSpot(level, x, base.getY(), z);
            if (spot == null) {
                continue;
            }

            double dx = spot.getX() + 0.5D - player.getX();
            double dz = spot.getZ() + 0.5D - player.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < (force ? 10.0D : 18.0D) || dist > 46.0D) {
                continue;
            }
            if (!force) {
                double dot = (view.x * dx + view.z * dz) / (viewLen * Math.max(0.001D, dist));
                if (dot > 0.35D) {
                    continue;      // never pop into view
                }
            }

            HollowGrinEntity host = ModEntities.HOLLOWGRIN.get().create(level);
            if (host == null) {
                return false;
            }
            host.moveTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, rnd.nextFloat() * 360.0F, 0.0F);
            host.setTargetPlayer(player);
            level.addFreshEntity(host);
            return true;
        }
        return false;
    }

    private static BlockPos findStandingSpot(ServerLevel level, int x, int baseY, int z) {
        for (int dy = 3; dy >= -3; dy--) {
            BlockPos p = new BlockPos(x, baseY + dy, z);
            if (!level.isPositionEntityTicking(p)) {
                return null;
            }
            BlockPos below = p.below();
            if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                continue;
            }
            if (!level.getFluidState(p).isEmpty()) {
                continue;
            }
            AABB box = EntityDimensions.scalable(0.6F, 1.95F).makeBoundingBox(x + 0.5D, p.getY(), z + 0.5D);
            if (level.noCollision(box)) {
                return p;
            }
        }
        return null;
    }
}
