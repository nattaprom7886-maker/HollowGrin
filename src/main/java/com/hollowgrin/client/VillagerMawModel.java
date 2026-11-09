package com.hollowgrin.client;

import com.hollowgrin.entity.HollowGrinEntity;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The vanilla villager model, except the head is cut in two: upper head + a jaw that can swing open.
 * UVs are chosen so the closed head looks pixel-identical to a normal villager.
 */
public class VillagerMawModel extends VillagerModel<HollowGrinEntity> {
    private final ModelPart jaw;

    public VillagerMawModel(ModelPart root) {
        super(root);
        ModelPart h = root.getChild("head");
        this.jaw = h.hasChild("jaw") ? h.getChild("jaw") : null;
    }

    public ModelPart getJaw() {
        return this.jaw;
    }

    public static LayerDefinition createBodyLayer() {
        LayerDefinition vanilla = LayerDefinition.create(VillagerModel.createBodyModel(), 64, 64);
        try {
            MeshDefinition mesh = VillagerModel.createBodyModel();
            PartDefinition root = mesh.getRoot();
            PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                    .texOffs(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8, 6, 8), PartPose.ZERO);
            head.addOrReplaceChild("jaw", CubeListBuilder.create()
                    .texOffs(0, 6).addBox(-4.0F, 0.0F, -7.5F, 8, 4, 8), PartPose.offset(0.0F, -4.0F, 3.5F));
            PartDefinition hat = head.addOrReplaceChild("hat", CubeListBuilder.create()
                    .texOffs(32, 0).addBox(-4.0F, -10.0F, -4.0F, 8, 10, 8, new CubeDeformation(0.51F)), PartPose.ZERO);
            hat.addOrReplaceChild("hat_rim", CubeListBuilder.create()
                    .texOffs(30, 47).addBox(-8.0F, -8.0F, -6.0F, 16, 16, 1),
                    PartPose.rotation(-1.5707964F, 0.0F, 0.0F));
            head.addOrReplaceChild("nose", CubeListBuilder.create()
                    .texOffs(24, 0).addBox(-1.0F, -1.0F, -6.0F, 2, 4, 2), PartPose.offset(0.0F, -2.0F, 0.0F));
            LayerDefinition custom = LayerDefinition.create(mesh, 64, 64);
            new VillagerModel<HollowGrinEntity>(custom.bakeRoot());   // sanity check: throws if a part name is wrong
            return custom;
        } catch (RuntimeException ex) {
            return vanilla;      // safe fallback: a plain villager that cannot open its mouth, but no crash
        }
    }

    @Override
    public void setupAnim(HollowGrinEntity e, float limbSwing, float limbSwingAmount, float age,
                          float netYaw, float pitch) {
        super.setupAnim(e, limbSwing, limbSwingAmount, age, netYaw, pitch);
        if (this.jaw != null) {
            float st = e.getStateTicks() + (age - e.tickCount);
            this.jaw.xRot = MawAnim.jaw(e.getState(), st, age);
        }
    }
}
