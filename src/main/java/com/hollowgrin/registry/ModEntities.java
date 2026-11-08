package com.hollowgrin.registry;

import com.hollowgrin.HollowGrinMod;
import com.hollowgrin.entity.HollowGrinEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, HollowGrinMod.MODID);

    public static final RegistryObject<EntityType<HollowGrinEntity>> HOLLOWGRIN = ENTITIES.register("hollowgrin",
            () -> EntityType.Builder.of(HollowGrinEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build(HollowGrinMod.MODID + ":hollowgrin"));

    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(HOLLOWGRIN.get(), HollowGrinEntity.createAttributes().build());
    }

    private ModEntities() {}
}
