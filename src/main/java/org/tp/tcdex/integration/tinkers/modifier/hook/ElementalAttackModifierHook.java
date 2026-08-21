package org.tp.tcdex.integration.tinkers.modifier.hook;

import org.tp.tcdex.element.ElementType;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.Collection;

/**
 * 元素攻击 Hook：武器上的词条可借此调整元素伤害与护盾破盾效率。
 *
 * <p>由 {@link org.tp.tcdex.event.ElementalDamageEvents} 在伤害转化/护盾结算时
 * 遍历工具 modifier 派发（链式，AllMerger）。</p>
 */
public interface ElementalAttackModifierHook {

    /**
     * 调整最终元素伤害（在抗性系数应用之后）。
     *
     * @param element 攻击元素（动能攻击不会调用本方法）
     * @param amount  当前元素伤害
     * @return 调整后的伤害
     */
    default float modifyElementalDamage(IToolStackView tool, ModifierEntry modifier, ElementType element, float amount) {
        return amount;
    }

    /**
     * 调整护盾破盾效率。
     *
     * @param shieldElement 目标护盾元素
     * @param efficiency    当前效率（匹配 ×2 / 不匹配 ×0.5）
     * @return 调整后的效率
     */
    default float modifyShieldEfficiency(IToolStackView tool, ModifierEntry modifier, ElementType shieldElement, float efficiency) {
        return efficiency;
    }

    /** 合并器：逐个调用所有实现（AllMerger） */
    record AllMerger(Collection<ElementalAttackModifierHook> modules) implements ElementalAttackModifierHook {
        @Override
        public float modifyElementalDamage(IToolStackView tool, ModifierEntry modifier, ElementType element, float amount) {
            for (ElementalAttackModifierHook module : modules) {
                amount = module.modifyElementalDamage(tool, modifier, element, amount);
            }
            return amount;
        }

        @Override
        public float modifyShieldEfficiency(IToolStackView tool, ModifierEntry modifier, ElementType shieldElement, float efficiency) {
            for (ElementalAttackModifierHook module : modules) {
                efficiency = module.modifyShieldEfficiency(tool, modifier, shieldElement, efficiency);
            }
            return efficiency;
        }
    }
}
