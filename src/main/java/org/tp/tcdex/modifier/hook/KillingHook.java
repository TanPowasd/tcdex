package org.tp.tcdex.modifier.hook;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.Collection;

/**
 * 击杀 Hook：工具击杀生物后触发（由 {@link org.tp.tcdex.event.TcdexHookEvents} 在 LivingDeathEvent 中手动派发）。
 *
 * <p>匠魂 3.10 原生没有击杀 hook，这里仿照 Tprt-re 的做法自定义一个，
 * 供需要"击杀结算"的词条使用（如 all_permitted 的禁忌翻倍）。</p>
 */
public interface KillingHook {

    /**
     * 工具击杀目标后调用。
     *
     * @param tool     击杀者的匠魂工具（主手或副手）
     * @param event    死亡事件（可读取伤害来源等）
     * @param attacker 攻击者
     * @param target   被击杀目标
     * @param level    词条等级
     */
    default void onKillLivingTarget(IToolStackView tool, LivingDeathEvent event, LivingEntity attacker, LivingEntity target, int level) {
    }

    /** 合并器：逐个调用所有实现（AllMerger） */
    record AllMerger(Collection<KillingHook> modules) implements KillingHook {
        @Override
        public void onKillLivingTarget(IToolStackView tool, LivingDeathEvent event, LivingEntity attacker, LivingEntity target, int level) {
            for (KillingHook module : modules) {
                module.onKillLivingTarget(tool, event, attacker, target, level);
            }
        }
    }
}
