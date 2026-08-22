package org.tp.tcdex.api;

import org.tp.tcdex.reaction.ReactionType;

/**
 * 通用元素链反应内置效果。
 *
 * <p>支持五类基础效果，并可通过布尔选项扩展为：
 * 对玩家自身施加效果、触发附近元素反应。</p>
 */
public final class GenericChainEffect {

    private final ReactionType type;
    private final float damage;
    private final float radius;
    private final int duration;
    private final float intensity;
    private final boolean selfBuff;
    private final boolean triggerReactions;

    public GenericChainEffect(ReactionType type, float damage, float radius, int duration,
                              float intensity, boolean selfBuff, boolean triggerReactions) {
        this.type = type;
        this.damage = damage;
        this.radius = radius;
        this.duration = duration;
        this.intensity = intensity;
        this.selfBuff = selfBuff;
        this.triggerReactions = triggerReactions;
    }

    public ReactionType getType() {
        return type;
    }

    public float getDamage() {
        return damage;
    }

    public float getRadius() {
        return radius;
    }

    public int getDuration() {
        return duration;
    }

    public float getIntensity() {
        return intensity;
    }

    public boolean isSelfBuff() {
        return selfBuff;
    }

    public boolean isTriggerReactions() {
        return triggerReactions;
    }
}
