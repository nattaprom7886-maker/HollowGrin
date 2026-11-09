package com.hollowgrin.client;

import com.hollowgrin.HollowGrinMod;
import com.hollowgrin.entity.HollowGrinEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Normal form: a perfectly ordinary villager (vanilla texture, biome clothes, profession, badge) whose mouth
 * can open. It NEVER changes into another body. The fat top-hatted body (HostModel) is only used for the
 * optional "/hollowgrin summon host" form.
 */
public class HollowGrinRenderer extends MobRenderer<HollowGrinEntity, HostModel> {
    private static final ResourceLocation HOST_TEX =
            new ResourceLocation(HollowGrinMod.MODID, "textures/entity/host.png");
    private static final ResourceLocation VILLAGER_TEX =
            new ResourceLocation("textures/entity/villager/villager.png");

    private final MobRenderer<HollowGrinEntity, VillagerMawModel> villager;

    public HollowGrinRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HostModel(ctx.bakeLayer(ClientSetup.HOST_LAYER)), 0.9F);
        this.addLayer(new HollowEyesLayer(this));
        this.villager = new MobRenderer<HollowGrinEntity, VillagerMawModel>(
                ctx, new VillagerMawModel(ctx.bakeLayer(ClientSetup.DISGUISE_LAYER)), 0.5F) {
            {
                this.addLayer(new VillagerProfessionLayer<HollowGrinEntity, VillagerMawModel>(
                        this, ctx.getResourceManager(), "villager"));
                this.addLayer(new MawLayer(this, ctx.bakeLayer(ClientSetup.MAW_LAYER)));
            }

            @Override
            public ResourceLocation getTextureLocation(HollowGrinEntity entity) {
                return VILLAGER_TEX;
            }

            @Override
            protected void scale(HollowGrinEntity entity, PoseStack ps, float partialTicks) {
                ps.scale(0.9375F, 0.9375F, 0.9375F);      // same as the vanilla villager renderer
            }

            @Override
            protected void setupRotations(HollowGrinEntity e, PoseStack ps, float age, float yaw, float pt) {
                super.setupRotations(e, ps, age, yaw, pt);
                if (e.getState() == HollowGrinEntity.REVEAL) {
                    // the villager freezes and trembles as its jaw starts to loosen
                    float k = Math.min(1.0F, (e.getStateTicks() + pt) / 30.0F);
                    ps.translate(Math.sin(age * 2.7F) * 0.04D * k, 0.0D, Math.cos(age * 3.1F) * 0.04D * k);
                }
            }
        };
    }

    @Override
    public void render(HollowGrinEntity entity, float yaw, float partialTicks, PoseStack ps,
                       MultiBufferSource buffer, int light) {
        if (!entity.isTrueForm()) {
            this.villager.render(entity, yaw, partialTicks, ps, buffer, light);
            return;
        }
        super.render(entity, yaw, partialTicks, ps, buffer, light);
    }

    @Override
    protected void scale(HollowGrinEntity entity, PoseStack ps, float partialTicks) {
        ps.scale(0.8F, 0.8F, 0.8F);
    }

    @Override
    public ResourceLocation getTextureLocation(HollowGrinEntity entity) {
        return HOST_TEX;
    }
}
