package org.tp.tcdex.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.tp.tcdex.chain.ElementActionType;
import org.tp.tcdex.element.ElementType;

import javax.annotation.Nullable;

/**
 * 自定义元素行为来源。
 *
 * <p>附属 Mod 注册后，TCDEX 在玩家使用对应物品/来源造成元素伤害时，
 * 会自动将行为按指定 {@link ElementActionType} 计入连携链。</p>
 */
public interface ElementActionSource {

    /** 是否匹配本次元素行为 */
    boolean matches(Player player, ItemStack stack, @Nullable LivingEntity target, ElementType element);

    /** 匹配时使用的连携行为类型 */
    ElementActionType getActionType();
}
