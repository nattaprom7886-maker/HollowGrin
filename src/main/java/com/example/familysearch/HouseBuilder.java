package com.example.familysearch;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.BedBlock;

public class HouseBuilder {

    public static final String NOTE_TEXT =
            "ฉันและลูกของเรา ทนไม่ไหวแล้ว เราจะไปจากคุณแล้ว\n\nอย่าตามหาเรา";

    public static ItemStack noteBook() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = book.getOrCreateTag();
        tag.putString("title", "โน้ตของภรรยา");
        tag.putString("author", "ภรรยา");
        ListTag pages = new ListTag();
        pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(NOTE_TEXT))));
        tag.put("pages", pages);
        return book;
    }

    /**
     * สร้างบ้านไม้เล็ก ๆ ขนาด 7x7 (ภายใน 5x5)
     * o = มุมล่างซ้ายของบ้าน (ระดับพื้นดิน)  family=true คือบ้านใหม่ของเมียและลูก
     */
    public static void build(ServerLevel level, BlockPos o, boolean family) {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState planks = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState log = Blocks.OAK_LOG.defaultBlockState();

        // เคลียร์พื้นที่ (ต้นไม้ ใบไม้ ฯลฯ)
        for (int x = -1; x <= 7; x++)
            for (int z = -1; z <= 7; z++)
                for (int y = 0; y <= 6; y++)
                    level.setBlock(o.offset(x, y, z), air, 2);

        // ฐานรากและพื้น
        for (int x = 0; x <= 6; x++) {
            for (int z = 0; z <= 6; z++) {
                for (int y = -2; y >= -4; y--) {
                    BlockPos p = o.offset(x, y, z);
                    BlockState s = level.getBlockState(p);
                    if (s.isAir() || !s.getFluidState().isEmpty() || s.canBeReplaced())
                        level.setBlock(p, Blocks.DIRT.defaultBlockState(), 2);
                }
                level.setBlock(o.offset(x, -1, z), planks, 2);
            }
        }

        // ผนัง + หลังคา
        for (int x = 0; x <= 6; x++) {
            for (int z = 0; z <= 6; z++) {
                boolean edge = x == 0 || x == 6 || z == 0 || z == 6;
                boolean corner = (x == 0 || x == 6) && (z == 0 || z == 6);
                if (edge) {
                    for (int y = 0; y <= 3; y++)
                        level.setBlock(o.offset(x, y, z), corner ? log : planks, 2);
                }
                level.setBlock(o.offset(x, 4, z), planks, 2);
            }
        }

        // หน้าต่าง
        BlockState glass = Blocks.GLASS_PANE.defaultBlockState();
        level.setBlock(o.offset(0, 1, 3), glass, 2);
        level.setBlock(o.offset(6, 1, 3), glass, 2);
        level.setBlock(o.offset(3, 1, 6), glass, 2);
        level.setBlock(o.offset(0, 2, 3), glass, 2);
        level.setBlock(o.offset(6, 2, 3), glass, 2);

        // ประตู (ด้าน z=0)
        BlockState doorLow = Blocks.OAK_DOOR.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
        BlockState doorUp = doorLow.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
        level.setBlock(o.offset(3, 0, 0), doorLow, 3);
        level.setBlock(o.offset(3, 1, 0), doorUp, 3);

        // เตียง
        placeBed(level, o.offset(1, 0, 1));
        // โต๊ะคราฟ + เตาเผา
        level.setBlock(o.offset(5, 0, 1), Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
        level.setBlock(o.offset(5, 0, 2), Blocks.FURNACE.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST), 3);
        // ตะเกียง
        level.setBlock(o.offset(1, 0, 5), Blocks.LANTERN.defaultBlockState(), 3);

        if (!family) {
            // แท่นวางหนังสือ + โน้ตของภรรยา
            BlockPos lec = o.offset(3, 0, 5);
            level.setBlock(lec, Blocks.LECTERN.defaultBlockState()
                    .setValue(LecternBlock.FACING, Direction.NORTH), 3);
            BlockEntity be = level.getBlockEntity(lec);
            if (be instanceof LecternBlockEntity lectern) {
                lectern.setBook(noteBook());
                level.setBlock(lec, level.getBlockState(lec).setValue(LecternBlock.HAS_BOOK, true), 3);
            }
        } else {
            // บ้านใหม่: เตียงที่สอง (ของลูก) + เค้ก
            placeBed(level, o.offset(1, 0, 3));
            level.setBlock(o.offset(3, 0, 5), Blocks.CAKE.defaultBlockState(), 3);
        }
    }

    private static void placeBed(ServerLevel level, BlockPos foot) {
        BlockState bed = Blocks.RED_BED.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST);
        level.setBlock(foot, bed.setValue(BedBlock.PART, BedPart.FOOT), 2);
        level.setBlock(foot.east(), bed.setValue(BedBlock.PART, BedPart.HEAD), 2);
    }
}
