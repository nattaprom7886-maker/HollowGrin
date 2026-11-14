package com.example.familysearch;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ModEvents {

    private static final String WIFE = "ภรรยา";
    private static final String CHILD = "ลูก";
    private static final int TRIGGER_RANGE = 96; // เข้าใกล้เท่านี้ บ้านใหม่ถึงจะถูกสร้าง

    /** เข้าเกมครั้งแรก: สร้างบ้านไม้ที่จุดเกิด ย้ายผู้เล่นเข้าบ้าน */
    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        ServerLevel level = p.getServer().overworld();
        FamilyData d = FamilyData.get(level);
        if (d.startBuilt) return;

        BlockPos sp = level.getSharedSpawnPos();
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, sp.getX(), sp.getZ());
        BlockPos o = new BlockPos(sp.getX() - 3, y, sp.getZ() - 3);

        d.origin = o;
        d.startBuilt = true;
        d.setDirty();

        HouseBuilder.build(level, o, false);
        p.teleportTo(level, o.getX() + 3.5, o.getY(), o.getZ() + 2.5, 0f, 0f);
        p.setRespawnPosition(Level.OVERWORLD, o.offset(1, 0, 1), 0f, true, false);

        // เข็มทิศชี้ไปทางบ้านใหม่ (ลบส่วนนี้ออกถ้าอยากให้ยากขึ้น)
        ItemStack compass = new ItemStack(Items.COMPASS);
        CompoundTag tag = compass.getOrCreateTag();
        tag.put("LodestonePos", NbtUtils.writeBlockPos(d.target()));
        tag.putString("LodestoneDimension", "minecraft:overworld");
        tag.putBoolean("LodestoneTracked", false);
        compass.setHoverName(Component.literal("เข็มทิศตามหา"));
        p.getInventory().add(compass);

        p.sendSystemMessage(Component.literal("บ้านเงียบเหลือเกิน... บนแท่นมีโน้ตวางอยู่"));
    }

    /** เข้าใกล้บ้านใหม่ (ห่าง 3000 บล็อก) แล้วสร้างบ้าน + เมียกับลูก */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!(e.player instanceof ServerPlayer p)) return;
        if (p.tickCount % 40 != 0) return;
        if (p.level().dimension() != Level.OVERWORLD) return;

        ServerLevel level = (ServerLevel) p.level();
        FamilyData d = FamilyData.get(level);
        if (!d.startBuilt || d.familyBuilt) return;

        BlockPos t = d.target();
        double dx = p.getX() - t.getX(), dz = p.getZ() - t.getZ();
        if (dx * dx + dz * dz > TRIGGER_RANGE * TRIGGER_RANGE) return;

        // โหลด/สร้าง chunk ที่ครอบคลุมบ้าน
        int bx = t.getX() - 3, bz = t.getZ() - 3;
        for (int cx : new int[]{bx - 1, bx + 8})
            for (int cz : new int[]{bz - 1, bz + 8})
                level.getChunk(cx >> 4, cz >> 4);

        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, t.getX(), t.getZ());
        BlockPos o = new BlockPos(bx, y, bz);
        HouseBuilder.build(level, o, true);

        spawnFamily(level, o.offset(3, 0, 3), WIFE, false);
        spawnFamily(level, o.offset(2, 0, 4), CHILD, true);

        d.familyBuilt = true;
        d.setDirty();
    }

    private void spawnFamily(ServerLevel level, BlockPos pos, String name, boolean baby) {
        Villager v = new Villager(EntityType.VILLAGER, level);
        v.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
        v.setCustomName(Component.literal(name));
        v.setCustomNameVisible(true);
        v.setPersistenceRequired();
        v.setNoAi(true);
        if (baby) v.setAge(-2000000000); // เด็กตลอดไป
        level.addFreshEntity(v);
    }

    /** คลิกขวาที่เมียหรือลูก = จบเกม */
    @SubscribeEvent
    public void onInteract(PlayerInteractEvent.EntityInteract e) {
        Component n = e.getTarget().getCustomName();
        if (n == null) return;
        String name = n.getString();
        if (!name.equals(WIFE) && !name.equals(CHILD)) return;

        // กันหน้าต่างเทรดของชาวบ้าน (ทั้งฝั่ง client และ server)
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);

        if (e.getLevel().isClientSide()) return;
        if (!(e.getEntity() instanceof ServerPlayer p)) return;

        FamilyData d = FamilyData.get((ServerLevel) e.getLevel());
        if (d.completed) {
            p.sendSystemMessage(Component.literal("<" + name + "> ..."));
            return;
        }
        d.completed = true;
        d.setDirty();

        p.sendSystemMessage(Component.literal("<ภรรยา> ...คุณตามมาจนได้"));
        p.sendSystemMessage(Component.literal("<ลูก> พ่อ!"));
        p.connection.send(new ClientboundSetTitlesAnimationPacket(20, 100, 40));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("คุณตามหาพวกเขาเจอแล้ว")));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("จบ")));
    }
}
