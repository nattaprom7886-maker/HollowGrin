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
 * The true body of Mr. Hollowgrin (v1.7, redrawn after the concept sketch): tall and lean, huge shoulders, long neck,
 * long narrow face with big ears, huge black hollow eyes with tiny red eyeballs, ear-to-ear needle-tooth grin,
 * tall top hat with a wide flat brim, overalls, white gloves, jointed arms (shoulder + elbow).
 * Texture layout: 128 x 192 (see tools/make_textures.py - the UV offsets below must stay in sync).
 *  */
public class HostModel extends EntityModel<HollowGrinEntity> {
    private final ModelPart root;
    private final ModelPart body, head, jaw, hat, armL, armR, foreL, foreR, legL, legR, eyeL, eyeR;
    private final ModelPart mawL, mawR, mawL2, mawR2;

    public HostModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.legL = root.getChild("legL");
        this.legR = root.getChild("legR");
        this.head = body.getChild("head");
        this.armL = body.getChild("armL");
        this.armR = body.getChild("armR");
        this.foreL = armL.getChild("foreL");
        this.foreR = armR.getChild("foreR");
        this.jaw = head.getChild("jaw");
        this.hat = head.getChild("hat");
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

        // long, thin legs (overalls + black boots)
        CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 49).addBox(-2.5F, 0.0F, -2.5F, 5, 14, 5);
        r.addOrReplaceChild("legL", leg, PartPose.offset(3.0F, 10.0F, 0.0F));
        r.addOrReplaceChild("legR", leg, PartPose.offset(-3.0F, 10.0F, 0.0F));

        // very broad shoulders, narrow waist, long neck, overalls bib on the chest
        PartDefinition body = r.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-8.0F, -14.0F, -4.0F, 16, 10, 8)          // chest / shoulders
                .texOffs(0, 20).addBox(-5.5F, -4.0F, -3.0F, 11, 4, 6)           // waist
                .texOffs(0, 32).addBox(-4.0F, -13.0F, -5.0F, 8, 8, 1)           // overalls bib
                .texOffs(24, 32).addBox(-2.0F, -18.0F, -2.0F, 4, 5, 4),         // long neck
                PartPose.offset(0.0F, 10.0F, 0.0F));

        // tall narrow face: 10 wide x 12 tall (+4 of jaw), huge hollow eye sockets, big ears
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(68, 0).addBox(-5.0F, -12.0F, -4.5F, 10, 12, 9),
                PartPose.offset(0.0F, -18.0F, 0.0F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(68, 24).addBox(-4.5F, 0.0F, -7.5F, 9, 4, 9),
                PartPose.offset(0.0F, 0.0F, 3.0F));
        head.addOrReplaceChild("nose", CubeListBuilder.create()
                .texOffs(56, 32).addBox(-1.0F, -1.5F, -2.0F, 2, 3, 2),
                PartPose.offset(0.0F, -5.5F, -4.5F));
        head.addOrReplaceChild("earL", CubeListBuilder.create()
                .texOffs(44, 32).addBox(0.0F, -4.0F, -1.5F, 2, 5, 3), PartPose.offset(5.0F, -5.0F, 0.0F));
        head.addOrReplaceChild("earR", CubeListBuilder.create()
                .texOffs(44, 32).addBox(-2.0F, -4.0F, -1.5F, 2, 5, 3), PartPose.offset(-5.0F, -5.0F, 0.0F));

        // tall top hat with a wide flat brim (pivot sits 2px above the head top; the animation code relies on y = -10)
        PartDefinition hat = head.addOrReplaceChild("hat", CubeListBuilder.create()
                .texOffs(0, 100).addBox(-9.0F, -3.0F, -9.0F, 18, 2, 18)          // brim
                .texOffs(68, 42).addBox(-5.5F, -17.0F, -5.5F, 11, 14, 11)        // crown
                .texOffs(68, 70).addBox(-6.0F, -6.0F, -6.0F, 12, 3, 12),         // band
                PartPose.offsetAndRotation(0.0F, -10.0F, 0.0F, 0.0F, 0.0F, 0.05F));
        hat.addOrReplaceChild("bowKnot", CubeListBuilder.create()
                .texOffs(80, 88).addBox(-1.5F, -6.0F, -7.5F, 3, 3, 2), PartPose.ZERO);
        hat.addOrReplaceChild("bowL", CubeListBuilder.create()
                .texOffs(68, 88).addBox(1.5F, -6.5F, -7.0F, 4, 3, 1), PartPose.ZERO);
        hat.addOrReplaceChild("bowR", CubeListBuilder.create()
                .texOffs(68, 88).addBox(-5.5F, -6.5F, -7.0F, 4, 3, 1), PartPose.ZERO);

        CubeListBuilder eye = CubeListBuilder.create().texOffs(92, 88).addBox(-0.5F, -0.5F, -0.5F, 1, 1, 1);
        head.addOrReplaceChild("eyeL", eye, PartPose.offset(2.5F, -7.5F, -4.3F));
        head.addOrReplaceChild("eyeR", eye, PartPose.offset(-2.5F, -7.5F, -4.3F));

        // long muscular arms: shoulder -> elbow -> white glove
        CubeListBuilder upper = CubeListBuilder.create().texOffs(0, 70).addBox(-2.5F, -2.5F, -2.5F, 5, 9, 5);
        CubeListBuilder fore = CubeListBuilder.create().texOffs(0, 86).addBox(-2.5F, 0.0F, -2.5F, 5, 9, 5);
        CubeListBuilder glove = CubeListBuilder.create().texOffs(28, 70).addBox(-2.5F, 0.0F, -2.5F, 5, 6, 5);
        PartDefinition aL = body.addOrReplaceChild("armL", upper, PartPose.offset(10.5F, -12.0F, 0.0F));
        aL.addOrReplaceChild("foreL", fore, PartPose.offset(0.0F, 6.0F, 0.0F))
                .addOrReplaceChild("handL", glove, PartPose.offset(0.0F, 8.0F, 0.0F));
        PartDefinition aR = body.addOrReplaceChild("armR", upper, PartPose.offset(-10.5F, -12.0F, 0.0F));
        aR.addOrReplaceChild("foreR", fore, PartPose.offset(0.0F, 6.0F, 0.0F))
                .addOrReplaceChild("handR", glove, PartPose.offset(0.0F, 8.0F, 0.0F));

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

    // ------------------------------------------------------------------ animation

    @Override
    public void setupAnim(HollowGrinEntity e, float limbSwing, float limbSwingAmount, float age,
                          float netYaw, float pitch) {
        int s = e.getState();
        float st = e.getStateTicks() + (age - e.tickCount);
        float amt = Math.min(limbSwingAmount, 1.0F);
        float swing = limbSwing * 0.55F;

        // reset everything that special animations may have moved
        body.y = 10.0F;
        hat.x = 0.0F; hat.y = -10.0F; hat.xRot = 0.0F; hat.zRot = 0.05F;
        armL.yRot = 0.0F; armR.yRot = 0.0F;
        foreL.yRot = 0.0F; foreR.yRot = 0.0F;

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
        foreL.xRot = -0.12F - 0.25F * amt;       // elbows bend a little when walking
        foreR.xRot = -0.12F - 0.25F * amt;

        if (s == HollowGrinEntity.HOST || s == HollowGrinEntity.RETREAT || s == HollowGrinEntity.STUNNED) {
            // polite host: gloved hands clasped in front of the belly, fingers tapping
            float tap = Mth.sin(age * 0.3F) * 0.04F;
            armL.xRot = -0.6F + tap; armL.yRot = 0.55F; armL.zRot = 0.0F;
            armR.xRot = -0.6F - tap; armR.yRot = -0.55F; armR.zRot = 0.0F;
            foreL.xRot = -0.95F; foreR.xRot = -0.95F;
        } else if (s == HollowGrinEntity.CHASE) {
            armL.xRot = -0.4F + Mth.sin(age * 0.9F) * 0.25F;
            armR.xRot = -0.4F - Mth.sin(age * 0.9F) * 0.25F;
            foreL.xRot = -0.3F; foreR.xRot = -0.3F;
        }

        if (s == HollowGrinEntity.FAREWELL) {
            farewell(st);
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
        eyeL.x = 2.5F + tw * 0.3F;
        eyeR.x = -2.5F + tw * 0.3F;
        eyeL.y = -7.5F + Mth.cos(age * 0.29F) * 0.25F;
        eyeR.y = -7.5F + Mth.cos(age * 0.29F + 1.0F) * 0.25F;
    }

    // ------------------------------------------------------------------ farewell routine
    // k = ticks since FAREWELL began. Phases are blended on top of a rest pose with smooth windows:
    //   clap ~8..64 | throw ~56..96 (diamond leaves the hand at tick 80) | tip hat + bow ~84..156

    private static final int LUX = 0, LUY = 1, LUZ = 2, RUX = 3, RUY = 4, RUZ = 5, LFX = 6, RFX = 7,
            BODYX = 8, BODYY = 9, BODYYAW = 10, HEADX = 11, HEADZ = 12, HATPX = 13, HATY = 14, HATX = 15, HATZ = 16,
            N = 17;

    private static float sm(float x) {
        x = Mth.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    private static final float[] REST = new float[N];
    static {
        REST[LUZ] = -0.06F; REST[RUZ] = 0.06F; REST[LFX] = -0.12F; REST[RFX] = -0.12F;
        REST[HATY] = -10.0F; REST[HATZ] = 0.05F;
    }

    private static void clapPose(float k, float[] o) {
        float ph = (k - 12.0F) * 1.05F;
        float c = 0.5F + 0.5F * Mth.sin(ph);
        float iy = 0.35F + 0.45F * c;
        o[LUX] = -0.85F; o[LUY] = iy; o[LUZ] = 0.0F;
        o[RUX] = -0.85F; o[RUY] = -iy; o[RUZ] = 0.0F;
        o[LFX] = -1.05F; o[RFX] = -1.05F;
        o[BODYX] = -0.08F;
        o[BODYY] = -0.5F * Math.abs(Mth.sin(ph));                 // little bounce on every clap
        o[HEADX] = 0.12F * Mth.sin(ph + 0.6F);                    // nodding
        o[HEADZ] = 0.08F * Mth.sin((k - 12.0F) * 0.5F);           // swaying
        o[HATY] = -10.0F;
        o[HATZ] = 0.05F + 0.05F * Mth.sin(ph + 1.0F);             // hat wobble
    }

    private static void throwPose(float k, float[] o) {
        float t = Mth.clamp((k - 62.0F) / 26.0F, 0.0F, 1.0F);
        float w = sm(t / 0.55F);                 // wind up
        float r = sm((t - 0.55F) / 0.2F);        // release
        float f = sm((t - 0.75F) / 0.25F);       // follow through
        float rux = Mth.lerp(w, -0.25F, 1.15F);
        rux = Mth.lerp(r, rux, -1.45F);
        rux = Mth.lerp(f, rux, -0.9F);
        float rfx = Mth.lerp(w, -0.12F, -1.7F);  // cocked elbow, then the whip
        rfx = Mth.lerp(r, rfx, -0.08F);
        rfx = Mth.lerp(f, rfx, -0.5F);
        float yaw = Mth.lerp(w, 0.0F, -0.35F);
        yaw = Mth.lerp(r, yaw, 0.28F);
        yaw = Mth.lerp(f, yaw, 0.1F);
        o[LUX] = -0.55F; o[LUY] = 0.45F; o[LUZ] = 0.0F;
        o[RUX] = rux; o[RUY] = 0.0F; o[RUZ] = 0.1F;
        o[LFX] = -0.9F; o[RFX] = rfx;
        o[BODYX] = 0.05F * w;
        o[BODYYAW] = yaw;
        o[HATY] = -10.0F; o[HATZ] = 0.05F;
    }

    private static void tipPose(float k, float[] o) {
        float t = k - 88.0F;
        float arm = sm(t / 12.0F) * (1.0F - sm((t - 54.0F) / 10.0F));
        float up = sm((t - 9.0F) / 5.0F) * (1.0F - sm((t - 50.0F) / 5.0F));
        float bow = sm((t - 16.0F) / 16.0F) * (1.0F - sm((t - 42.0F) / 14.0F));
        o[LUX] = -0.9F * bow; o[LUY] = 0.55F * bow; o[LUZ] = 0.0F;
        o[RUX] = -1.75F * arm; o[RUY] = -0.15F * arm; o[RUZ] = 0.06F - 0.55F * arm;
        o[LFX] = -1.2F * bow - 0.12F * (1.0F - bow);
        o[RFX] = -2.15F * arm - 0.12F * (1.0F - arm);
        o[BODYX] = 0.6F * bow;                                    // gentleman's bow
        o[HEADX] = 0.25F * bow;
        o[HATPX] = -4.5F * up; o[HATY] = -10.0F - 4.0F * up;      // hat slides to the raised hand and tilts
        o[HATX] = -0.15F * up; o[HATZ] = 0.05F - 0.45F * up;
    }

    private void farewell(float k) {
        float wc = sm((k - 8.0F) / 8.0F) * (1.0F - sm((k - 56.0F) / 8.0F));
        float wt = sm((k - 56.0F) / 8.0F) * (1.0F - sm((k - 88.0F) / 8.0F));
        float wh = sm((k - 84.0F) / 10.0F) * (1.0F - sm((k - 146.0F) / 10.0F));
        float[] c = new float[N], t = new float[N], h = new float[N], v = new float[N];
        System.arraycopy(REST, 0, c, 0, N); System.arraycopy(REST, 0, t, 0, N); System.arraycopy(REST, 0, h, 0, N);
        clapPose(k, c); throwPose(k, t); tipPose(k, h);
        for (int i = 0; i < N; i++) {
            v[i] = REST[i] + wc * (c[i] - REST[i]) + wt * (t[i] - REST[i]) + wh * (h[i] - REST[i]);
        }
        if (k < 10.0F) {
            v[BODYX] += -0.12F * Mth.sin(k * 0.3F);          // recoil right after the transformation
        }
        armL.xRot = v[LUX]; armL.yRot = v[LUY]; armL.zRot = v[LUZ];
        armR.xRot = v[RUX]; armR.yRot = v[RUY]; armR.zRot = v[RUZ];
        foreL.xRot = v[LFX]; foreR.xRot = v[RFX];
        body.xRot = v[BODYX]; body.y = 10.0F + v[BODYY]; body.yRot = v[BODYYAW];
        head.xRot += v[HEADX]; head.zRot = v[HEADZ];
        hat.x = v[HATPX]; hat.y = v[HATY]; hat.xRot = v[HATX]; hat.zRot = v[HATZ];
    }

    @Override
    public void renderToBuffer(PoseStack ps, VertexConsumer buf, int light, int overlay,
                               float r, float g, float b, float a) {
        root.render(ps, buf, light, overlay, r, g, b, a);
    }
}
