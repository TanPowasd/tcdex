package org.tp.tcdex.reaction;

import org.tp.tcdex.element.ElementType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/**
 * TCDEX 元素反应注册中心。
 *
 * <p>按玩家提供的新反应表注册；支持二元反应和带催化剂的三元反应。</p>
 */
public final class ElementReactionRegistry {

    private static final Map<ElementType, Map<ElementType, ElementReaction>> REACTIONS = new EnumMap<>(ElementType.class);

    static {
        registerDefaultReactions();
    }

    private ElementReactionRegistry() {
    }

    /** 注册一条反应；二元反应自动注册反向，三元反应只注册指定方向 */
    public static void register(ElementReaction reaction) {
        if (reaction == null || reaction.getAuraElement() == null || reaction.getTriggerElement() == null) {
            return;
        }
        registerDirection(reaction);
        if (reaction.getCatalystElement() == null && reaction.getAuraElement() != reaction.getTriggerElement()) {
            registerDirection(new ElementReaction(
                    reaction.getTriggerElement(), reaction.getAuraElement(), null,
                    reaction.getType(), reaction.getAuraCost(), reaction.getCooldownTicks(),
                    reaction.getDuration(), reaction.getRadius(), reaction.getIntensity(), reaction.getDamage(),
                    reaction.getApplyElement(), reaction.getApplyStacks(), reaction.getApplyDuration(),
                    reaction.getPriority()));
        }
    }

    private static void registerDirection(ElementReaction reaction) {
        REACTIONS.computeIfAbsent(reaction.getAuraElement(), k -> new EnumMap<>(ElementType.class))
                .put(reaction.getTriggerElement(), reaction);
    }

    /** 查找目标已有元素 aura 被 trigger 触发时的反应；没有返回 null */
    public static ElementReaction find(ElementType aura, ElementType trigger) {
        if (aura == null || trigger == null) {
            return null;
        }
        Map<ElementType, ElementReaction> map = REACTIONS.get(aura);
        return map == null ? null : map.get(trigger);
    }

    /**
     * 取消注册一条反应。
     *
     * <p>二进制反应会连同自动注册的反向一起移除；三元反应只移除指定方向。
     * 返回被移除的反应定义，用于调用方同步清理模块。</p>
     */
    @javax.annotation.Nullable
    public static ElementReaction unregister(ElementType aura, ElementType trigger) {
        if (aura == null || trigger == null) {
            return null;
        }
        ElementReaction removed = find(aura, trigger);
        if (removed == null) {
            return null;
        }
        removeDirection(aura, trigger);
        if (removed.getCatalystElement() == null && removed.getAuraElement() != removed.getTriggerElement()) {
            removeDirection(trigger, aura);
        }
        return removed;
    }

