package org.tp.tcdex.reaction;

import org.tp.tcdex.element.ElementType;

/**
 * 一条 TCDEX 元素反应定义。
 *
 * <p>由“目标身上已有元素 aura” + “本次触发元素 trigger” 组合而成。
 * 标准版使用附着量消耗：触发反应时从 aura 元素上消耗 auraCost 附着量。</p>
 */
public class ElementReaction {

    private final ElementType auraElement;
    private final ElementType triggerElement;
    private final ReactionType type;
    private final float auraCost;
    private final int cooldownTicks;
    private final int duration;
    private final float radius;
    private final float intensity;
    private final float damage;

    public ElementReaction(ElementType auraElement, ElementType triggerElement, ReactionType type,
                           float auraCost, int cooldownTicks, int duration, float radius, float intensity) {
        this(auraElement, triggerElement, type, auraCost, cooldownTicks, duration, radius, intensity, intensity);
    }

    public ElementReaction(ElementType auraElement, ElementType triggerElement, ReactionType type,
                           float auraCost, int cooldownTicks, int duration, float radius, float intensity, float damage) {
        this.auraElement = auraElement;
        this.triggerElement = triggerElement;
        this.type = type;
        this.auraCost = Math.max(0.01f, auraCost);
        this.cooldownTicks = Math.max(0, cooldownTicks);
        this.duration = Math.max(0, duration);
        this.radius = Math.max(0.0f, radius);
        this.intensity = intensity;
        this.damage = Math.max(0.0f, damage);
    }

    public ElementType getAuraElement() {
        return auraElement;
    }

    public ElementType getTriggerElement() {
        return triggerElement;
    }

    public ReactionType getType() {
        return type;
    }

    public float getAuraCost() {
        return auraCost;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    public int getDuration() {
        return duration;
    }

    public float getRadius() {
        return radius;
    }

    public float getIntensity() {
        return intensity;
    }

    /** 伤害类反应使用的伤害值；非伤害反应通常等于 intensity */
    public float getDamage() {
        return damage;
    }
}
