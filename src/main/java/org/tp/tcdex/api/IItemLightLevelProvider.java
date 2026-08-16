package org.tp.tcdex.api;

import net.minecraft.world.item.ItemStack;

/**
 * 物品光等提供器。
 *
 * <p>附属 mod 可以通过 {@link TcdexAPI#registerItemLightLevelProvider(IItemLightLevelProvider)}
 * 注册自己的物品光等，让非匠魂物品也能参与 TCDEX 光等系统。</p>
 */
public interface IItemLightLevelProvider {

    /**
     * 是否能处理这个物品。
     */
    boolean canProvide(ItemStack stack);

    /**
     * 获取该物品当前光等。
     */
    int getLightLevel(ItemStack stack);

    /**
     * 设置该物品光等。如果物品不支持写入，可以留空实现。
     */
    default void setLightLevel(ItemStack stack, int value) {
    }
}
