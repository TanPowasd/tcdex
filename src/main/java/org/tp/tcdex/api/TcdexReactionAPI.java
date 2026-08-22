package org.tp.tcdex.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.tp.tcdex.element.ElementManager;
import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.modifier.elemental.IElementalEntity;
import org.tp.tcdex.player.reaction.IPlayerReactionModifiers;
import org.tp.tcdex.player.reaction.PlayerReactionModifiersCapability;
import org.tp.tcdex.reaction.ElementReaction;
import org.tp.tcdex.reaction.ElementReactionModule;
import org.tp.tcdex.reaction.ElementReactionEngine;
import org.tp.tcdex.reaction.ElementReactionRegistry;
import org.tp.tcdex.reaction.ReactionModuleRegistry;

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

    /** 注册一条自定义元素反应（自动双向注册并同步默认模块） */
    public static void registerReaction(ElementReaction reaction) {
        ElementReactionRegistry.register(reaction);
        ReactionModuleRegistry.ensure(reaction);
        // 二元反应会自动注册反向，这里同时确保反向也有默认模块
        if (reaction.getCatalystElement() == null && reaction.getAuraElement() != reaction.getTriggerElement()) {
            ElementReaction reverse = ElementReactionRegistry.find(reaction.getTriggerElement(), reaction.getAuraElement());
            ReactionModuleRegistry.ensure(reverse);
        }
    }

    /** 注册自定义反应模块（完全自定义触发/效果） */
    public static void registerReactionModule(ElementReactionModule module) {
        ReactionModuleRegistry.register(module);
    }

    /** 取消注册一条反应（同时移除反向与对应的反应模块） */
    public static void unregisterReaction(ElementType aura, ElementType trigger) {
        ElementReaction removed = ElementReactionRegistry.unregister(aura, trigger);
        if (removed == null) {
            return;
        }
        ReactionModuleRegistry.unregister(aura, trigger);
        // 二元反应注册时会自动注册反向，这里同步清理反向模块
        if (removed.getCatalystElement() == null && removed.getAuraElement() != removed.getTriggerElement()) {
            ReactionModuleRegistry.unregister(removed.getTriggerElement(), removed.getAuraElement());
        }
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

    // ===== 玩家反应词条（默认全部开启） =====

    /** 玩家是否启用某个反应词条 */
    public static boolean hasReactionModifier(Player player, ElementReaction reaction) {
        return PlayerReactionModifiersCapability.get(player)
                .map(cap -> cap.hasModifier(org.tp.tcdex.reaction.ReactionModifierIds.forReaction(reaction)))
                .orElse(true);
    }

    /** 启用某个反应词条 */
    public static void enableReactionModifier(Player player, ElementReaction reaction) {
        PlayerReactionModifiersCapability.get(player).ifPresent(cap ->
                cap.addModifier(org.tp.tcdex.reaction.ReactionModifierIds.forReaction(reaction)));
    }

    /** 禁用某个反应词条 */
    public static void disableReactionModifier(Player player, ElementReaction reaction) {
        PlayerReactionModifiersCapability.get(player).ifPresent(cap ->
                cap.removeModifier(org.tp.tcdex.reaction.ReactionModifierIds.forReaction(reaction)));
    }

    /** 获取玩家当前禁用的反应词条 */
    public static java.util.Set<String> getDisabledReactionModifiers(Player player) {
        return PlayerReactionModifiersCapability.get(player)
                .map(IPlayerReactionModifiers::getDisabledModifiers)
                .orElse(java.util.Collections.emptySet());
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
        return ElementReactionEngine.triggerReaction(target, reaction, source);
    }

    /** 自动尝试触发：遍历目标身上已有附着，寻找可用反应 */
    public static void tryTriggerReaction(LivingEntity target, ElementType trigger, @Nullable LivingEntity source) {
        ElementReactionEngine.tryTriggerReaction(target, trigger, source);
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
        return ElementReactionEngine.isEnabled();
    }

    /** 设置元素反应总开关 */
    public static void setEnabled(boolean enabled) {
        ElementReactionEngine.setEnabled(enabled);
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
