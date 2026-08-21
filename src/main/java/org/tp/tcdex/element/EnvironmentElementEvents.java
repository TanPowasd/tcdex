package org.tp.tcdex.element;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tp.tcdex.Tcdex;
import org.tp.tcdex.modifier.elemental.IElementalEntity;

/**
 * 环境元素附着：
 * - 水中/雨天 → 潮汐 TIDE
 * - 着火/熔岩 → 烈日 SOLAR
 * 环境附着会参与元素反应。
 */
@Mod.EventBusSubscriber(modid = Tcdex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EnvironmentElementEvents {

    private static final int INTERVAL = 40;

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) {
            return;
        }
        if (entity.tickCount % INTERVAL != 0) {
            return;
        }
        if (entity.isInWaterRainOrBubble()) {
            IElementalEntity.of(entity).addElementState(ElementType.TIDE, 1, 100);
        }
        if (entity.isOnFire()) {
            IElementalEntity.of(entity).addElementState(ElementType.SOLAR, 25, 100);
        }
    }
}
