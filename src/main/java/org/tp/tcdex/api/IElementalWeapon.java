package org.tp.tcdex.api;

import net.minecraft.world.item.ItemStack;
import org.tp.tcdex.element.ElementType;

import javax.annotation.Nullable;

/**
 * 元素武器抽象：Core 通过该接口从 ItemStack 获取元素，避免直接依赖 Tinkers。
 */
public interface IElementalWeapon {

    @Nullable
    ElementType getElement(ItemStack stack);
}
