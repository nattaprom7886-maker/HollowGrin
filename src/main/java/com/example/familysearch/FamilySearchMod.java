package com.example.familysearch;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(FamilySearchMod.MODID)
public class FamilySearchMod {
    public static final String MODID = "familysearch";

    /** ระยะห่างบ้านใหม่ของครอบครัวจากจุดเกิด (บล็อก) */
    public static final int DISTANCE = 3000;

    public FamilySearchMod() {
        MinecraftForge.EVENT_BUS.register(new ModEvents());
    }
}
