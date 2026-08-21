package org.tp.tcdex.modifier.hook;

import net.minecraft.world.entity.LivingEntity;
import org.tp.tcdex.reaction.ElementReaction;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * 元素反应 Hook：武器/装备上的词条可调整元素反应的各项参数，并在反应触发后获得回调。
 *
 * <p>由 {@link org.tp.tcdex.reaction.ElementReactionEvents} 在反应触发时遍历攻击者
 * 手持匠魂工具链式派发（AllMerger）。</p>
 */
public interface ReactionModifierHook {

    /** 调整反应附着量消耗 */
    default float modifyReactionAuraCost(IToolStackView tool, ModifierEntry modifier, ElementReaction reaction, float auraCost) {
        return auraCost;
    }

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

    /** 调整伤害类反应的伤害值 */
    default float modifyReactionDamage(IToolStackView tool, ModifierEntry modifier, ElementReaction reaction, float damage) {
        return damage;
    }

    /** 调整反应冷却（tick，越小越频繁） */
    default int modifyReactionCooldown(IToolStackView tool, ModifierEntry modifier, ElementReaction reaction, int cooldown) {
        return cooldown;
    }

    /** 反应触发后的回调（在所有数值调整完成后调用） */
    default void onReactionTriggered(IToolStackView tool, ModifierEntry modifier, LivingEntity target,
                                     ElementReaction reaction, @Nullable LivingEntity source, float finalIntensity) {
    }

    /** 合并器：逐个调用所有实现（AllMerger） */
    record AllMerger(Collection<ReactionModifierHook> modules) implements ReactionModifierHook {
        @Override
        public float modifyReactionAuraCost(IToolStackView tool, ModifierEntry modifier, ElementReaction reaction, float auraCost) {
            for (ReactionModifierHook module : modules) {
                auraCost = module.modifyReactionAuraCost(tool, modifier, reaction, auraCost);
            }
            return auraCost;
        }

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
        public float modifyReactionDamage(IToolStackView tool, ModifierEntry modifier, ElementReaction reaction, float damage) {
            for (ReactionModifierHook module : modules) {
                damage = module.modifyReactionDamage(tool, modifier, reaction, damage);
            }
            return damage;
        }

        @Override
        public int modifyReactionCooldown(IToolStackView tool, ModifierEntry modifier, ElementReaction reaction, int cooldown) {
            for (ReactionModifierHook module : modules) {
                cooldown = module.modifyReactionCooldown(tool, modifier, reaction, cooldown);
            }
            return cooldown;
        }

        @Override
        public void onReactionTriggered(IToolStackView tool, ModifierEntry modifier, LivingEntity target,
                                        ElementReaction reaction, @Nullable LivingEntity source, float finalIntensity) {
            for (ReactionModifierHook module : modules) {
                module.onReactionTriggered(tool, modifier, target, reaction, source, finalIntensity);
            }
        }
    }
}
