package com.escooter;

import com.escooter.registry.ModCreativeTabs;
import com.escooter.registry.ModEntities;
import com.escooter.registry.ModItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(EScooterMod.MODID)
public class EScooterMod {
    public static final String MODID = "escooter";

    public EScooterMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
    }
}
