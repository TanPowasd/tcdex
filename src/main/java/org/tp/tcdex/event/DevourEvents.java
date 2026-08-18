package org.tp.tcdex.event;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tp.tcdex.Tcdex;
import org.tp.tcdex.effect.DevourEffect;
import org.tp.tcdex.effect.TcdexEffects;

/**
 * 吞噬效果联动（命运2 Devour，独立 buff 非词条）：
 *
 * <p>玩家持有「吞噬」效果时，<b>无论用什么方式击杀</b>（近战/远程、匠魂/原版武器）：
 * <ul>
 *   <li>回复满生命值</li>
 *   <li>刷新吞噬时长（10 秒，保留原 amplifier）</li>
 * </ul></p>
 */
@Mod.EventBusSubscriber(modid = Tcdex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DevourEvents {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onKill(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player killer)) {
            return;
        }
        MobEffectInstance devour = killer.getEffect(TcdexEffects.DEVOUR.get());
        if (devour == null) {
            return;
        }
        // 击杀：回满生命 + 刷新吞噬时长（保留原 amplifier）
        killer.setHealth(killer.getMaxHealth());
        killer.addEffect(new MobEffectInstance(TcdexEffects.DEVOUR.get(),
                DevourEffect.DURATION, devour.getAmplifier(), false, true));
    }
}
