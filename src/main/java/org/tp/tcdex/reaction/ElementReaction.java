package org.tp.tcdex.reaction;

import org.tp.tcdex.element.ElementType;

import javax.annotation.Nullable;

/**
 * 一条 TCDEX 元素反应定义。
 *
 * <p>支持两类反应：</p>
 * <ul>
 *   <li>二元反应：aura + trigger</li>
 *   <li>三元反应：aura + trigger + catalyst（如 月 + 虚空 + 落星 → 月结晶）</li>
 * </ul>
 *
 * <p>部分反应还会在触发后给目标附加一个元素状态（如 烈日 + 冰影 → 融化并挂水）。</p>
 */
public class ElementReaction {

    private final ElementType auraElement;
    private final ElementType triggerElement;
    @Nullable
    private final ElementType catalystElement;
    private final ReactionType type;
    private final float auraCost;
    private final int cooldownTicks;
    private final int duration;
    private final float radius;
    private final float intensity;
    private final float damage;

    /** 反应触发后附加到目标身上的元素（可为 null） */
    @Nullable
    private final ElementType applyElement;
    private final float applyStacks;
    private final int applyDuration;
    private final int priority;

    public ElementReaction(ElementType auraElement, ElementType triggerElement, ReactionType type,
                           float auraCost, int cooldownTicks, int duration, float radius, float intensity) {
        this(auraElement, triggerElement, null, type, auraCost, cooldownTicks, duration, radius, intensity, intensity, null, 0, 0);
    }

    public ElementReaction(ElementType auraElement, ElementType triggerElement, ReactionType type,
                           float auraCost, int cooldownTicks, int duration, float radius, float intensity, float damage) {
        this(auraElement, triggerElement, null, type, auraCost, cooldownTicks, duration, radius, intensity, damage, null, 0, 0);
    }

    public ElementReaction(ElementType auraElement, ElementType triggerElement, @Nullable ElementType catalystElement,
                           ReactionType type, float auraCost, int cooldownTicks, int duration, float radius,
                           float intensity, float damage,
                           @Nullable ElementType applyElement, float applyStacks, int applyDuration) {
        this(auraElement, triggerElement, catalystElement, type, auraCost, cooldownTicks, duration, radius,
                intensity, damage, applyElement, applyStacks, applyDuration, 0);
    }

    public ElementReaction(ElementType auraElement, ElementType triggerElement, @Nullable ElementType catalystElement,
                           ReactionType type, float auraCost, int cooldownTicks, int duration, float radius,
                           float intensity, float damage,
                           @Nullable ElementType applyElement, float applyStacks, int applyDuration, int priority) {
        this.auraElement = auraElement;
        this.triggerElement = triggerElement;
        this.catalystElement = catalystElement;
        this.type = type;
        this.auraCost = Math.max(0.01f, auraCost);
        this.cooldownTicks = Math.max(0, cooldownTicks);
        this.duration = Math.max(0, duration);
        this.radius = Math.max(0.0f, radius);
        this.intensity = intensity;
        this.damage = Math.max(0.0f, damage);
        this.applyElement = applyElement;
        this.applyStacks = applyStacks;
        this.applyDuration = applyDuration;
        this.priority = priority;
    }

    public static Builder builder(ElementType aura, ElementType trigger, ReactionType type) {
        return new Builder(aura, trigger, type);
    }

    public ElementType getAuraElement() {
        return auraElement;
    }

    public ElementType getTriggerElement() {
        return triggerElement;
    }

    @Nullable
    public ElementType getCatalystElement() {
        return catalystElement;
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

    @Nullable
    public ElementType getApplyElement() {
        return applyElement;
    }

    public float getApplyStacks() {
        return applyStacks;
    }

    public int getApplyDuration() {
        return applyDuration;
    }

    /** 优先级：数值越大越优先触发（默认 0） */
    public int getPriority() {
        return priority;
    }

    public static final class Builder {
        private final ElementType auraElement;
        private final ElementType triggerElement;
        private ElementType catalystElement;
        private final ReactionType type;
        private float auraCost = 1.0f;
        private int cooldownTicks = 40;
        private int duration = 0;
        private float radius = 0.0f;
        private float intensity = 0.0f;
        private float damage = 0.0f;
        private ElementType applyElement;
        private float applyStacks = 0;
        private int applyDuration = 0;
        private int priority = 0;

        private Builder(ElementType aura, ElementType trigger, ReactionType type) {
            this.auraElement = aura;
            this.triggerElement = trigger;
            this.type = type;
        }

        public Builder catalyst(ElementType catalystElement) {
            this.catalystElement = catalystElement;
            return this;
        }

        public Builder auraCost(float auraCost) {
            this.auraCost = auraCost;
            return this;
        }

        public Builder cooldown(int cooldownTicks) {
            this.cooldownTicks = cooldownTicks;
            return this;
        }

        public Builder duration(int duration) {
            this.duration = duration;
            return this;
        }

        public Builder radius(float radius) {
            this.radius = radius;
            return this;
        }

        public Builder intensity(float intensity) {
            this.intensity = intensity;
            return this;
        }

        public Builder damage(float damage) {
            this.damage = damage;
            return this;
        }

        public Builder applyElement(ElementType applyElement, float stacks, int duration) {
            this.applyElement = applyElement;
            this.applyStacks = stacks;
            this.applyDuration = duration;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public ElementReaction build() {
            return new ElementReaction(auraElement, triggerElement, catalystElement, type,
                    auraCost, cooldownTicks, duration, radius, intensity, damage,
                    applyElement, applyStacks, applyDuration, priority);
        }
    }
}
