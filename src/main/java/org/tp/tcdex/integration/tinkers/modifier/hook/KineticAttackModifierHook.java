package org.tp.tcdex.integration.tinkers.modifier.hook;

import net.minecraft.world.entity.LivingEntity;
import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.integration.tinkers.modifier.elemental.ElementalModifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.Collection;

/**
 * 动能攻击 Hook：动能武器（无元素词条）上的词条可借此调整动能伤害与动能破盾效率。
 *
 * <p>由 {@link org.tp.tcdex.event.ElementalDamageEvents} 在动能伤害转化/护盾结算时
 * 遍历工具 modifier 派发（链式，AllMerger）。元素武器走 {@link ElementalAttackModifierHook}，
 * 动能武器走本 hook——两套体系互不干扰。</p>
 */
public interface KineticAttackModifierHook {

    /**
     * 调整最终动能伤害（动能分支：目标无元素抗性参与，数值为工具面板原值）。
     *
     * @param target 受击目标（动能词条常需要看目标：是否带盾/带标记）
     * @param amount 当前动能伤害
     * @return 调整后的伤害
     */
    default float modifyKineticDamage(IToolStackView tool, ModifierEntry modifier, LivingEntity target, float amount) {
        return amount;
    }

    /**
     * 调整动能破盾效率（默认不匹配 ×0.5，棱镜盾 ×0.1——动能破盾慢是核心痛点，
     * 动能词条可借此补偿，如"动能破盾效率提升"）。
     *
     * @param shieldElement 目标护盾元素
     * @param efficiency    当前效率（普通盾 ×0.5 / 棱镜盾 ×0.1）
     * @return 调整后的效率
     */
    default float modifyKineticShieldEfficiency(IToolStackView tool, ModifierEntry modifier, ElementType shieldElement, float efficiency) {
        return efficiency;
    }

    /**
     * 动能武器判定：工具未固化元素（无元素充能随机元素，且未与棱镜共鸣同时存在——
     * 动能词条与两者互斥，见 {@link org.tp.tcdex.integration.tinkers.modifier.ModifierExclusivity}，此处只查持久数据兜底）。
     */
    static boolean isKineticTool(IToolStackView tool) {
        return tool != null && tool.getPersistentData().getString(ElementalModifier.ELEMENT_KEY).isEmpty();
    }

    /** 合并器：逐个调用所有实现（AllMerger） */
    record AllMerger(Collection<KineticAttackModifierHook> modules) implements KineticAttackModifierHook {
        @Override
        public float modifyKineticDamage(IToolStackView tool, ModifierEntry modifier, LivingEntity target, float amount) {
            for (KineticAttackModifierHook module : modules) {
                amount = module.modifyKineticDamage(tool, modifier, target, amount);
            }
            return amount;
        }

        @Override
        public float modifyKineticShieldEfficiency(IToolStackView tool, ModifierEntry modifier, ElementType shieldElement, float efficiency) {
            for (KineticAttackModifierHook module : modules) {
                efficiency = module.modifyKineticShieldEfficiency(tool, modifier, shieldElement, efficiency);
            }
            return efficiency;
        }
    }
}
