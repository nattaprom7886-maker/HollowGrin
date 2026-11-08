package com.hollowgrin.registry;

import com.hollowgrin.HollowGrinMod;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, HollowGrinMod.MODID);

    public static final RegistryObject<ForgeSpawnEggItem> SPAWN_EGG = ITEMS.register("hollowgrin_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.HOLLOWGRIN, 0xE8DDC9, 0x8E0F0F, new Item.Properties()));

    public static void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(SPAWN_EGG);
        }
    }

    private ModItems() {}
}
