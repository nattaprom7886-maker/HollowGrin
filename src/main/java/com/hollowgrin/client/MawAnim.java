package com.hollowgrin.client;

import com.hollowgrin.entity.HollowGrinEntity;
import net.minecraft.util.Mth;

/** Shared animation curves for the villager's mouth (jaw angle in radians, arm extension 0..1). */
public final class MawAnim {
    private MawAnim() {}

    public static float jaw(int state, float st, float age) {
        switch (state) {
            case HollowGrinEntity.REVEAL: {
                float k = Math.min(1.0F, st / 30.0F);
                return 0.30F * k * k + 0.05F * k * Mth.sin(age * 2.5F);
            }
            case HollowGrinEntity.CHASE:
                return 0.30F + Math.min(1.0F, st / 8.0F) * 0.65F + Mth.sin(age * 0.9F) * 0.05F;
            case HollowGrinEntity.GRAB:
                return 1.05F + Mth.sin(age * 0.5F) * 0.03F;
            case HollowGrinEntity.STUNNED:
                return 0.5F;
            case HollowGrinEntity.RETREAT:
                return Math.max(0.0F, 0.5F - st / 20.0F * 0.5F);
            default:
                return 0.0F;
        }
    }

    public static float ext(int state, float st) {
        switch (state) {
            case HollowGrinEntity.CHASE:
                return Math.min(1.0F, st / 12.0F);
            case HollowGrinEntity.GRAB:
                return 1.0F;
            case HollowGrinEntity.STUNNED:
                return 0.7F;
            default:
                return 0.0F;
        }
    }
}
