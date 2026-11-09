package com.hollowgrin.client;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Inside of the mouth: roof, upper teeth (head space) - floor, tongue, lower teeth (jaw space) - and the two
 * long black arms that unfold from the throat (head space). Texture: maw.png (64x64).
 */
public final class MawParts {
    private MawParts() {}

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();

        // ---- head space
        PartDefinition upper = r.addOrReplaceChild("upper", CubeListBuilder.create(), PartPose.ZERO);
        upper.addOrReplaceChild("roof", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4.0F, -4.4F, -4.0F, 8, 1, 8, new CubeDeformation(0.0F, -0.4F, 0.0F)), PartPose.ZERO);
        upper.addOrReplaceChild("teethF", CubeListBuilder.create().texOffs(0, 10)
                .addBox(-3.5F, -4.0F, -3.95F, 7, 3, 1, new CubeDeformation(0.0F, 0.0F, -0.35F)), PartPose.ZERO);
        upper.addOrReplaceChild("teethL", CubeListBuilder.create().texOffs(0, 15)
                .addBox(-3.9F, -4.0F, -3.5F, 1, 3, 7, new CubeDeformation(-0.35F, 0.0F, 0.0F)), PartPose.ZERO);
        upper.addOrReplaceChild("teethR", CubeListBuilder.create().texOffs(0, 15)
                .addBox(2.9F, -4.0F, -3.5F, 1, 3, 7, new CubeDeformation(-0.35F, 0.0F, 0.0F)), PartPose.ZERO);

        // ---- jaw space (origin = jaw hinge)
        PartDefinition lower = r.addOrReplaceChild("lower", CubeListBuilder.create(), PartPose.ZERO);
        lower.addOrReplaceChild("floor", CubeListBuilder.create().texOffs(32, 0)
                .addBox(-4.0F, -0.6F, -7.5F, 8, 1, 8, new CubeDeformation(0.0F, -0.4F, 0.0F)), PartPose.ZERO);
        lower.addOrReplaceChild("teethF", CubeListBuilder.create().texOffs(16, 10)
                .addBox(-3.5F, -3.0F, -7.45F, 7, 3, 1, new CubeDeformation(0.0F, 0.0F, -0.35F)), PartPose.ZERO);
        lower.addOrReplaceChild("teethL", CubeListBuilder.create().texOffs(16, 15)
                .addBox(-3.9F, -3.0F, -7.0F, 1, 3, 7, new CubeDeformation(-0.35F, 0.0F, 0.0F)), PartPose.ZERO);
        lower.addOrReplaceChild("teethR", CubeListBuilder.create().texOffs(16, 15)
                .addBox(2.9F, -3.0F, -7.0F, 1, 3, 7, new CubeDeformation(-0.35F, 0.0F, 0.0F)), PartPose.ZERO);
        lower.addOrReplaceChild("tongue", CubeListBuilder.create().texOffs(32, 10)
                .addBox(-1.5F, -1.8F, -6.5F, 3, 2, 7), PartPose.ZERO);

        // ---- black arms (head space)
        addArm(r, "armL", 2.4F);
        addArm(r, "armR", -2.4F);
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void addArm(PartDefinition r, String name, float x) {
        PartDefinition seg1 = r.addOrReplaceChild(name, CubeListBuilder.create().texOffs(0, 26)
                .addBox(-1.5F, -1.5F, -12.0F, 3, 3, 12), PartPose.offset(x, -2.6F, 1.0F));
        PartDefinition seg2 = seg1.addOrReplaceChild("seg2", CubeListBuilder.create().texOffs(0, 26)
                .addBox(-1.5F, -1.5F, -12.0F, 3, 3, 12, new CubeDeformation(-0.3F)), PartPose.offset(0.0F, 0.0F, -12.0F));
        PartDefinition palm = seg2.addOrReplaceChild("palm", CubeListBuilder.create().texOffs(32, 26)
                .addBox(-3.0F, -2.5F, -5.0F, 6, 5, 5), PartPose.offset(0.0F, 0.0F, -12.0F));
        for (int i = 0; i < 4; i++) {
            palm.addOrReplaceChild("f" + i, CubeListBuilder.create().texOffs(32, 38)
                    .addBox(0.0F, -0.6F, -5.0F, 1.2F, 1.2F, 5.0F),
                    PartPose.offset(-2.4F + i * 1.6F, 0.0F, -5.0F));
        }
    }
}
