package org.tp.tcdex.integration.tinkers.modifier.hook;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.Collection;

/**
 * 玩家护盾破碎 Hook：玩家穿戴的匠魂装备/手持工具上的词条可在<b>护盾被打穿</b>时
 * 减免溢出伤害并触发破碎联动（命运2 语义：护盾破碎是关键时刻）。
 *
 * <p>由 {@link org.tp.tcdex.shield.PlayerShieldEvents} 在护盾从有到无（吸收后归零）
 * 的当次受击中派发：先链式调整溢出伤害（所有词条），再链式触发破碎回调（所有词条）。</p>
 */
public interface PlayerShieldBreakHook {

    /**
     * 调整护盾破碎后结算到生命的溢出伤害。
     *
     * @param source   本击伤害来源
     * @param overflow 当前溢出伤害（护盾全扣后剩余部分；0 = 完全格挡溢出）
     * @return 调整后的溢出伤害（&lt;=0 则本击被完全格挡）
     */
    default float modifyBreakOverflow(IToolStackView tool, ModifierEntry modifier,
                                      Player player, DamageSource source, float overflow) {
        return overflow;
    }

    /**
     * 护盾破碎回调：本击护盾被打穿后触发（溢出伤害已按所有词条调整完毕）。
     * 词条可借此触发 AOE / 增益 / 回复等联动。
     *
     * @param source   本击伤害来源
     * @param overflow 调整后的溢出伤害（&gt;0 表示仍有伤害结算到生命）
     */
    default void onShieldBreak(IToolStackView tool, ModifierEntry modifier,
                               Player player, DamageSource source, float overflow) {
    }

    /** 合并器：逐个调用所有实现（AllMerger） */
    record AllMerger(Collection<PlayerShieldBreakHook> modules) implements PlayerShieldBreakHook {
        @Override
        public float modifyBreakOverflow(IToolStackView tool, ModifierEntry modifier,
                                         Player player, DamageSource source, float overflow) {
            for (PlayerShieldBreakHook module : modules) {
                overflow = module.modifyBreakOverflow(tool, modifier, player, source, overflow);
            }
            return overflow;
        }

        @Override
        public void onShieldBreak(IToolStackView tool, ModifierEntry modifier,
                                  Player player, DamageSource source, float overflow) {
            for (PlayerShieldBreakHook module : modules) {
                module.onShieldBreak(tool, modifier, player, source, overflow);
            }
        }
    }
}
