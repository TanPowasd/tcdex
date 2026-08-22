package org.tp.tcdex.element;

import net.minecraft.world.entity.LivingEntity;
import org.tp.tcdex.modifier.elemental.ElementStatus;

import javax.annotation.Nullable;

/**
 * 元素效果处理器。
 *
 * <p>每个元素可以注册一个处理器，负责该元素在“受击 / 附着 / tick”等节点的特殊逻辑。
 * 这是元素体系重构后替代 {@code ElementalStateEvents} 巨型 switch 的扩展点。</p>
 */
public interface ElementEffectProcessor {

    /** 元素状态被施加到目标身上时调用 */
    default void onApply(LivingEntity target, ElementType element, float stacks, int duration) {
    }

    /** 元素状态在目标身上每 tick 结算时调用（服务端） */
    default void onTick(LivingEntity entity, ElementType element, ElementStatus status) {
    }

    /** 目标受到攻击时调用，用于关键词结算（如 Shatter / Volatile / Jolt / 月净化） */
    default void onHurt(LivingEntity target, ElementType element, @Nullable ElementType attackElement,
                        float amount, @Nullable LivingEntity attacker) {
    }
}
