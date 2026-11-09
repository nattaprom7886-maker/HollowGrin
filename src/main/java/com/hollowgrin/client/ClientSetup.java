package com.hollowgrin.client;

import com.hollowgrin.HollowGrinMod;
import com.hollowgrin.registry.ModEntities;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = HollowGrinMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    public static final ModelLayerLocation HOST_LAYER =
            new ModelLayerLocation(new ResourceLocation(HollowGrinMod.MODID, "host"), "main");
    public static final ModelLayerLocation DISGUISE_LAYER =
            new ModelLayerLocation(new ResourceLocation(HollowGrinMod.MODID, "disguise"), "main");
    public static final ModelLayerLocation MAW_LAYER =
            new ModelLayerLocation(new ResourceLocation(HollowGrinMod.MODID, "maw"), "main");

    private ClientSetup() {}

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(HOST_LAYER, HostModel::createBodyLayer);
        event.registerLayerDefinition(DISGUISE_LAYER, VillagerMawModel::createBodyLayer);
        event.registerLayerDefinition(MAW_LAYER, MawParts::createLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.HOLLOWGRIN.get(), HollowGrinRenderer::new);
    }
}
