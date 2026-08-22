package org.tp.tcdex.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.tp.tcdex.chain.ChainActionType;
import org.tp.tcdex.chain.ElementCombatEvents;
import org.tp.tcdex.chain.ChainEntry;
import org.tp.tcdex.chain.ChainManager;
import org.tp.tcdex.chain.ElementActionType;
import org.tp.tcdex.chain.IPlayerChainData;
import org.tp.tcdex.chain.PlayerChainCapability;
import org.tp.tcdex.element.ElementType;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 原命连携 / 命定终结技对外 API。
 *
 * <p>附属 Mod 可以通过本类读取玩家连携链、上报元素行为、
 * 手动触发连携引爆和命定终结技。</p>
 */
public final class TcdexChainAPI {

    private TcdexChainAPI() {
    }

    /** 上报一次元素行为（只更新主链） */
    public static void reportElementAction(Player player, ElementType element, ElementActionType actionType) {
        ElementCombatEvents.report(player, element, actionType);
    }

    /** 上报一次元素行为，并更新当前目标准焦点链 */
    public static void reportElementAction(Player player, ElementType element,
                                           ElementActionType actionType, @Nullable LivingEntity target) {
        ElementCombatEvents.report(player, element, actionType, target);
    }

    /** 获取玩家主链 */
    public static List<ChainEntry> getMainChain(Player player) {
        IPlayerChainData data = PlayerChainCapability.get(player).orElse(null);
        return data == null ? List.of() : data.getMainChain();
    }

    /** 获取玩家焦点链 */
    public static List<ChainEntry> getFocusChain(Player player) {
        IPlayerChainData data = PlayerChainCapability.get(player).orElse(null);
        return data == null ? List.of() : data.getFocusChain();
    }

    /** 获取主链不同元素数量 */
    public static int getDistinctElementCount(Player player) {
        IPlayerChainData data = PlayerChainCapability.get(player).orElse(null);
        return data == null ? 0 : data.getDistinctElementCount();
    }

    /** 获取群体连携溢出 */
    public static float getGroupOverflow(Player player) {
        IPlayerChainData data = PlayerChainCapability.get(player).orElse(null);
        return data == null ? 0.0f : data.getGroupOverflow();
    }

    /** 获取引爆冷却 */
    public static int getDetonateCooldown(Player player) {
        IPlayerChainData data = PlayerChainCapability.get(player).orElse(null);
        return data == null ? 0 : data.getDetonateCooldown();
    }

    /** 获取连携增益剩余时间 */
    public static int getChainBuffTicks(Player player) {
        IPlayerChainData data = PlayerChainCapability.get(player).orElse(null);
        return data == null ? 0 : data.getChainBuffTicks();
    }

    /** 清空玩家连携数据 */
    public static void clearChain(Player player) {
        IPlayerChainData data = PlayerChainCapability.get(player).orElse(null);
        if (data != null) {
            data.clearAll();
        }
    }

    /** 尝试触发连携引爆 */
    public static boolean tryDetonate(Player player) {
        return ChainManager.tryDetonate(player);
    }

    /** 尝试对破绽目标发动命定终结技 */
    public static boolean tryFinisher(Player player, LivingEntity target) {
        return ChainManager.tryFinisher(player, target);
    }

    /** 触发客户端按键对应的连携动作 */
    public static boolean handleAction(Player player, ChainActionType action) {
        return ChainManager.handleAction(player, action);
    }
}
