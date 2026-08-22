package org.tp.tcdex.element.processor;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.tp.tcdex.element.ElementCategory;
import org.tp.tcdex.element.ElementEffectProcessor;
import org.tp.tcdex.element.ElementRegistry;
import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.modifier.elemental.IElementalEntity;

import javax.annotation.Nullable;

/**
 * 月元素处理器。
 *
 * <p>当前实现月蚀净化：带月标记的目标受到光能元素攻击时，
 * 月标记被“净化”并造成额外暗影伤害，同时清除月状态。</p>
 */
public class MoonProcessor implements ElementEffectProcessor {

    private static final float PURIFY_DAMAGE = 4.0f;

    @Override
    public void onHurt(LivingEntity target, ElementType element, @Nullable ElementType attackElement,
                       float amount, @Nullable LivingEntity attacker) {
        if (target.level().isClientSide || element != ElementType.MOON) {
            return;
        }
        if (attackElement == null || ElementRegistry.isCategory(attackElement, ElementCategory.LIGHT)) {
            IElementalEntity data = IElementalEntity.of(target);
            if (data.getElementStacks(ElementType.MOON) > 0) {
                float damage = PURIFY_DAMAGE * (data.getElementStacks(ElementType.MOON) / 100.0f);
                if (attacker != null) {
                    target.hurt(attacker.damageSources().magic(), damage);
                } else {
                    target.hurt(target.damageSources().magic(), damage);
                }
                data.clearElementState(ElementType.MOON);
            }
        }
    }
}
