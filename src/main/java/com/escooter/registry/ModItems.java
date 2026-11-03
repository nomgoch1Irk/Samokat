package com.escooter.registry;

import com.escooter.EScooterMod;
import com.escooter.entity.ElectricScooterEntity;
import com.escooter.item.ScooterItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, EScooterMod.MODID);

    public static final RegistryObject<Item> YELLOW_SCOOTER = ITEMS.register("yellow_electric_scooter",
            () -> new ScooterItem(new Item.Properties().stacksTo(1), ElectricScooterEntity.VARIANT_YELLOW));

    public static final RegistryObject<Item> PURPLE_SCOOTER = ITEMS.register("purple_electric_scooter",
            () -> new ScooterItem(new Item.Properties().stacksTo(1), ElectricScooterEntity.VARIANT_PURPLE));

    public static final RegistryObject<Item> BATTERY = ITEMS.register("battery",
            () -> new Item(new Item.Properties().stacksTo(16)));

    /** Предмет-самокат нужного цвета. */
    public static Item scooterForVariant(int variant) {
        return variant == ElectricScooterEntity.VARIANT_PURPLE ? PURPLE_SCOOTER.get() : YELLOW_SCOOTER.get();
    }
}
