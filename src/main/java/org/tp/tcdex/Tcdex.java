package org.tp.tcdex;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import org.tp.tcdex.compat.TcdexCompat;
import org.tp.tcdex.effect.TcdexEffects;
import org.tp.tcdex.modifier.ModifierExclusivity;
import org.tp.tcdex.modifier.elemental.ElementalModifier;
import org.tp.tcdex.modifier.elemental.FiveForcesModifier;
import org.tp.tcdex.modifier.elemental.PrismResonanceModifier;
import org.tp.tcdex.modifier.melee.ArcAmplifierModifier;
import org.tp.tcdex.modifier.melee.BurningFistsModifier;
import org.tp.tcdex.modifier.melee.BurstBarrierModifier;
import org.tp.tcdex.modifier.melee.CombatEchoModifier;
import org.tp.tcdex.modifier.melee.EagerEdgeModifier;
import org.tp.tcdex.modifier.melee.KineticSiphonModifier;
import org.tp.tcdex.modifier.melee.KineticTremorsModifier;
import org.tp.tcdex.modifier.melee.SynthoModifier;
import org.tp.tcdex.modifier.special.AllPermittedModifier;
import org.tp.tcdex.network.PacketHandler;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Tcdex.MODID)
public class Tcdex {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "tcdex";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "tcdex" namespace

    public Tcdex() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册网络通道（护盾同步等）
        PacketHandler.register();

        // 客户端：超越激活按键（玩家基础机制，非词条）
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) {
            org.tp.tcdex.transcendence.TranscendenceKeybind.register();
        }

        // 其他 mod 软联动（冰与火之舞 / 铁魔法，不作为前置依赖）
        TcdexCompat.init();

        // 注册词条互斥关系（元素充能 ↔ 棱镜共鸣）
        ModifierExclusivity.registerAll();

        // 注册自定义匠魂 Modifier
        modEventBus.addListener(EagerEdgeModifier::registerModifier);
        modEventBus.addListener(AllPermittedModifier::registerModifier);
        modEventBus.addListener(CombatEchoModifier::registerModifier);
        modEventBus.addListener(ElementalModifier::registerModifier);
        modEventBus.addListener(PrismResonanceModifier::registerModifier);
        modEventBus.addListener(FiveForcesModifier::registerModifier);
        modEventBus.addListener(SynthoModifier::registerModifier);
        modEventBus.addListener(BurningFistsModifier::registerModifier);
        modEventBus.addListener(ArcAmplifierModifier::registerModifier);
        modEventBus.addListener(BurstBarrierModifier::registerModifier);
        modEventBus.addListener(KineticTremorsModifier::registerModifier);
        modEventBus.addListener(KineticSiphonModifier::registerModifier);
        modEventBus.addListener(org.tp.tcdex.modifier.special.WarBannerModifier::registerModifier);

        // 注册自定义药水效果（吞噬等）
        TcdexEffects.register(modEventBus);

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        ModItems.registerItems(modEventBus);
        ModCreativeModeTabs.registerCreativeModeTabs(modEventBus);
        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        //modEventBus.addListener(this::addCreative);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");
        LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        if (Config.logDirtBlock) LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
   //     if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) event.accept(EXAMPLE_BLOCK_ITEM);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}
