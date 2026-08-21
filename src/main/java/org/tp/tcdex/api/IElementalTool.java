package org.tp.tcdex.api;

import net.minecraft.world.item.ItemStack;

/**
 * 元素工具抽象：供武器催化等功能使用，避免 Core 直接依赖 Tinkers。
 */
public interface IElementalTool {

    int getCatalystLevel(ItemStack stack);

    void addCatalystProgress(ItemStack stack, int amount);
}
