package com.hollowgrin.client;

import com.hollowgrin.entity.HollowGrinEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The true body of Mr. Hollowgrin (v1.1 proportions): round gentleman, wide head, modest top hat with a bow,
 * hollow eyes with small red eyeballs, needle-tooth grin, white gloves - and two long gloved arms hidden in the throat.
 * Texture layout: 128 x 192 (see tools/make_textures.py - the UV offsets below must stay in sync).
 */
public class HostModel extends EntityModel<HollowGrinEntity> {
    private final ModelPart root;
    private final ModelPart body, head, jaw, armL, armR, legL, legR, eyeL, eyeR;
    private final ModelPart mawL, mawR, mawL2, mawR2;

    public HostModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.legL = root.getChild("legL");
        this.legR = root.getChild("legR");
        this.head = body.getChild("head");
        this.armL = body.getChild("armL");
        this.armR = body.getChild("armR");
        this.jaw = head.getChild("jaw");
        this.eyeL = head.getChild("eyeL");
        this.eyeR = head.getChild("eyeR");
        this.mawL = head.getChild("mawL");
        this.mawR = head.getChild("mawR");
        this.mawL2 = mawL.getChild("seg2");
        this.mawR2 = mawR.getChild("seg2");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();

        // legs: hips at y=10, feet at y=24
        CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 49).addBox(-3.0F, 0.0F, -3.0F, 6, 14, 6);
        r.addOrReplaceChild("legL", leg, PartPose.offset(4.5F, 10.0F, 0.0F));
        r.addOrReplaceChild("legR", leg, PartPose.offset(-4.5F, 10.0F, 0.0F));

        // round torso + belly
        PartDefinition body = r.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-10.0F, -16.0F, -7.0F, 20, 16, 14)
                .texOffs(0, 30).addBox(-9.0F, -11.0F, -12.0F, 18, 13, 6),
                PartPose.offset(0.0F, 10.0F, 0.0F));

        // wide head (16x10x14) + hinged jaw (15x5x13)
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(68, 0).addBox(-8.0F, -10.0F, -7.0F, 16, 10, 14),
                PartPose.offset(0.0F, -21.0F, 0.0F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(68, 24).addBox(-7.5F, 0.0F, -11.0F, 15, 5, 13),
                PartPose.offset(0.0F, 0.0F, 4.0F));

        // modest top hat with band and bow
        PartDefinition hat = head.addOrReplaceChild("hat", CubeListBuilder.create()
                .texOffs(0, 96).addBox(-10.0F, -1.0F, -10.0F, 20, 2, 20)
                .texOffs(68, 42).addBox(-6.0F, -13.0F, -6.0F, 12, 12, 12)
                .texOffs(68, 66).addBox(-6.5F, -4.0F, -6.5F, 13, 3, 13),
                PartPose.offsetAndRotation(0.0F, -10.0F, 0.0F, 0.0F, 0.0F, 0.05F));
        hat.addOrReplaceChild("bowKnot", CubeListBuilder.create()
                .texOffs(80, 84).addBox(-1.5F, -3.5F, -8.5F, 3, 3, 2), PartPose.ZERO);
        hat.addOrReplaceChild("bowL", CubeListBuilder.create()
                .texOffs(68, 84).addBox(-6.0F, -4.0F, -7.5F, 4, 3, 1), PartPose.ZERO);
        hat.addOrReplaceChild("bowR", CubeListBuilder.create()
                .texOffs(68, 84).addBox(2.0F, -4.0F, -7.5F, 4, 3, 1), PartPose.ZERO);

        // hollow eyes: tiny red eyeballs floating in dark sockets
        CubeListBuilder eye = CubeListBuilder.create().texOffs(92, 84).addBox(-1.0F, -1.0F, -1.0F, 2, 2, 2);
        head.addOrReplaceChild("eyeL", eye, PartPose.offset(3.0F, -6.0F, -6.7F));
        head.addOrReplaceChild("eyeR", eye, PartPose.offset(-3.0F, -6.0F, -6.7F));

        // arms with white gloves (hang to hip height)
        CubeListBuilder arm = CubeListBuilder.create().texOffs(0, 70).addBox(-3.5F, -2.0F, -3.5F, 7, 16, 7);
        CubeListBuilder glove = CubeListBuilder.create().texOffs(28, 70).addBox(-3.5F, 0.0F, -3.5F, 7, 6, 7);
        body.addOrReplaceChild("armL", arm, PartPose.offset(13.0F, -13.0F, 0.0F))
                .addOrReplaceChild("handL", glove, PartPose.offset(0.0F, 13.0F, 0.0F));
        body.addOrReplaceChild("armR", arm, PartPose.offset(-13.0F, -13.0F, 0.0F))
                .addOrReplaceChild("handR", glove, PartPose.offset(0.0F, 13.0F, 0.0F));

        // arms that come out of the mouth (hidden unless CHASE / GRAB)
        addMawArm(head, "mawL", 3.0F);
        addMawArm(head, "mawR", -3.0F);

        return LayerDefinition.create(mesh, 128, 192);
    }

    private static void addMawArm(PartDefinition head, String name, float x) {
        PartDefinition seg1 = head.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(0, 122).addBox(-1.5F, -1.5F, -14.0F, 3, 3, 14),
                PartPose.offset(x, 1.5F, 2.0F));
        PartDefinition seg2 = seg1.addOrReplaceChild("seg2", CubeListBuilder.create()
                .texOffs(0, 122).addBox(-1.5F, -1.5F, -14.0F, 3, 3, 14, new CubeDeformation(-0.3F)),
                PartPose.offset(0.0F, 0.0F, -14.0F));
        PartDefinition palm = seg2.addOrReplaceChild("palm", CubeListBuilder.create()
                .texOffs(50, 122).addBox(-3.0F, -2.5F, -5.0F, 6, 5, 5),
                PartPose.offset(0.0F, 0.0F, -14.0F));
        for (int i = 0; i < 4; i++) {
            palm.addOrReplaceChild("f" + i, CubeListBuilder.create()
                    .texOffs(36, 122).addBox(0.0F, -0.6F, -5.0F, 1.2F, 1.2F, 5.0F),
                    PartPose.offset(-2.4F + i * 1.6F, 0.0F, -5.0F));
        }
    }

    @Override
    public void setupAnim(HollowGrinEntity e, float limbSwing, float limbSwingAmount, float age,
                          float netYaw, float pitch) {
        int s = e.getState();
        float st = e.getStateTicks() + (age - e.tickCount);
        float amt = Math.min(limbSwingAmount, 1.0F);
        float swing = limbSwing * 0.55F;

        head.yRot = netYaw * Mth.DEG_TO_RAD;
        head.xRot = pitch * Mth.DEG_TO_RAD * 0.8F;
        head.zRot = (s == HollowGrinEntity.GRAB) ? 0.0F : Mth.sin(age * 0.04F) * 0.08F;

        legR.xRot = Mth.cos(swing) * 1.0F * amt;
        legL.xRot = Mth.cos(swing + Mth.PI) * 1.0F * amt;
        body.zRot = Mth.cos(swing) * 0.05F * amt;
        body.yRot = 0.0F;
        body.xRot = (s == HollowGrinEntity.CHASE) ? 0.14F : 0.0F;

        armR.xRot = Mth.cos(swing + Mth.PI) * 0.7F * amt;
        armL.xRot = Mth.cos(swing) * 0.7F * amt;
        armL.zRot = -0.06F;
        armR.zRot = 0.06F;
        if (s == HollowGrinEntity.HOST || s == HollowGrinEntity.RETREAT || s == HollowGrinEntity.STUNNED) {
            float tap = Mth.sin(age * 0.3F) * 0.04F;
            armL.xRot = -0.95F + tap; armL.zRot = 0.30F;
            armR.xRot = -0.95F - tap; armR.zRot = -0.30F;
        } else if (s == HollowGrinEntity.CHASE) {
            armL.xRot = -0.4F + Mth.sin(age * 0.9F) * 0.25F;
            armR.xRot = -0.4F - Mth.sin(age * 0.9F) * 0.25F;
        }

        float jawOpen;
        boolean maw = false;
        float ext = 0.0F;
        if (s == HollowGrinEntity.CHASE) {
            jawOpen = Math.min(1.0F, st / 10.0F) * 0.85F + Mth.sin(age * 0.9F) * 0.05F;
            maw = true;
            ext = Math.min(1.0F, st / 14.0F);
        } else if (s == HollowGrinEntity.GRAB) {
            jawOpen = 1.0F + Mth.sin(age * 0.5F) * 0.03F;
            maw = true;
            ext = 1.0F;
        } else if (s == HollowGrinEntity.STUNNED) {
            jawOpen = 0.55F;
        } else {
            jawOpen = 0.04F + (Mth.sin(age * 0.09F) + 1.0F) * 0.02F;
        }
        jaw.xRot = jawOpen;

        mawL.visible = maw;
        mawR.visible = maw;
        if (maw) {
            float wob = Mth.sin(age * 0.7F);
            if (s == HollowGrinEntity.GRAB) {
                mawL.xRot = -0.60F + wob * 0.03F;
                mawR.xRot = -0.60F - wob * 0.03F;
                mawL.yRot = 0.22F;
                mawR.yRot = -0.22F;
                mawL2.xRot = -0.30F;
                mawR2.xRot = -0.30F;
            } else {
                mawL.xRot = -0.12F + Mth.sin(age * 0.9F) * 0.16F;
                mawR.xRot = -0.12F + Mth.sin(age * 0.9F + 2.0F) * 0.16F;
                mawL.yRot = 0.18F + wob * 0.10F;
                mawR.yRot = -0.18F - wob * 0.10F;
                mawL2.xRot = Mth.sin(age * 1.1F) * 0.25F;
                mawR2.xRot = Mth.sin(age * 1.1F + 1.5F) * 0.25F;
            }
            float z = Math.max(0.05F, ext);
            mawL.zScale = z;
            mawR.zScale = z;
        }

        float tw = Mth.sin(age * 0.37F) * Mth.sin(age * 0.11F);
        eyeL.x = 3.0F + tw * 0.6F;
        eyeR.x = -3.0F + tw * 0.6F;
        eyeL.y = -6.0F + Mth.cos(age * 0.29F) * 0.3F;
        eyeR.y = -6.0F + Mth.cos(age * 0.29F + 1.0F) * 0.3F;
    }

    @Override
    public void renderToBuffer(PoseStack ps, VertexConsumer buf, int light, int overlay,
                               float r, float g, float b, float a) {
        root.render(ps, buf, light, overlay, r, g, b, a);
    }
}
