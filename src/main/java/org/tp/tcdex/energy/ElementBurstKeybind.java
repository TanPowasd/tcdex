package org.tp.tcdex.energy;

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
import org.tp.tcdex.network.ElementBurstActivatePacket;
import org.tp.tcdex.network.PacketHandler;

/**
 * 元素爆发激活按键（客户端，默认 X）。
 */
@Mod.EventBusSubscriber(modid = Tcdex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ElementBurstKeybind {

    private static final String CATEGORY = "key.categories.tcdex";
    private static final String KEY_NAME = "key.tcdex.element_burst";

    private static KeyMapping keyMapping;

    private ElementBurstKeybind() {
    }

    /** 在 mod 构造时调用（客户端）：注册按键 */
    public static void register() {
        keyMapping = new KeyMapping(KEY_NAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, CATEGORY);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ElementBurstKeybind::registerKeyMappings);
        MinecraftForge.EVENT_BUS.register(ElementBurstKeybind.class);
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(keyMapping);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (keyMapping != null && keyMapping.consumeClick()) {
            PacketHandler.CHANNEL.sendToServer(new ElementBurstActivatePacket());
        }
    }
}
