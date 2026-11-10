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
            case HollowGrinEntity.PRETEND:
                return 0.30F + Math.min(1.0F, st / 8.0F) * 0.6F + Mth.sin(age * 1.1F) * 0.04F;
            case HollowGrinEntity.TAUNT: {
                if (st < 10.0F) {
                    return Mth.lerp(st / 10.0F, 0.9F, 0.04F);          // mouth snaps shut
                }
                boolean speaking = st >= HollowGrinEntity.TAUNT_SPEECH_AT
                        && st < HollowGrinEntity.TAUNT_SPEECH_AT + HollowGrinEntity.TAUNT_SPEECH_TICKS;
                return speaking ? 0.04F + 0.09F * Math.max(0.0F, Mth.sin(age * 0.9F)) : 0.04F;
            }
            case HollowGrinEntity.PULL:
                return 1.05F + Mth.sin(age * 0.6F) * 0.03F;
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
            case HollowGrinEntity.PULL:
                return 1.0F;
            case HollowGrinEntity.PRETEND:
                return 0.55F;                       // arms flail upward but fall short
            case HollowGrinEntity.TAUNT:
                return Math.max(0.0F, 0.55F - st / 10.0F * 0.55F);
            case HollowGrinEntity.STUNNED:
                return 0.7F;
            default:
                return 0.0F;
        }
    }
}
