package com.hollowgrin;

import net.minecraftforge.common.ForgeConfigSpec;

public final class HollowConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.BooleanValue NATURAL_SPAWN;
    public static final ForgeConfigSpec.BooleanValue LETHAL;
    public static final ForgeConfigSpec.IntValue VILLAGE_SECONDS;
    public static final ForgeConfigSpec.IntValue COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.IntValue STARE_SECONDS;
    public static final ForgeConfigSpec.IntValue GRAB_BREAK_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue CHASE_SPEED;
    public static final ForgeConfigSpec.BooleanValue INTRO_ENABLED;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("hollowgrin");
        ENABLED = b.comment("Master switch. false = the mod never spawns him (command/egg still work).")
                .define("enabled", true);
        NATURAL_SPAWN = b.comment("Let him show up by himself after you stay in a village long enough.")
                .define("naturalSpawn", true);
        LETHAL = b.comment("true = the squeeze kills. false = the squeeze deals 8 damage, then he lets go and leaves.")
                .define("lethal", true);
        VILLAGE_SECONDS = b.comment("Seconds spent near 2+ villagers before he may arrive.")
                .defineInRange("villageSeconds", 300, 20, 100000);
        COOLDOWN_SECONDS = b.comment("Seconds spent in villages after an encounter before he can arrive again.")
                .defineInRange("cooldownSeconds", 600, 0, 100000);
        STARE_SECONDS = b.comment("Seconds of total staring (at long range) before the disguise breaks. Closer = faster.")
                .defineInRange("stareSeconds", 6, 1, 120);
        GRAB_BREAK_DAMAGE = b.comment("Total damage you must deal while he holds you to make him drop you.")
                .defineInRange("grabBreakDamage", 24, 1, 1000);
        CHASE_SPEED = b.comment("Chase speed multiplier. Lower it if he feels too fast, raise it for more terror.")
                .defineInRange("chaseSpeed", 1.0D, 0.3D, 3.0D);
        INTRO_ENABLED = b.comment("Play the black-screen red-eyes intro once when you first enter a NEW Survival world (normal worlds only, not Superflat/Hardcore).")
                .define("introEnabled", true);
        b.pop();
        SPEC = b.build();
    }

    private HollowConfig() {}
}
