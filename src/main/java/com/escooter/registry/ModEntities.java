package com.escooter.registry;

import com.escooter.EScooterMod;
import com.escooter.entity.ElectricScooterEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, EScooterMod.MODID);

    public static final RegistryObject<EntityType<ElectricScooterEntity>> ELECTRIC_SCOOTER =
            ENTITIES.register("electric_scooter", () ->
                    EntityType.Builder.<ElectricScooterEntity>of(ElectricScooterEntity::new, MobCategory.MISC)
                            .sized(0.8F, 1.2F)
                            .clientTrackingRange(10)
                            .updateInterval(1)
                            .build("electric_scooter"));
}