    private static void removeDirection(ElementType aura, ElementType trigger) {
        Map<ElementType, ElementReaction> map = REACTIONS.get(aura);
        if (map != null) {
            map.remove(trigger);
            if (map.isEmpty()) {
                REACTIONS.remove(aura);
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

    /** 注册玩家提供的新版元素反应表 */
    public static void registerDefaultReactions() {
        // 烈日-冰影：融化 + 挂水
        register(ElementReaction.builder(ElementType.SOLAR, ElementType.STASIS, ReactionType.DAMAGE)
                .damage(12f)
                .cooldown(40)
                .priority(40)
                .applyElement(ElementType.TIDE, 1f, 100)
                .build());

        // 烈日-水：蒸发（高倍率直伤）
        register(ElementReaction.builder(ElementType.SOLAR, ElementType.TIDE, ReactionType.DAMAGE)
                .damage(14f)
                .cooldown(40)
                .priority(50)
                .build());

        // 电能-水：感电（附带小范围）
        register(ElementReaction.builder(ElementType.ARC, ElementType.TIDE, ReactionType.DAMAGE)
                .damage(8f)
                .radius(2f)
                .cooldown(35)
                .priority(40)
                .build());

        // 冰影-水：冻结（强控制）
        register(ElementReaction.builder(ElementType.STASIS, ElementType.TIDE, ReactionType.CONTROL)
                .duration(80)
                .cooldown(45)
                .priority(50)
                .build());

        // 虚空-罡流：虚空扩散
        register(ElementReaction.builder(ElementType.VOID, ElementType.MISTFLOW, ReactionType.DIFFUSION)
                .radius(3.5f)
                .cooldown(35)
                .priority(30)
                .build());

        // 虚空-水：暗流涌动
        register(ElementReaction.builder(ElementType.VOID, ElementType.TIDE, ReactionType.DAMAGE)
                .damage(10f)
                .radius(2f)
                .cooldown(45)
                .priority(40)
                .build());

        // 虚空-落星：虚空结晶（给攻击者临时吸收盾）
        register(ElementReaction.builder(ElementType.VOID, ElementType.SINKSTAR, ReactionType.SHIELD)
                .duration(160)
                .intensity(2)
                .cooldown(55)
                .priority(60)
                .build());

        // 月-虚空-落星：月结晶（三元反应，高优先级）
        register(ElementReaction.builder(ElementType.MOON, ElementType.VOID, ReactionType.SHIELD)
                .catalyst(ElementType.SINKSTAR)
                .duration(240)
                .intensity(3)
                .cooldown(60)
                .priority(100)
                .build());

        // 月-电能-水：月感电（三元反应，高优先级）
        register(ElementReaction.builder(ElementType.MOON, ElementType.ARC, ReactionType.DAMAGE)
                .catalyst(ElementType.TIDE)
                .damage(14f)
                .radius(2.5f)
                .cooldown(50)
                .priority(100)
                .build());

        // 月-缚丝：蜕散
        register(ElementReaction.builder(ElementType.MOON, ElementType.STRAND, ReactionType.DAMAGE)
                .damage(12f)
                .cooldown(40)
                .priority(40)
                .build());

        // 冰影-电能：聚导体（增幅）
        register(ElementReaction.builder(ElementType.STASIS, ElementType.ARC, ReactionType.AMPLIFY)
                .duration(200)
                .intensity(2)
                .cooldown(60)
                .priority(60)
                .build());

        // 水-罡流：扩散（罡流扩散水）
        register(ElementReaction.builder(ElementType.TIDE, ElementType.MISTFLOW, ReactionType.DIFFUSION)
                .radius(3.5f)
                .cooldown(35)
                .priority(30)
                .build());

        // 烈日-罡流：扩散（罡流扩散烈日）
        register(ElementReaction.builder(ElementType.SOLAR, ElementType.MISTFLOW, ReactionType.DIFFUSION)
                .radius(3.5f)
                .cooldown(35)
                .priority(30)
                .build());

        // 月-冰影：极致冰流（强控制）
        register(ElementReaction.builder(ElementType.MOON, ElementType.STASIS, ReactionType.CONTROL)
                .duration(100)
                .cooldown(50)
                .priority(70)
                .build());

        // 棱镜-月：月之暗面（高倍率单体/AOE）
        register(ElementReaction.builder(ElementType.PRISM, ElementType.MOON, ReactionType.DAMAGE)
                .damage(18f)
                .radius(3f)
                .cooldown(60)
                .priority(90)
                .build());

        // ===== 补全：光能 × 光能 =====

        // 烈日-电能：熔爆
        register(ElementReaction.builder(ElementType.SOLAR, ElementType.ARC, ReactionType.DAMAGE)
                .damage(10f)
                .radius(2f)
                .cooldown(40)
                .priority(45)
                .build());

        // 烈日-虚空：湮灭
        register(ElementReaction.builder(ElementType.SOLAR, ElementType.VOID, ReactionType.DAMAGE)
                .damage(14f)
                .cooldown(50)
                .priority(50)
                .build());

        // 电能-虚空：雷蚀
        register(ElementReaction.builder(ElementType.ARC, ElementType.VOID, ReactionType.DAMAGE)
                .damage(10f)
                .radius(2f)
                .cooldown(45)
                .priority(40)
                .build());

        // ===== 补全：光能 × 暗影 =====

        // 烈日-缚丝：燃缚
        register(ElementReaction.builder(ElementType.SOLAR, ElementType.STRAND, ReactionType.DAMAGE)
                .damage(10f)
                .radius(1.5f)
                .cooldown(40)
                .priority(40)
                .build());

        // 烈日-月：日蚀
        register(ElementReaction.builder(ElementType.SOLAR, ElementType.MOON, ReactionType.DAMAGE)
                .damage(16f)
                .radius(2f)
                .cooldown(50)
                .priority(70)
                .build());

        // 电能-缚丝：雷缠
        register(ElementReaction.builder(ElementType.ARC, ElementType.STRAND, ReactionType.DAMAGE)
                .damage(8f)
                .radius(1.5f)
                .cooldown(40)
                .priority(40)
                .build());


        // 虚空-冰影：虚空霜
        register(ElementReaction.builder(ElementType.VOID, ElementType.STASIS, ReactionType.DAMAGE)
                .damage(10f)
                .radius(1.5f)
                .cooldown(45)
                .priority(45)
                .build());

        // 虚空-缚丝：虚空缚
        register(ElementReaction.builder(ElementType.VOID, ElementType.STRAND, ReactionType.DAMAGE)
                .damage(10f)
                .radius(1.5f)
                .cooldown(45)
                .priority(45)
                .build());


        // ===== 补全：光能 × 中性 =====

        // 烈日-落星：星火护壁
        register(ElementReaction.builder(ElementType.SOLAR, ElementType.SINKSTAR, ReactionType.SHIELD)
                .duration(120)
                .intensity(1)
                .cooldown(50)
                .priority(50)
                .build());

        // 电能-罡流：风暴锁链
        register(ElementReaction.builder(ElementType.ARC, ElementType.MISTFLOW, ReactionType.CONTROL)
                .duration(60)
                .cooldown(40)
                .priority(45)
                .build());

        // 电能-落星：雷晶护壁
        register(ElementReaction.builder(ElementType.ARC, ElementType.SINKSTAR, ReactionType.SHIELD)
                .duration(140)
                .intensity(2)
                .cooldown(55)
                .priority(55)
                .build());

        // ===== 补全：暗影 × 暗影 =====

        // 冰影-缚丝：霜缚
        register(ElementReaction.builder(ElementType.STASIS, ElementType.STRAND, ReactionType.CONTROL)
                .duration(80)
                .cooldown(45)
                .priority(50)
                .build());

        // ===== 补全：暗影 × 中性 =====

        // 冰影-罡流：冰岚
        register(ElementReaction.builder(ElementType.STASIS, ElementType.MISTFLOW, ReactionType.CONTROL)
                .duration(70)
                .cooldown(45)
                .priority(45)
                .build());

        // 冰影-落星：沉霜镇压
        register(ElementReaction.builder(ElementType.STASIS, ElementType.SINKSTAR, ReactionType.CONTROL)
                .duration(70)
                .cooldown(45)
                .priority(50)
                .build());

        // 缚丝-罡流：风缚
        register(ElementReaction.builder(ElementType.STRAND, ElementType.MISTFLOW, ReactionType.CONTROL)
                .duration(60)
                .cooldown(40)
                .priority(40)
                .build());

        // 缚丝-水：潮缚
        register(ElementReaction.builder(ElementType.STRAND, ElementType.TIDE, ReactionType.CONTROL)
                .duration(60)
                .cooldown(40)
                .priority(40)
                .build());

        // 缚丝-落星：星缚
        register(ElementReaction.builder(ElementType.STRAND, ElementType.SINKSTAR, ReactionType.CONTROL)
                .duration(60)
                .cooldown(45)
                .priority(45)
                .build());

        // 月-罡流：月岚
        register(ElementReaction.builder(ElementType.MOON, ElementType.MISTFLOW, ReactionType.DIFFUSION)
                .radius(3.5f)
                .cooldown(40)
                .priority(40)
                .build());

        // 月-水：月潮
        register(ElementReaction.builder(ElementType.MOON, ElementType.TIDE, ReactionType.DAMAGE)
                .damage(10f)
                .radius(2f)
                .cooldown(45)
                .priority(45)
                .build());

        // 月-落星：月星
        register(ElementReaction.builder(ElementType.MOON, ElementType.SINKSTAR, ReactionType.SHIELD)
                .duration(180)
                .intensity(2)
                .cooldown(55)
                .priority(65)
                .build());

        // ===== 补全：中性 × 中性 =====

        // 罡流-落星：风星
        register(ElementReaction.builder(ElementType.MISTFLOW, ElementType.SINKSTAR, ReactionType.DIFFUSION)
                .radius(3.5f)
                .cooldown(35)
                .priority(35)
                .build());

        // 水-落星：星潮
        register(ElementReaction.builder(ElementType.TIDE, ElementType.SINKSTAR, ReactionType.CONTROL)
                .duration(80)
                .cooldown(45)
                .priority(45)
                .build());

        // ===== 补全：棱镜 × 各元素（强化反应） =====

        // 棱镜-烈日：棱镜烈阳
        register(ElementReaction.builder(ElementType.PRISM, ElementType.SOLAR, ReactionType.DAMAGE)
                .damage(16f)
                .radius(3f)
                .cooldown(55)
                .priority(80)
                .build());

        // 棱镜-电能：棱镜感电
        register(ElementReaction.builder(ElementType.PRISM, ElementType.ARC, ReactionType.DAMAGE)
                .damage(14f)
                .radius(2.5f)
                .cooldown(50)
                .priority(80)
                .build());

        // 棱镜-虚空：棱镜虚空
        register(ElementReaction.builder(ElementType.PRISM, ElementType.VOID, ReactionType.DAMAGE)
                .damage(14f)
                .radius(2.5f)
                .cooldown(50)
                .priority(85)
                .build());

        // 棱镜-冰影：棱镜霜封
        register(ElementReaction.builder(ElementType.PRISM, ElementType.STASIS, ReactionType.CONTROL)
                .duration(100)
                .cooldown(55)
                .priority(80)
                .build());

        // 棱镜-缚丝：棱镜缠绕
        register(ElementReaction.builder(ElementType.PRISM, ElementType.STRAND, ReactionType.CONTROL)
                .duration(100)
                .cooldown(55)
                .priority(80)
                .build());

        // 棱镜-罡流：棱镜风暴
        register(ElementReaction.builder(ElementType.PRISM, ElementType.MISTFLOW, ReactionType.DIFFUSION)
                .radius(3.5f)
                .cooldown(45)
                .priority(75)
                .build());

        // 棱镜-水：棱镜潮汐
        register(ElementReaction.builder(ElementType.PRISM, ElementType.TIDE, ReactionType.DAMAGE)
                .damage(12f)
                .radius(2f)
                .cooldown(50)
                .priority(70)
                .build());

        // 棱镜-落星：棱镜结晶
        register(ElementReaction.builder(ElementType.PRISM, ElementType.SINKSTAR, ReactionType.SHIELD)
                .duration(200)
                .intensity(3)
                .cooldown(60)
                .priority(90)
                .build());
    }
}
