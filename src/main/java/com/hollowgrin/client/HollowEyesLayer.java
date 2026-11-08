package com.hollowgrin.client;

import com.hollowgrin.HollowGrinMod;
import com.hollowgrin.entity.HollowGrinEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/** Fullbright layer: the small red eyeballs (and the red glow inside the open mouth) shine in the dark. */
public class HollowEyesLayer extends EyesLayer<HollowGrinEntity, HostModel> {
    private static final RenderType GLOW = RenderType.eyes(
            new ResourceLocation(HollowGrinMod.MODID, "textures/entity/host_glow.png"));

    public HollowEyesLayer(RenderLayerParent<HollowGrinEntity, HostModel> parent) {
        super(parent);
    }

    @Override
    public RenderType renderType() {
        return GLOW;
    }
}
