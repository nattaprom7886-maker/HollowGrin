package com.hollowgrin.client;

import com.hollowgrin.entity.HollowGrinEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Dog trick: while he shows it, a tamed wolf's head floats in front of the villager's chest. */
public class DogHeadLayer extends RenderLayer<HollowGrinEntity, VillagerMawModel> {
    private static final ResourceLocation WOLF_TAME = new ResourceLocation("textures/entity/wolf/wolf_tame.png");

    private final ModelPart head;

    public DogHeadLayer(RenderLayerParent<HollowGrinEntity, VillagerMawModel> parent, ModelPart wolfRoot) {
        super(parent);
        this.head = wolfRoot.hasChild("head") ? wolfRoot.getChild("head") : null;   // missing part = no head, but no crash
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buf, int light, HollowGrinEntity e, float limbSwing,
                       float limbSwingAmount, float partialTick, float age, float netYaw, float pitch) {
        if (this.head == null || e.isInvisible() || e.getState() != HollowGrinEntity.DOG_SHOW) {
            return;
        }
        float st = e.getStateTicks() + (age - e.tickCount);
        float k = Mth.clamp(st / 12.0F, 0.0F, 1.0F);       // it rises into view
        ps.pushPose();
        ps.translate(0.0D, (4.0F + (1.0F - k) * 10.0F) / 16.0F + Mth.sin(age * 0.12F) * 0.012F, -9.0F / 16.0F);
        ps.scale(1.6F, 1.6F, 1.6F);
        this.head.setPos(-1.0F, 0.0F, 0.0F);
        this.head.xRot = 0.0F;
        this.head.yRot = Mth.sin(age * 0.09F) * 0.35F;
        this.head.zRot = 0.25F + Mth.sin(age * 0.2F) * 0.03F;
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(WOLF_TAME));
        this.head.render(ps, vc, light, OverlayTexture.NO_OVERLAY);
        ps.popPose();
    }
}
