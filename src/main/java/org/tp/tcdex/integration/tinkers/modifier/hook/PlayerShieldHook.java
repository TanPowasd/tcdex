package org.tp.tcdex.integration.tinkers.modifier.hook;

import net.minecraft.world.entity.player.Player;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.Collection;

/**
 * 玩家护盾 Hook：玩家穿戴的匠魂装备/手持工具上的词条可调整护盾吸收量与脱战回复速率
 * （由 {@link org.tp.tcdex.shield.PlayerShieldManager} 在吸收/回复时派发）。
 */
public interface PlayerShieldHook {

    /**
     * 调整玩家护盾本次吸收量。
     *
     * @param damageAmount 本击原始伤害
     * @param absorbed     当前计算的吸收量（受击优先扣盾，初始 = min(剩余护盾, 伤害)）
     * @return 调整后的吸收量（可放大/缩小；溢出部分仍结算到生命）
     */
    default float modifyAbsorbed(IToolStackView tool, ModifierEntry modifier,
                                 Player player, float damageAmount, float absorbed) {
        return absorbed;
    }

    /**
     * 调整玩家护盾脱战回复速率（每 tick）。
     *
     * @param rate 当前回复速率（基础值 × 元素状态干扰系数）
     * @return 调整后的回复速率
     */
    default float modifyRegenRate(IToolStackView tool, ModifierEntry modifier,
                                  Player player, float rate) {
        return rate;
    }

    /** 合并器：逐个调用所有实现（AllMerger） */
    record AllMerger(Collection<PlayerShieldHook> modules) implements PlayerShieldHook {
        @Override
        public float modifyAbsorbed(IToolStackView tool, ModifierEntry modifier,
                                    Player player, float damageAmount, float absorbed) {
            for (PlayerShieldHook module : modules) {
                absorbed = module.modifyAbsorbed(tool, modifier, player, damageAmount, absorbed);
            }
            return absorbed;
        }

        @Override
        public float modifyRegenRate(IToolStackView tool, ModifierEntry modifier,
                                     Player player, float rate) {
            for (PlayerShieldHook module : modules) {
                rate = module.modifyRegenRate(tool, modifier, player, rate);
            }
            return rate;
        }
    }
}
