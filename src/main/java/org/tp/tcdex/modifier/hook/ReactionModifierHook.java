package org.tp.tcdex.modifier.hook;

import org.tp.tcdex.reaction.ElementReaction;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.Collection;

/**
 * 元素反应 Hook：武器/装备上的词条可调整元素反应的持续时间、范围、强度与冷却。
 *
 * <p>由 {@link org.tp.tcdex.reaction.ElementReactionEvents} 在反应触发时遍历攻击者
 * 手持匠魂工具链式派发（AllMerger）。</p>
 */
public interface ReactionModifierHook {

    /** 调整反应控制/效果持续时间（tick） */
    default float modifyReactionDuration(IToolStackView tool, ModifierEntry modifier, ElementReaction reaction, float duration) {
        return duration;
    }

    /** 调整反应范围（格） */
    default float modifyReactionRadius(IToolStackView tool, ModifierEntry modifier, ElementReaction reaction, float radius) {
        return radius;
    }

    /** 调整反应强度（如聚怪力度） */
    default float modifyReactionIntensity(IToolStackView tool, ModifierEntry modifier, ElementReaction reaction, float intensity) {
        return intensity;
    }

    /** 调整反应冷却（tick，越小越频繁） */
    default int modifyReactionCooldown(IToolStackView tool, ModifierEntry modifier, ElementReaction reaction, int cooldown) {
        return cooldown;
    }

    /** 合并器：逐个调用所有实现（AllMerger） */
    record AllMerger(Collection<ReactionModifierHook> modules) implements ReactionModifierHook {
        @Override
        public float modifyReactionDuration(IToolStackView tool, ModifierEntry modifier, ElementReaction reaction, float duration) {
            for (ReactionModifierHook module : modules) {
                duration = module.modifyReactionDuration(tool, modifier, reaction, duration);
            }
            return duration;
        }

        @Override
        public float modifyReactionRadius(IToolStackView tool, ModifierEntry modifier, ElementReaction reaction, float radius) {
            for (ReactionModifierHook module : modules) {
                radius = module.modifyReactionRadius(tool, modifier, reaction, radius);
            }
            return radius;
        }

        @Override
        public float modifyReactionIntensity(IToolStackView tool, ModifierEntry modifier, ElementReaction reaction, float intensity) {
            for (ReactionModifierHook module : modules) {
                intensity = module.modifyReactionIntensity(tool, modifier, reaction, intensity);
            }
            return intensity;
        }

        @Override
        public int modifyReactionCooldown(IToolStackView tool, ModifierEntry modifier, ElementReaction reaction, int cooldown) {
            for (ReactionModifierHook module : modules) {
                cooldown = module.modifyReactionCooldown(tool, modifier, reaction, cooldown);
            }
            return cooldown;
        }
    }
}
