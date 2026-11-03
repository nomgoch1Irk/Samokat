package com.escooter.registry;

import com.escooter.EScooterMod;
import com.escooter.entity.ElectricScooterEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EScooterMod.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.escooter.main"))
                    .icon(() -> new ItemStack(ModItems.YELLOW_SCOOTER.get()))
                    .displayItems((params, output) -> {
                        // Полностью заряженные самокаты обоих цветов
                        for (int variant = 0; variant < ElectricScooterEntity.VARIANT_COUNT; variant++) {
                            ItemStack full = new ItemStack(ModItems.scooterForVariant(variant));
                            CompoundTag tag = full.getOrCreateTag();
                            tag.putInt("Charge", ElectricScooterEntity.MAX_CHARGE);
                            output.accept(full);
                        }
                        output.accept(ModItems.BATTERY.get());
                    })
                    .build());
}
