package org.tp.tcdex;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.tp.tcdex.artifact.ArtifactItem;
import org.tp.tcdex.artifact.ArtifactSlot;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Tcdex.MODID);
    public static final RegistryObject<Item> MY_LOVE =
            ITEMS.register("my_love", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> LIGHT_ESSENCE =
            ITEMS.register("light_essence", () -> new Item(new Item.Properties()));

    /** 守护者徽记：命运2 风格纪念品（成就「守护者掌握自己的命运」图标） */
    public static final RegistryObject<Item> GUARDIAN_EMBLEM =
            ITEMS.register("guardian_emblem", () -> new Item(new Item.Properties()));

    // ===== 原神圣遗物 =====
    public static final RegistryObject<ArtifactItem> ARTIFACT_FLOWER =
            ITEMS.register("artifact_flower", () -> new ArtifactItem(ArtifactSlot.FLOWER));
    public static final RegistryObject<ArtifactItem> ARTIFACT_PLUME =
            ITEMS.register("artifact_plume", () -> new ArtifactItem(ArtifactSlot.PLUME));
    public static final RegistryObject<ArtifactItem> ARTIFACT_SANDS =
            ITEMS.register("artifact_sands", () -> new ArtifactItem(ArtifactSlot.SANDS));
    public static final RegistryObject<ArtifactItem> ARTIFACT_GOBLET =
            ITEMS.register("artifact_goblet", () -> new ArtifactItem(ArtifactSlot.GOBLET));
    public static final RegistryObject<ArtifactItem> ARTIFACT_CIRCLET =
            ITEMS.register("artifact_circlet", () -> new ArtifactItem(ArtifactSlot.CIRCLET));

    public static void registerItems(IEventBus bus) {
        ITEMS.register(bus);
    }
}
