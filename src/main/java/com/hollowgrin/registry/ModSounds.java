package com.hollowgrin.registry;

import com.hollowgrin.HollowGrinMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, HollowGrinMod.MODID);

    public static final RegistryObject<SoundEvent> HUM = reg("hum");
    public static final RegistryObject<SoundEvent> HEARTBEAT = reg("heartbeat");
    public static final RegistryObject<SoundEvent> LAUGH = reg("laugh");
    public static final RegistryObject<SoundEvent> REVEAL = reg("reveal");
    public static final RegistryObject<SoundEvent> JAW = reg("jaw");
    public static final RegistryObject<SoundEvent> GOTCHA = reg("gotcha");
    public static final RegistryObject<SoundEvent> SQUEEZE = reg("squeeze");

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name,
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(HollowGrinMod.MODID, name)));
    }

    private ModSounds() {}
}
