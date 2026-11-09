package com.hollowgrin.client;

import com.hollowgrin.HollowGrinMod;
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

/** Draws mouth interior, tongue, teeth and the black hands on top of the villager model while its mouth is open. */
public class MawLayer extends RenderLayer<HollowGrinEntity, VillagerMawModel> {
    private static final ResourceLocation TEX = new ResourceLocation(HollowGrinMod.MODID, "textures/entity/maw.png");

    private final ModelPart upper, lower, tongue, armL, armR, armL2, armR2;

    public MawLayer(RenderLayerParent<HollowGrinEntity, VillagerMawModel> parent, ModelPart root) {
        super(parent);
        this.upper = root.getChild("upper");
        this.lower = root.getChild("lower");
        this.tongue = lower.getChild("tongue");
        this.armL = root.getChild("armL");
        this.armR = root.getChild("armR");
        this.armL2 = armL.getChild("seg2");
        this.armR2 = armR.getChild("seg2");
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buf, int light, HollowGrinEntity e, float limbSwing,
                       float limbSwingAmount, float partialTick, float age, float netYaw, float pitch) {
        if (e.isInvisible() || e.isTrueForm()) {
            return;
        }
        VillagerMawModel model = this.getParentModel();
        ModelPart jaw = model.getJaw();
        if (jaw == null) {
            return;
        }
        int state = e.getState();
        float st = e.getStateTicks() + (age - e.tickCount);
        if (MawAnim.jaw(state, st, age) < 0.03F) {
            return;
        }

        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(TEX));
        int ov = OverlayTexture.NO_OVERLAY;

        ps.pushPose();
        model.getHead().translateAndRotate(ps);

        upper.render(ps, vc, light, ov, 1.0F, 1.0F, 1.0F, 1.0F);

        float ext = MawAnim.ext(state, st);
        if (ext > 0.02F) {
            float wob = Mth.sin(age * 0.7F);
            if (state == HollowGrinEntity.GRAB) {
                armL.xRot = -0.45F + wob * 0.03F;
                armR.xRot = -0.45F - wob * 0.03F;
                armL.yRot = 0.20F;
                armR.yRot = -0.20F;
                armL2.xRot = -0.25F;
                armR2.xRot = -0.25F;
            } else {
                armL.xRot = -0.10F + Mth.sin(age * 0.9F) * 0.16F;
                armR.xRot = -0.10F + Mth.sin(age * 0.9F + 2.0F) * 0.16F;
                armL.yRot = 0.16F + wob * 0.10F;
                armR.yRot = -0.16F - wob * 0.10F;
                armL2.xRot = Mth.sin(age * 1.1F) * 0.25F;
                armR2.xRot = Mth.sin(age * 1.1F + 1.5F) * 0.25F;
            }
            float z = Math.max(0.05F, ext);
            armL.zScale = z;
            armR.zScale = z;
            armL.render(ps, vc, light, ov, 1.0F, 1.0F, 1.0F, 1.0F);
            armR.render(ps, vc, light, ov, 1.0F, 1.0F, 1.0F, 1.0F);
        }

        ps.pushPose();
        jaw.translateAndRotate(ps);
        this.tongue.zRot = Mth.sin(age * 0.6F) * 0.12F;
        lower.render(ps, vc, light, ov, 1.0F, 1.0F, 1.0F, 1.0F);
        ps.popPose();

        ps.popPose();
    }
}
