package com.hollowgrin;

import com.hollowgrin.network.ModNetwork;
import com.hollowgrin.registry.ModEntities;
import com.hollowgrin.registry.ModItems;
import com.hollowgrin.registry.ModSounds;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(HollowGrinMod.MODID)
public class HollowGrinMod {
    public static final String MODID = "hollowgrin";

    public HollowGrinMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModNetwork.register();
        ModSounds.SOUNDS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        modBus.addListener(ModEntities::onAttributes);
        modBus.addListener(ModItems::onCreativeTab);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, HollowConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(new HollowEvents());
    }
}
