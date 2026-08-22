package org.tp.tcdex.reaction;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tp.tcdex.Tcdex;
import org.tp.tcdex.damage.ModDamageSources;
import org.tp.tcdex.element.ElementType;

import javax.annotation.Nullable;

/**
 * 元素反应 Forge 事件入口。
 *
 * <p>实际触发/结算逻辑在 {@link ElementReactionEngine}，本类只负责把
 * TCDEX 元素伤害事件转发给引擎。</p>
 */
@Mod.EventBusSubscriber(modid = Tcdex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ElementReactionEvents {

    private ElementReactionEvents() {
    }

    public static boolean isEnabled() {
        return ElementReactionEngine.isEnabled();
    }

    public static void setEnabled(boolean value) {
        ElementReactionEngine.setEnabled(value);
    }

    /** 尝试触发一次元素反应（供外部联动直接调用） */
    public static void tryTriggerReaction(LivingEntity target, ElementType trigger, @Nullable LivingEntity source) {
        ElementReactionEngine.tryTriggerReaction(target, trigger, source);
    }

    /** 使用指定反应直接尝试触发（供 API 手动触发） */
    public static boolean triggerReaction(LivingEntity target, ElementReaction reaction, @Nullable LivingEntity source) {
        return ElementReactionEngine.triggerReaction(target, reaction, source);
    }

    /** TCDEX 元素伤害命中时自动尝试触发反应 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!ElementReactionEngine.isEnabled() || event.getEntity().level().isClientSide || event.isCanceled()) {
            return;
        }
        ElementType trigger = ModDamageSources.getElement(event.getSource());
        if (trigger == null) {
            return;
        }
        Entity sourceEntity = event.getSource().getEntity();
        LivingEntity source = sourceEntity instanceof LivingEntity living ? living : null;
        ElementReactionEngine.tryTriggerReaction(event.getEntity(), trigger, source);
    }
}
