package org.tp.tcdex.reaction;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.tp.tcdex.api.ITinkersBridge;
import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.integration.tinkers.TinkersBridgeHolder;
import org.tp.tcdex.mastery.ElementalMasteryManager;
import org.tp.tcdex.modifier.elemental.ElementStatus;
import org.tp.tcdex.modifier.elemental.IElementalEntity;
import org.tp.tcdex.chain.ElementActionType;
import org.tp.tcdex.chain.ElementCombatEvents;
import org.tp.tcdex.debug.TcdexDebug;
import org.tp.tcdex.player.reaction.PlayerReactionModifiersCapability;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 元素反应核心引擎。
 *
 * <p>负责反应条件检查、数值调整、附着消耗、模块执行与 Hook 回调。
 * Forge 事件入口见 {@link ElementReactionEvents}，外部 mod 可直接调用
 * {@link #tryTriggerReaction} / {@link #triggerReaction}。</p>
 */
public final class ElementReactionEngine {

    /** 岚流通用扩散半径 */
    private static final float DEFAULT_DIFFUSION_RADIUS = 3.0f;
    /** 岚流通用扩散附着消耗 */
    private static final float DEFAULT_DIFFUSION_COST = 1.0f;
    /** 岚流通用扩散冷却 */
    private static final int DEFAULT_DIFFUSION_COOLDOWN = 40;

    /** 元素反应总开关（配置 elementReactionsEnabled） */
    private static boolean enabled = true;

    /** 防止反应模块造成的元素伤害再次进入反应管线导致递归/失控连锁 */
    private static final ThreadLocal<Boolean> IN_REACTION = ThreadLocal.withInitial(() -> false);

    private ElementReactionEngine() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    /**
     * 尝试触发一次元素反应。
     *
     * @param target  反应目标（身上已有元素附着）
     * @param trigger 本次触发元素
     * @param source  攻击者/施法者，可为 null
     */
    public static void tryTriggerReaction(LivingEntity target, ElementType trigger, @Nullable LivingEntity source) {
        Level level = target.level();
        if (!enabled || level.isClientSide || trigger == null) {
            return;
        }
        IElementalEntity data = IElementalEntity.of(target);
        List<ReactionCandidate> candidates = new ArrayList<>();

        if (trigger == ElementType.MISTFLOW) {
            // 岚流扩散：岚流触发任意已有元素附着，把该元素扩散给周围敌人。
            // 优先使用注册表中已有的扩散反应；没有显式定义时使用通用扩散反应。
            for (Map.Entry<ElementType, ElementStatus> entry : data.getAllElementStates().entrySet()) {
                ElementType aura = entry.getKey();
                ElementStatus status = entry.getValue();
                if (aura == ElementType.MISTFLOW || status.aura <= 0) {
                    continue;
                }
                ElementReaction reaction = ElementReactionRegistry.find(aura, ElementType.MISTFLOW);
                if (reaction == null) {
                    reaction = new ElementReaction(
                            aura, ElementType.MISTFLOW, null, ReactionType.DIFFUSION,
                            DEFAULT_DIFFUSION_COST, DEFAULT_DIFFUSION_COOLDOWN, 0,
                            DEFAULT_DIFFUSION_RADIUS, 0.0f, 0.0f, null, 0, 0);
                }
                candidates.add(new ReactionCandidate(aura, reaction));
            }
        } else {
            // 普通反应：查找目标已有元素 aura + 本次触发元素 trigger
            for (Map.Entry<ElementType, ElementStatus> entry : data.getAllElementStates().entrySet()) {
                ElementType aura = entry.getKey();
                ElementStatus status = entry.getValue();
                if (aura == trigger || status.aura <= 0) {
                    continue;
                }
                ElementReaction reaction = ElementReactionRegistry.find(aura, trigger);
                if (reaction == null) {
                    continue;
                }
                candidates.add(new ReactionCandidate(aura, reaction));
            }
        }

        // 按优先级从高到低选择；同优先级时优先选择附着量更高的元素。
        candidates.sort((a, b) -> {
            int byPriority = Integer.compare(b.reaction.getPriority(), a.reaction.getPriority());
            if (byPriority != 0) {
                return byPriority;
            }
            return Float.compare(data.getAura(b.aura), data.getAura(a.aura));
        });

        for (ReactionCandidate candidate : candidates) {
            if (executeReaction(target, candidate.aura, trigger, candidate.reaction, source)) {
                return;
            }
        }
    }

    private record ReactionCandidate(ElementType aura, ElementReaction reaction) {
    }

    /**
     * 使用指定反应直接尝试触发（供 API 手动触发）。
     *
     * @return 是否成功触发
     */
    public static boolean triggerReaction(LivingEntity target, ElementReaction reaction, @Nullable LivingEntity source) {
        if (!enabled || target.level().isClientSide || reaction == null) {
            return false;
        }
        return executeReaction(target, reaction.getAuraElement(), reaction.getTriggerElement(), reaction, source);
    }

    /**
     * 统一的反应执行管线。
     *
     * <p>所有入口最终都经过这里：调整数值 → 模块条件检查 → 玩家词条检查 →
     * 催化剂/附着消耗 → 模块效果 → 附加元素 → REACTION Hook 回调。</p>
     */
    private static boolean executeReaction(LivingEntity target, ElementType aura, ElementType trigger,
                                           ElementReaction reaction, @Nullable LivingEntity source) {
        if (aura == null || trigger == null || aura == trigger || IN_REACTION.get()) {
            return false;
        }
        IN_REACTION.set(true);
        try {
            Level level = target.level();
            IElementalEntity data = IElementalEntity.of(target);
            long now = level.getGameTime();

            ElementStatus status = data.getAllElementStates().get(aura);
            if (status == null || status.aura <= 0) {
                return false;
            }

            ElementReaction effective = adjustReaction(source, reaction);
            ReactionContext context = new ReactionContext(level, target, source, aura, trigger,
                    effective.getCatalystElement(), effective);
            ElementReactionModule module = ReactionModuleRegistry.getOrCreate(effective);

            // 模块条件与模块级数值调整
            if (module != null) {
                if (!module.canTrigger(context)) {
                    return false;
                }
                effective = applyModuleModifiers(module, context, effective);
                context = new ReactionContext(level, target, source, aura, trigger,
                        effective.getCatalystElement(), effective);
            }

            // 冷却检查必须在消耗之前
            if (now - status.lastReactionTime < effective.getCooldownTicks()) {
                return false;
            }

            // 玩家必须拥有对应反应词条才会触发；必须在消耗附着前检查
            if (source instanceof Player player) {
                final ElementReaction reactionForCheck = effective;
                boolean hasModifier = PlayerReactionModifiersCapability.get(player)
                        .map(cap -> cap.hasModifier(ReactionModifierIds.forReaction(reactionForCheck)))
                        .orElse(true);
                if (!hasModifier) {
                    return false;
                }
            }

            // 三元反应：还需要目标身上有催化剂元素附着
            if (effective.getCatalystElement() != null) {
                ElementType catalyst = effective.getCatalystElement();
                ElementStatus catalystStatus = data.getAllElementStates().get(catalyst);
                if (catalystStatus == null || catalystStatus.aura <= 0) {
                    return false;
                }
                float catalystCost = Math.max(0.1f, effective.getAuraCost() * 0.5f);
                float catalystConsumed = data.consumeAura(catalyst, catalystCost);
                if (catalystConsumed <= 0) {
                    return false;
                }
                data.markReaction(catalyst, now);
            }

            float consumed = data.consumeAura(aura, effective.getAuraCost());
            if (consumed <= 0) {
                return false;
            }
            data.markReaction(aura, now);
            data.markReaction(trigger, now);

            // 执行模块效果（使用调整后的 effective reaction）
            if (module != null) {
                module.onTrigger(context);
            }

            // 部分反应会附加元素（如融化后挂水）
            if (effective.getApplyElement() != null) {
                IElementalEntity.of(target).addElementState(
                        effective.getApplyElement(), effective.getApplyStacks(), effective.getApplyDuration());
            }

            // 元素反应计入连携链
            if (source instanceof Player player) {
                ElementCombatEvents.report(player, effective.getTriggerElement(), ElementActionType.REACTION, target);
            }

            dispatchReactionTriggered(source, target, effective);
            debugReactionTriggered(source, target, effective);
            return true;
        } finally {
            IN_REACTION.remove();
        }
    }

    /** 应用模块自带的数值调整 */
    private static ElementReaction applyModuleModifiers(ElementReactionModule module, ReactionContext context,
                                                        ElementReaction reaction) {
        float auraCost = module.modifyAuraCost(context, reaction.getAuraCost());
        float damage = module.modifyDamage(context, reaction.getDamage());
        int duration = module.modifyDuration(context, reaction.getDuration());
        float radius = module.modifyRadius(context, reaction.getRadius());
        float intensity = module.modifyIntensity(context, reaction.getIntensity());
        int cooldown = module.modifyCooldown(context, reaction.getCooldownTicks());
        return new ElementReaction(
                reaction.getAuraElement(), reaction.getTriggerElement(), reaction.getCatalystElement(),
                reaction.getType(),
                Math.max(0.01f, auraCost),
                Math.max(0, cooldown),
                Math.max(0, duration),
                Math.max(0.0f, radius),
                intensity,
                Math.max(0.0f, damage),
                reaction.getApplyElement(), reaction.getApplyStacks(), reaction.getApplyDuration(),
                reaction.getPriority());
    }

    /** 通过攻击者手持匠魂工具上的 REACTION hook 调整反应参数 */
    private static ElementReaction adjustReaction(@Nullable LivingEntity source, ElementReaction reaction) {
        if (!(source instanceof Player player)) {
            return reaction;
        }
        float auraCost = reaction.getAuraCost();
        float duration = reaction.getDuration();
        float radius = reaction.getRadius();
        float intensity = reaction.getIntensity();
        float damage = reaction.getDamage();
        int cooldown = reaction.getCooldownTicks();
        if (TinkersBridgeHolder.isAvailable()) {
            ITinkersBridge bridge = TinkersBridgeHolder.get();
            for (ItemStack stack : List.of(player.getMainHandItem(), player.getOffhandItem())) {
                if (!bridge.isUsableTinkersTool(stack)) {
                    continue;
                }
                auraCost = bridge.modifyReactionAuraCost(stack, reaction, auraCost);
                duration = bridge.modifyReactionDuration(stack, reaction, duration);
                radius = bridge.modifyReactionRadius(stack, reaction, radius);
                intensity = bridge.modifyReactionIntensity(stack, reaction, intensity);
                damage = bridge.modifyReactionDamage(stack, reaction, damage);
                cooldown = bridge.modifyReactionCooldown(stack, reaction, cooldown);

                // 武器催化：每级提升反应伤害/持续时间/范围，降低冷却
                int catalystLevel = bridge.getCatalystLevel(stack);
                if (catalystLevel > 0) {
                    duration *= 1.0f + catalystLevel * 0.05f;
                    radius += catalystLevel * 0.5f;
                    damage *= 1.0f + catalystLevel * 0.10f;
                    cooldown -= catalystLevel * 2;
                }
            }
        }
        // 元素精通全局属性：统一增强反应伤害/持续时间/范围，降低冷却/附着消耗
        auraCost *= ElementalMasteryManager.getAuraCostMultiplier(player);
        duration *= ElementalMasteryManager.getDurationMultiplier(player);
        radius += ElementalMasteryManager.getRadiusBonus(player);
        damage *= ElementalMasteryManager.getDamageMultiplier(player);
        cooldown = (int) (cooldown * ElementalMasteryManager.getCooldownMultiplier(player));

        return new ElementReaction(reaction.getAuraElement(), reaction.getTriggerElement(), reaction.getCatalystElement(),
                reaction.getType(),
                Math.max(0.01f, auraCost),
                Math.max(0, cooldown),
                Math.max(0, (int) duration),
                Math.max(0.0f, radius),
                intensity,
                Math.max(0.0f, damage),
                reaction.getApplyElement(), reaction.getApplyStacks(), reaction.getApplyDuration(),
                reaction.getPriority());
    }

    /** 反应触发后回调所有攻击者工具上的 REACTION hook */
    private static void dispatchReactionTriggered(@Nullable LivingEntity source, LivingEntity target, ElementReaction reaction) {
        if (!(source instanceof Player player)) {
            return;
        }
        if (!TinkersBridgeHolder.isAvailable()) {
            return;
        }
        ITinkersBridge bridge = TinkersBridgeHolder.get();
        for (ItemStack stack : List.of(player.getMainHandItem(), player.getOffhandItem())) {
            if (bridge.isUsableTinkersTool(stack)) {
                bridge.onReactionTriggered(stack, target, reaction, source, reaction.getIntensity());
            }
        }
    }

    /** 反应触发时向相关玩家输出调试信息 */
    private static void debugReactionTriggered(@Nullable LivingEntity source, LivingEntity target, ElementReaction reaction) {
        if (!TcdexDebug.isReactionDebugEnabled()) {
            return;
        }
        String reactionPath = reaction.getAuraElement().getId()
                + (reaction.getCatalystElement() != null ? "+" + reaction.getCatalystElement().getId() : "")
                + "+" + reaction.getTriggerElement().getId();
        String message = String.format("[TCDEX反应] %s -> %s | 目标: %s",
                reactionPath, reaction.getType().name(), target.getDisplayName().getString());
        if (source instanceof Player player) {
            player.sendSystemMessage(Component.literal(message));
        } else if (target instanceof Player player) {
            player.sendSystemMessage(Component.literal(message));
        }
    }
}
