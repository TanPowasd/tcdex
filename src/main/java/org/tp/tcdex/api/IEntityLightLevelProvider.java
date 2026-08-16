package org.tp.tcdex.api;

import net.minecraft.world.entity.LivingEntity;

/**
 * 实体光等提供器。
 *
 * <p>附属 mod 可以通过 {@link TcdexAPI#registerEntityLightLevelProvider(IEntityLightLevelProvider)}
 * 为自己的生物提供自定义光等逻辑。</p>
 */
public interface IEntityLightLevelProvider {

    /**
     * 是否能处理这个实体。
     */
    boolean canProvide(LivingEntity entity);

    /**
     * 获取该实体当前光等。
     */
    int getLightLevel(LivingEntity entity);

    /**
     * 设置该实体光等。如果实体不支持写入，可以留空实现。
     */
    default void setLightLevel(LivingEntity entity, int value) {
    }
}
