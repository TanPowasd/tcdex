package org.tp.tcdex;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Tcdex.MODID);

    public static final RegistryObject<CreativeModeTab> TCDEX_TAB =
            CREATIVE_MODE_TABS.register("tcdex_tab", () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.MY_LOVE.get()))
                    .title(Component.translatable("itemGroup.tcdex_tab"))
                    .displayItems((pParameters, pOutput) -> {
                        pOutput.accept(ModItems.LIGHT_ESSENCE.get());
                        pOutput.accept(ModItems.GUARDIAN_EMBLEM.get());
                        pOutput.accept(ModItems.ARTIFACT_FLOWER.get());
                        pOutput.accept(ModItems.ARTIFACT_PLUME.get());
                        pOutput.accept(ModItems.ARTIFACT_SANDS.get());
                        pOutput.accept(ModItems.ARTIFACT_GOBLET.get());
                        pOutput.accept(ModItems.ARTIFACT_CIRCLET.get());
                    }).build());

    public static void registerCreativeModeTabs(IEventBus bus) {
        CREATIVE_MODE_TABS.register(bus);
    }
}
