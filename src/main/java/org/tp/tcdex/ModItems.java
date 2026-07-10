package org.tp.tcdex;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Tcdex.MODID);
    public static final RegistryObject<Item> MY_LOVE=
            ITEMS.register("my_love",()->new Item(new Item.Properties()));





    public static void registerItems(IEventBus bus) {
        ITEMS.register(bus);
    }
}
