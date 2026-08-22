package org.tp.tcdex.chain;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;
import org.tp.tcdex.Tcdex;
import org.tp.tcdex.network.ChainActionPacket;
import org.tp.tcdex.network.PacketHandler;

/**
 * 连携按键：智能键 + 连携引爆键 + 终结技键。
 */
@Mod.EventBusSubscriber(modid = Tcdex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ChainKeybind {

    private static final String CATEGORY = "key.categories.tcdex";
    private static final String KEY_SMART = "key.tcdex.chain_smart";
    private static final String KEY_DETONATE = "key.tcdex.chain_detonate";
    private static final String KEY_FINISHER = "key.tcdex.chain_finisher";

    private static KeyMapping smartKey;
    private static KeyMapping detonateKey;
    private static KeyMapping finisherKey;

    private ChainKeybind() {
    }

    public static void register() {
        smartKey = new KeyMapping(KEY_SMART, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY);
        detonateKey = new KeyMapping(KEY_DETONATE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);
        finisherKey = new KeyMapping(KEY_FINISHER, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F, CATEGORY);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ChainKeybind::registerKeyMappings);
        MinecraftForge.EVENT_BUS.register(ChainKeybind.class);
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(smartKey);
        event.register(detonateKey);
        event.register(finisherKey);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (smartKey != null && smartKey.consumeClick()) {
            PacketHandler.CHANNEL.sendToServer(new ChainActionPacket(ChainActionType.SMART));
        }
        if (detonateKey != null && detonateKey.consumeClick()) {
            PacketHandler.CHANNEL.sendToServer(new ChainActionPacket(ChainActionType.DETONATE));
        }
        if (finisherKey != null && finisherKey.consumeClick()) {
            PacketHandler.CHANNEL.sendToServer(new ChainActionPacket(ChainActionType.FINISHER));
        }
    }
}
