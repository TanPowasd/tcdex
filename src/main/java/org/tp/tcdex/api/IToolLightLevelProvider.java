package org.tp.tcdex.api;

import net.minecraft.world.item.ItemStack;

/**
 * 工具光等提供器：Addon/联动实现匠魂工具光等计算。
 */
public interface IToolLightLevelProvider {

    boolean canProvide(ItemStack stack);

    int getLightLevel(ItemStack stack);
}
