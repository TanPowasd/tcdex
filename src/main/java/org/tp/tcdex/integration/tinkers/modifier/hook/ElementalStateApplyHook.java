package org.tp.tcdex.integration.tinkers.modifier.hook;

import org.tp.tcdex.element.ElementType;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.Collection;

/**
 * 元素状态施加 Hook：武器命中施加元素状态时，工具上的词条可调整层数与时长
 * （由元素词条/棱镜共鸣在近战命中路径派发，远程路径不派发——无工具上下文）。
 */
public interface ElementalStateApplyHook {

    /**
     * 调整本次施加的元素状态层数。
     *
     * @param element 施加的元素
     * @param stacks  当前层数（元素基础每击层数）
     * @return 调整后的层数（&lt;=0 则不施加该状态）
     */
    default float modifyStateStacks(IToolStackView tool, ModifierEntry modifier,
                                    ElementType element, float stacks) {
        return stacks;
    }

    /**
     * 调整本次施加的元素状态时长（tick）。
     *
     * @param element  施加的元素
     * @param duration 当前时长（元素基础状态时长）
     * @return 调整后的时长（&lt;=0 则不施加该状态）
     */
    default int modifyStateDuration(IToolStackView tool, ModifierEntry modifier,
                                    ElementType element, int duration) {
        return duration;
    }

    /** 合并器：逐个调用所有实现（AllMerger） */
    record AllMerger(Collection<ElementalStateApplyHook> modules) implements ElementalStateApplyHook {
        @Override
        public float modifyStateStacks(IToolStackView tool, ModifierEntry modifier,
                                       ElementType element, float stacks) {
            for (ElementalStateApplyHook module : modules) {
                stacks = module.modifyStateStacks(tool, modifier, element, stacks);
            }
            return stacks;
        }

        @Override
        public int modifyStateDuration(IToolStackView tool, ModifierEntry modifier,
                                       ElementType element, int duration) {
            for (ElementalStateApplyHook module : modules) {
                duration = module.modifyStateDuration(tool, modifier, element, duration);
            }
            return duration;
        }
    }
}
