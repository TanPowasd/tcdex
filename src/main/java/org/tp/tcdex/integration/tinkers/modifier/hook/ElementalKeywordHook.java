package org.tp.tcdex.integration.tinkers.modifier.hook;

import org.tp.tcdex.element.ElementType;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.Collection;

/**
 * 元素关键词结算 Hook：武器上的词条可借此调整元素关键词（命运2 关键词）的结算数值。
 *
 * <p>关键词结算位于 {@link org.tp.tcdex.event.ElementalStateEvents}（受击联动），
 * 本 hook 在每处关键词结算时遍历攻击者/目标的匠魂工具链式派发（AllMerger）。</p>
 *
 * <p>三类数值（keyword = 触发关键词的元素）：
 * <ul>
 *   <li><b>倍率类</b>（{@link #modifyKeywordMultiplier}）：缚丝 Sever 减伤倍率（0.6）、
 *       冰影 Shatter 增伤倍率（1.5）、虚空 Weaken 增伤倍率（1.15）</li>
 *   <li><b>伤害类</b>（{@link #modifyKeywordDamage}）：虚空 Volatile 爆炸（%最大生命，0.1）、
 *       电弧 Jolt 连锁伤害（3.0）、棱镜 Refract 溅射比例（0.25）</li>
 *   <li><b>范围类</b>（{@link #modifyKeywordRadius}）：Volatile 爆炸半径（1.5）、
 *       Jolt 连锁半径（2.0）、Refract 溅射半径（2.0）</li>
 * </ul>
 * 所有方法均为 default（返回原值），词条只覆写需要的。</p>
 */
public interface ElementalKeywordHook {

    /**
     * 调整关键词结算的倍率类数值。
     *
     * @param keyword    关键词元素：STRAND=Sever、STASIS=Shatter、VOID=Weaken
     * @param multiplier 当前倍率（Sever 0.6 减伤 / Shatter 1.5 增伤 / Weaken 1.15 增伤）
     * @return 调整后的倍率
     */
    default float modifyKeywordMultiplier(IToolStackView tool, ModifierEntry modifier, ElementType keyword, float multiplier) {
        return multiplier;
    }

    /**
     * 调整关键词结算的伤害类数值。
     *
     * @param keyword 关键词元素：VOID=Volatile、ARC=Jolt、PRISM=Refract
     * @param damage  当前伤害值（Volatile = %最大生命；Jolt = 固定伤害；Refract = 溅射比例）
     * @return 调整后的伤害值
     */
    default float modifyKeywordDamage(IToolStackView tool, ModifierEntry modifier, ElementType keyword, float damage) {
        return damage;
    }

    /**
     * 调整关键词结算的范围（半径，格）。
     *
     * @param keyword 关键词元素：VOID=Volatile、ARC=Jolt、PRISM=Refract
     * @param radius  当前半径
     * @return 调整后的半径
     */
    default float modifyKeywordRadius(IToolStackView tool, ModifierEntry modifier, ElementType keyword, float radius) {
        return radius;
    }

    /** 合并器：逐个调用所有实现（AllMerger） */
    record AllMerger(Collection<ElementalKeywordHook> modules) implements ElementalKeywordHook {
        @Override
        public float modifyKeywordMultiplier(IToolStackView tool, ModifierEntry modifier, ElementType keyword, float multiplier) {
            for (ElementalKeywordHook module : modules) {
                multiplier = module.modifyKeywordMultiplier(tool, modifier, keyword, multiplier);
            }
            return multiplier;
        }

        @Override
        public float modifyKeywordDamage(IToolStackView tool, ModifierEntry modifier, ElementType keyword, float damage) {
            for (ElementalKeywordHook module : modules) {
                damage = module.modifyKeywordDamage(tool, modifier, keyword, damage);
            }
            return damage;
        }

        @Override
        public float modifyKeywordRadius(IToolStackView tool, ModifierEntry modifier, ElementType keyword, float radius) {
            for (ElementalKeywordHook module : modules) {
                radius = module.modifyKeywordRadius(tool, modifier, keyword, radius);
            }
            return radius;
        }
    }
}
