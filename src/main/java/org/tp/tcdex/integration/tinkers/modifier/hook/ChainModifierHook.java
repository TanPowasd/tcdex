package org.tp.tcdex.integration.tinkers.modifier.hook;

import net.minecraft.world.entity.LivingEntity;
import org.tp.tcdex.chain.ElementActionType;
import org.tp.tcdex.element.ElementType;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

/**
 * 原命连携 Hook：匠魂工具/护甲上的词条可以调整连携积累、引爆、终结技效果。
 */
public interface ChainModifierHook {

    /** 调整元素行为贡献给连携链的权重 */
    default float modifyChainContribution(IToolStackView tool, ModifierEntry modifier,
                                          ElementType element, ElementActionType actionType, float contribution) {
        return contribution;
    }

    /** 调整连携引爆伤害 */
    default float modifyDetonateDamage(IToolStackView tool, ModifierEntry modifier, float damage) {
        return damage;
    }

    /** 调整连携引爆半径 */
    default float modifyDetonateRadius(IToolStackView tool, ModifierEntry modifier, float radius) {
        return radius;
    }

    /** 调整连携引爆冷却 */
    default int modifyDetonateCooldown(IToolStackView tool, ModifierEntry modifier, int cooldown) {
        return cooldown;
    }

    /** 调整连携增益持续 */
    default int modifyDetonateBuffDuration(IToolStackView tool, ModifierEntry modifier, int duration) {
        return duration;
    }

    /** 调整终结技伤害 */
    default float modifyFinisherDamage(IToolStackView tool, ModifierEntry modifier, float damage) {
        return damage;
    }

    /** 调整终结技范围 */
    default float modifyFinisherRadius(IToolStackView tool, ModifierEntry modifier, float radius) {
        return radius;
    }

    /** 连携引爆后回调 */
    default void onChainDetonate(IToolStackView tool, ModifierEntry modifier,
                                 LivingEntity player, List<ElementType> elements,
                                 @Nullable LivingEntity center) {
    }

    /** 命定终结技后回调 */
    default void onChainFinisher(IToolStackView tool, ModifierEntry modifier,
                                 LivingEntity player, LivingEntity target, List<ElementType> elements) {
    }

    /** 合并器：逐个调用所有实现（AllMerger） */
    record AllMerger(Collection<ChainModifierHook> modules) implements ChainModifierHook {
        @Override
        public float modifyChainContribution(IToolStackView tool, ModifierEntry modifier,
                                             ElementType element, ElementActionType actionType, float contribution) {
            for (ChainModifierHook module : modules) {
                contribution = module.modifyChainContribution(tool, modifier, element, actionType, contribution);
            }
            return contribution;
        }

        @Override
        public float modifyDetonateDamage(IToolStackView tool, ModifierEntry modifier, float damage) {
            for (ChainModifierHook module : modules) {
                damage = module.modifyDetonateDamage(tool, modifier, damage);
            }
            return damage;
        }

        @Override
        public float modifyDetonateRadius(IToolStackView tool, ModifierEntry modifier, float radius) {
            for (ChainModifierHook module : modules) {
                radius = module.modifyDetonateRadius(tool, modifier, radius);
            }
            return radius;
        }

        @Override
        public int modifyDetonateCooldown(IToolStackView tool, ModifierEntry modifier, int cooldown) {
            for (ChainModifierHook module : modules) {
                cooldown = module.modifyDetonateCooldown(tool, modifier, cooldown);
            }
            return cooldown;
        }

        @Override
        public int modifyDetonateBuffDuration(IToolStackView tool, ModifierEntry modifier, int duration) {
            for (ChainModifierHook module : modules) {
                duration = module.modifyDetonateBuffDuration(tool, modifier, duration);
            }
            return duration;
        }

        @Override
        public float modifyFinisherDamage(IToolStackView tool, ModifierEntry modifier, float damage) {
            for (ChainModifierHook module : modules) {
                damage = module.modifyFinisherDamage(tool, modifier, damage);
            }
            return damage;
        }

        @Override
        public float modifyFinisherRadius(IToolStackView tool, ModifierEntry modifier, float radius) {
            for (ChainModifierHook module : modules) {
                radius = module.modifyFinisherRadius(tool, modifier, radius);
            }
            return radius;
        }

        @Override
        public void onChainDetonate(IToolStackView tool, ModifierEntry modifier,
                                    LivingEntity player, List<ElementType> elements,
                                    @Nullable LivingEntity center) {
            for (ChainModifierHook module : modules) {
                module.onChainDetonate(tool, modifier, player, elements, center);
            }
        }

        @Override
        public void onChainFinisher(IToolStackView tool, ModifierEntry modifier,
                                    LivingEntity player, LivingEntity target, List<ElementType> elements) {
            for (ChainModifierHook module : modules) {
                module.onChainFinisher(tool, modifier, player, target, elements);
            }
        }
    }
}
