package org.tp.tcdex.api;

import net.minecraft.world.entity.LivingEntity;
import org.tp.tcdex.element.ElementManager;
import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.modifier.elemental.IElementalEntity;
import org.tp.tcdex.reaction.ElementReaction;
import org.tp.tcdex.reaction.ElementReactionEvents;
import org.tp.tcdex.reaction.ElementReactionRegistry;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * TCDEX 元素反应对外 API。
 *
 * <p>附属 mod 可以注册/取消自定义元素反应、查询反应、手动触发反应、
 * 读写目标元素附着量，并控制反应总开关与附着衰减速度。</p>
 */
public final class TcdexReactionAPI {

    private TcdexReactionAPI() {
    }

    // ===== 反应注册 =====

    /** 注册一条自定义元素反应（自动双向注册） */
    public static void registerReaction(ElementReaction reaction) {
        ElementReactionRegistry.register(reaction);
    }

    /** 取消注册一条反应（同时移除反向） */
    public static void unregisterReaction(ElementType aura, ElementType trigger) {
        ElementReactionRegistry.unregister(aura, trigger);
    }

    /** 查询已有元素 aura 被 trigger 触发时是否存在反应 */
    @Nullable
    public static ElementReaction findReaction(ElementType aura, ElementType trigger) {
        return ElementReactionRegistry.find(aura, trigger);
    }

    /** 获取当前注册的全部反应 */
    public static Collection<ElementReaction> getAllReactions() {
        return ElementReactionRegistry.getAllReactions();
    }

    // ===== 手动触发 =====

    /**
     * 尝试手动触发一次元素反应（会检查附着量、反应冷却并消耗附着量）。
     *
     * @return 是否成功触发
     */
    public static boolean triggerReaction(LivingEntity target, ElementType aura, ElementType trigger, @Nullable LivingEntity source) {
        ElementReaction reaction = ElementReactionRegistry.find(aura, trigger);
        if (reaction == null) {
            return false;
        }
        return ElementReactionEvents.triggerReaction(target, reaction, source);
    }

    /** 自动尝试触发：遍历目标身上已有附着，寻找可用反应 */
    public static void tryTriggerReaction(LivingEntity target, ElementType trigger, @Nullable LivingEntity source) {
        ElementReactionEvents.tryTriggerReaction(target, trigger, source);
    }

    // ===== 附着量 =====

    /** 获取实体某元素当前附着量 */
    public static float getAura(LivingEntity entity, ElementType type) {
        return IElementalEntity.of(entity).getAura(type);
    }

    /** 给实体增加指定元素的附着量并设置/刷新状态时长 */
    public static void addAura(LivingEntity entity, ElementType type, float amount, int duration) {
        IElementalEntity.of(entity).addAura(type, amount, duration);
    }

    /** 消耗实体某元素附着量，返回实际消耗值 */
    public static float consumeAura(LivingEntity entity, ElementType type, float amount) {
        return IElementalEntity.of(entity).consumeAura(type, amount);
    }

    /** 清除实体某元素附着（同时清除该元素状态） */
    public static void clearAura(LivingEntity entity, ElementType type) {
        IElementalEntity.of(entity).clearElementState(type);
    }

    // ===== 配置 =====

    /** 元素反应总开关 */
    public static boolean isEnabled() {
        return ElementReactionEvents.isEnabled();
    }

    /** 设置元素反应总开关 */
    public static void setEnabled(boolean enabled) {
        ElementReactionEvents.setEnabled(enabled);
    }

    /** 获取附着量每 tick 衰减速度 */
    public static float getAuraDecayPerTick() {
        return ElementManager.getAuraDecayPerTick();
    }

    /** 设置附着量每 tick 衰减速度 */
    public static void setAuraDecayPerTick(float decay) {
        ElementManager.setAuraDecayPerTick(decay);
    }
}
