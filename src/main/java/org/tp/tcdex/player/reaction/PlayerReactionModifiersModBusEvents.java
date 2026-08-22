package org.tp.tcdex.player.reaction;

import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tp.tcdex.Tcdex;

/**
 * 玩家反应词条 Capability 的 MOD 总线注册。
 */
@Mod.EventBusSubscriber(modid = Tcdex.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class PlayerReactionModifiersModBusEvents {

    private PlayerReactionModifiersModBusEvents() {
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(IPlayerReactionModifiers.class);
    }
}
