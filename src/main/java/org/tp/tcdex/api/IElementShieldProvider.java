package org.tp.tcdex.api;

import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.tp.tcdex.element.ElementType;

/**
 * 元素护盾提供器：附属 mod 注册后，可自定义任意生物的护盾元素。
 *
 * <p>分配优先级：黑名单（绝对无盾）→ 提供器（第一个返回非 null 者生效）→ 静态表 → 加权随机。</p>
 */
@FunctionalInterface
public interface IElementShieldProvider {

    /**
     * 返回该实体应拥有的护盾元素；返回 null 表示不接管（交给下一层逻辑）。
     */
    @Nullable
    ElementType getShieldElement(LivingEntity entity);
}
