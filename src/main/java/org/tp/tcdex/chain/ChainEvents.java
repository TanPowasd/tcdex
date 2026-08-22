package org.tp.tcdex.chain;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tp.tcdex.Tcdex;

/**
 * 连携系统 Forge 事件入口。
 */
@Mod.EventBusSubscriber(modid = Tcdex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ChainEvents {

    private ChainEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        ChainManager.tick(event.player);
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        ChainAreaManager.tick(event.level);
    }
}
