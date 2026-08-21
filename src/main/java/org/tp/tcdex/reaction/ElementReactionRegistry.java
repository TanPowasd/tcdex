package org.tp.tcdex.reaction;

import org.tp.tcdex.element.ElementType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/**
 * TCDEX 元素反应注册中心。
 *
 * <p>默认注册第一批控制类反应。后续可通过 API 注册更多自定义反应。</p>
 */
public final class ElementReactionRegistry {

    private static final Map<ElementType, Map<ElementType, ElementReaction>> REACTIONS = new EnumMap<>(ElementType.class);

    static {
        registerDefaultReactions();
    }

    private ElementReactionRegistry() {
    }

    /** 注册一条反应（自动双向注册：A+B 与 B+A 使用同一条反应） */
    public static void register(ElementReaction reaction) {
        if (reaction == null || reaction.getAuraElement() == null || reaction.getTriggerElement() == null) {
            return;
        }
        if (reaction.getAuraElement() == ElementType.PRISM || reaction.getTriggerElement() == ElementType.PRISM) {
            return; // Prism 不参与常规七元素反应
        }
        registerDirection(reaction);
        // 双向：冰影+缚丝 与 缚丝+冰影 都能触发同一条霜缚
        if (reaction.getAuraElement() != reaction.getTriggerElement()) {
            registerDirection(new ElementReaction(
                    reaction.getTriggerElement(), reaction.getAuraElement(), reaction.getType(),
                    reaction.getAuraCost(), reaction.getCooldownTicks(), reaction.getDuration(),
                    reaction.getRadius(), reaction.getIntensity(), reaction.getDamage()));
        }
    }

    private static void registerDirection(ElementReaction reaction) {
        REACTIONS.computeIfAbsent(reaction.getAuraElement(), k -> new EnumMap<>(ElementType.class))
                .put(reaction.getTriggerElement(), reaction);
    }

    /** 查找目标已有元素 aura 被 trigger 触发时的反应；没有返回 null */
    public static ElementReaction find(ElementType aura, ElementType trigger) {
        if (aura == null || trigger == null || aura == ElementType.PRISM || trigger == ElementType.PRISM) {
            return null;
        }
        Map<ElementType, ElementReaction> map = REACTIONS.get(aura);
        return map == null ? null : map.get(trigger);
    }

    /** 取消注册一条反应（同时移除反向） */
    public static void unregister(ElementType aura, ElementType trigger) {
        if (aura == null || trigger == null) {
            return;
        }
        Map<ElementType, ElementReaction> map = REACTIONS.get(aura);
        if (map != null) {
            map.remove(trigger);
            if (map.isEmpty()) {
                REACTIONS.remove(aura);
            }
        }
        Map<ElementType, ElementReaction> reverse = REACTIONS.get(trigger);
        if (reverse != null) {
            reverse.remove(aura);
            if (reverse.isEmpty()) {
                REACTIONS.remove(trigger);
            }
        }
    }

    /** 获取当前注册的全部反应（只读快照） */
    public static Collection<ElementReaction> getAllReactions() {
        Collection<ElementReaction> reactions = new ArrayList<>();
        for (Map<ElementType, ElementReaction> map : REACTIONS.values()) {
            reactions.addAll(map.values());
        }
        return reactions;
    }

    /** 注册第一批控制类反应 */
    public static void registerDefaultReactions() {
        // 霜缚：冰影 + 缚丝 -> 冻结/禁锢
        register(new ElementReaction(ElementType.STASIS, ElementType.STRAND, ReactionType.CONTROL,
                1.0f, 40, 60, 0.0f, 0.0f));
        // 重力坍缩：沉星 + 虚空 -> 聚怪 + 重力压制
        register(new ElementReaction(ElementType.SINKSTAR, ElementType.VOID, ReactionType.CONTROL,
                1.0f, 60, 40, 4.0f, 0.6f));
        // 风暴锁链：电弧 + 岚流 -> 短暂麻痹
        register(new ElementReaction(ElementType.ARC, ElementType.MISTFLOW, ReactionType.CONTROL,
                1.0f, 40, 30, 0.0f, 0.0f));
        // 沉霜镇压：沉星 + 冰影 -> 重压减速、禁止跳跃
        register(new ElementReaction(ElementType.SINKSTAR, ElementType.STASIS, ReactionType.CONTROL,
                1.0f, 40, 60, 0.0f, 0.0f));
        // 岚蚀恐惧：虚空 + 岚流 -> 恐惧/强制逃跑
        register(new ElementReaction(ElementType.VOID, ElementType.MISTFLOW, ReactionType.CONTROL,
                1.0f, 60, 60, 0.0f, 0.0f));

        // ===== 伤害类 =====
        // 熔爆：烈日 + 电弧 -> 小范围 AOE 伤害
        register(new ElementReaction(ElementType.SOLAR, ElementType.ARC, ReactionType.DAMAGE,
                1.0f, 40, 0, 2.0f, 8.0f));
        // 湮灭：烈日 + 虚空 -> 高额单体伤害
        register(new ElementReaction(ElementType.SOLAR, ElementType.VOID, ReactionType.DAMAGE,
                1.0f, 60, 0, 0.0f, 12.0f));
        // 雷蚀：电弧 + 虚空 -> 中等 AOE 伤害
        register(new ElementReaction(ElementType.ARC, ElementType.VOID, ReactionType.DAMAGE,
                1.0f, 50, 0, 2.5f, 6.0f));

        // ===== 增幅类 =====
        // 热熔：烈日 + 冰影 -> 攻击者获得临时伤害提升
        register(new ElementReaction(ElementType.SOLAR, ElementType.STASIS, ReactionType.AMPLIFY,
                1.0f, 60, 200, 0.0f, 2.0f));

        // ===== 护盾类 =====
        // 星辉结晶：沉星 + 烈日 -> 攻击者获得临时吸收盾
        register(new ElementReaction(ElementType.SINKSTAR, ElementType.SOLAR, ReactionType.SHIELD,
                1.0f, 60, 200, 0.0f, 2.0f));
        // 雷晶护壁：沉星 + 电弧 -> 攻击者获得临时吸收盾
        register(new ElementReaction(ElementType.SINKSTAR, ElementType.ARC, ReactionType.SHIELD,
                1.0f, 60, 200, 0.0f, 2.0f));
    }
}
