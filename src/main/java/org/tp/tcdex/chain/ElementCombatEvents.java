package org.tp.tcdex.chain;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.tp.tcdex.element.ElementType;

import javax.annotation.Nullable;

/**
 * 元素行为统一上报入口。
 *
 * <p>所有元素来源（武器、技能、爆发、反应、残响、破盾）都应通过这里上报，
 * 由 {@link ChainManager} 统一更新玩家连携链，避免各系统各自维护连携逻辑。</p>
 */
public final class ElementCombatEvents {

    private ElementCombatEvents() {
    }

    /**
     * 上报一次元素行为（仅更新玩家主链）。
     */
    public static void report(@Nullable Player player, ElementType element, ElementActionType actionType) {
        ChainManager.onElementAction(player, element, actionType, null);
    }

    /**
     * 上报一次元素行为，并同时更新当前目标准焦点链。
     */
    public static void report(@Nullable Player player, ElementType element, ElementActionType actionType,
                              @Nullable LivingEntity target) {
        ChainManager.onElementAction(player, element, actionType, target);
    }
}
