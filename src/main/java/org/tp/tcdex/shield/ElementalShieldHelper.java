package org.tp.tcdex.shield;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import org.tp.tcdex.element.ElementManager;
import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.event.ElementalDamageEvents;
import org.tp.tcdex.modifier.elemental.IElementalEntity;

import javax.annotation.Nullable;

/**
 * 外部伤害源（法术、龙息、其他 mod 攻击）参与元素护盾结算的辅助工具。
 *
 * <p>与玩家近战伤害转化共用同一套破盾效率/棱镜盾规则：
 * 返回值为“扣除护盾后实际应打到血量的伤害”；返回 0 表示伤害被护盾完全吸收。</p>
 */
public final class ElementalShieldHelper {

    private ElementalShieldHelper() {
    }

    /**
     * 让一次外部元素/动能伤害参与目标元素护盾结算。
     *
     * @param target        受击目标
     * @param attackElement 本次伤害元素；null 表示动能/无元素
     * @param amount        原始伤害值
     * @return 扣除护盾后仍应造成生命伤害的数值；0 = 被护盾完全吸收
     */
    public static float damageShield(LivingEntity target, @Nullable ElementType attackElement, float amount) {
        if (target.level().isClientSide || amount <= 0) {
            return amount;
        }
        IElementalEntity data = IElementalEntity.of(target);
        if (data.getShieldElement() == null || data.getShieldAmount() <= 0) {
            return amount;
        }

        ElementType shieldElement = data.getShieldElement();
        float efficiency;
        boolean permanent = false;

        if (shieldElement == ElementType.PRISM) {
            if (attackElement == ElementType.PRISM) {
                efficiency = PrismShieldConfig.getMatchEfficiency();
                permanent = true;
            } else if (attackElement == null) {
                efficiency = PrismShieldConfig.getKineticEfficiency();
            } else {
                efficiency = PrismShieldConfig.getElementEfficiency();
            }
            data.markShieldHit(target.level().getGameTime());
        } else {
            efficiency = attackElement == ElementType.PRISM
                    ? 2.0f
                    : ElementManager.getShieldEfficiency(shieldElement, attackElement);
        }

        float overflow;
        if (shieldElement == ElementType.PRISM) {
            overflow = data.consumeShield(amount * efficiency, permanent);
        } else {
            overflow = data.consumeShield(amount * efficiency);
        }
        if (overflow > 0) {
            float breakDamage = target.getMaxHealth() * 0.1f;
            ElementalDamageEvents.shieldBreak(target, shieldElement, breakDamage);

            // 元素使徒多层护盾：破碎后立即生成下一层
            if (data.getShieldLayers() > 0) {
                data.setShieldLayers(data.getShieldLayers() - 1);
                ElementType nextShield = ElementManager.rollShieldElement(target.getRandom());
                data.setShield(nextShield, target.getMaxHealth() * 0.5f);
            }

            return efficiency > 0 ? overflow / efficiency : amount;
        }

        // 未打穿：播放护盾格挡反馈
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 0.8F, 1.2F);
        return 0f;
    }
}
