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

    public static final RegistryObject<Item> LIGHT_ESSENCE=
            ITEMS.register("light_essence",()->new Item(new Item.Properties()));

    /** 守护者徽记：命运2 风格纪念品（成就「守护者掌握自己的命运」图标） */
    public static final RegistryObject<Item> GUARDIAN_EMBLEM=
            ITEMS.register("guardian_emblem",()->new Item(new Item.Properties()));




    public static void registerItems(IEventBus bus) {
        ITEMS.register(bus);
    }
}
