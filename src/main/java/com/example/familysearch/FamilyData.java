package com.example.familysearch;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** เก็บสถานะของ mod ต่อโลก (บันทึกลงไฟล์ world) */
public class FamilyData extends SavedData {
    public boolean startBuilt = false;
    public boolean familyBuilt = false;
    public boolean completed = false;
    public BlockPos origin = BlockPos.ZERO;

    public static FamilyData get(ServerLevel any) {
        ServerLevel overworld = any.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(FamilyData::load, FamilyData::new, "familysearch");
    }

    /** จุดศูนย์กลางบ้านใหม่ของเมียและลูก: ห่างจากบ้านเริ่มต้น 3000 บล็อกไปทางทิศตะวันออก (+X) */
    public BlockPos target() {
        return origin.offset(FamilySearchMod.DISTANCE, 0, 0);
    }

    public static FamilyData load(CompoundTag t) {
        FamilyData d = new FamilyData();
        d.startBuilt = t.getBoolean("startBuilt");
        d.familyBuilt = t.getBoolean("familyBuilt");
        d.completed = t.getBoolean("completed");
        d.origin = new BlockPos(t.getInt("ox"), t.getInt("oy"), t.getInt("oz"));
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag t) {
        t.putBoolean("startBuilt", startBuilt);
        t.putBoolean("familyBuilt", familyBuilt);
        t.putBoolean("completed", completed);
        t.putInt("ox", origin.getX());
        t.putInt("oy", origin.getY());
        t.putInt("oz", origin.getZ());
        return t;
    }
}
