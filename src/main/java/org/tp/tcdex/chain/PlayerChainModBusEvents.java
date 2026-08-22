package org.tp.tcdex.chain;

import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tp.tcdex.Tcdex;

/**
 * 玩家连携 Capability 的 MOD 总线注册。
 */
@Mod.EventBusSubscriber(modid = Tcdex.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class PlayerChainModBusEvents {

    private PlayerChainModBusEvents() {
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(IPlayerChainData.class);
    }
}
