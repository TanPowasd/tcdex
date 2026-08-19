package org.tp.tcdex.transcendence;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;
import org.tp.tcdex.Tcdex;
import org.tp.tcdex.network.PacketHandler;
import org.tp.tcdex.network.TranscendenceActivatePacket;

/**
 * 超越激活按键（客户端，默认 V）：按下后发送 C2S 包请求服务端激活 Transcendence。
 * 命运2 语义：双槽充满后手动按键爆发。
 */
@Mod.EventBusSubscriber(modid = Tcdex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class TranscendenceKeybind {

    private static final String CATEGORY = "key.categories.tcdex";
    private static final String KEY_NAME = "key.tcdex.transcendence";

    private static KeyMapping keyMapping;

    /** 在 mod 构造时调用（客户端）：注册按键 */
    public static void register() {
        keyMapping = new KeyMapping(KEY_NAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(TranscendenceKeybind::registerKeyMappings);
        MinecraftForge.EVENT_BUS.register(TranscendenceKeybind.class);
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(keyMapping);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (keyMapping != null && keyMapping.consumeClick()) {
            PacketHandler.CHANNEL.sendToServer(new TranscendenceActivatePacket());
        }
    }

    private TranscendenceKeybind() {
    }
}
