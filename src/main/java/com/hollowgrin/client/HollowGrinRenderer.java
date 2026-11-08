package com.hollowgrin.client;

import com.hollowgrin.HollowGrinMod;
import com.hollowgrin.entity.HollowGrinEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Disguise = a perfectly normal villager (vanilla villager model, texture, biome clothes, profession, badge).
 * True body = HostModel. Which one is drawn depends on the synced state.
 */
public class HollowGrinRenderer extends MobRenderer<HollowGrinEntity, HostModel> {
    private static final ResourceLocation HOST_TEX =
            new ResourceLocation(HollowGrinMod.MODID, "textures/entity/host.png");
    private static final ResourceLocation VILLAGER_TEX =
            new ResourceLocation("textures/entity/villager/villager.png");

    private final MobRenderer<HollowGrinEntity, VillagerModel<HollowGrinEntity>> disguise;

    public HollowGrinRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HostModel(ctx.bakeLayer(ClientSetup.HOST_LAYER)), 0.9F);
        this.addLayer(new HollowEyesLayer(this));
        this.disguise = new MobRenderer<HollowGrinEntity, VillagerModel<HollowGrinEntity>>(
                ctx, new VillagerModel<HollowGrinEntity>(ctx.bakeLayer(ClientSetup.DISGUISE_LAYER)), 0.5F) {
            {
                this.addLayer(new VillagerProfessionLayer<HollowGrinEntity, VillagerModel<HollowGrinEntity>>(
                        this, ctx.getResourceManager(), "villager"));
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
                    // only at the very end: the "villager" shakes as something inside pushes out
                    float k = Math.min(1.0F, (e.getStateTicks() + pt) / 30.0F);
                    ps.translate(Math.sin(age * 2.7F) * 0.05D * k, 0.0D, Math.cos(age * 3.1F) * 0.05D * k);
                    ps.scale(1.0F, 1.0F + 0.12F * k * (float) Math.sin(age * 1.3F), 1.0F);
                }
            }
        };
    }

    @Override
    public void render(HollowGrinEntity entity, float yaw, float partialTicks, PoseStack ps,
                       MultiBufferSource buffer, int light) {
        if (entity.isDisguised()) {
            this.disguise.render(entity, yaw, partialTicks, ps, buffer, light);
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
