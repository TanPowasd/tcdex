package org.tp.tcdex.reaction;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.tp.tcdex.element.ElementType;

import javax.annotation.Nullable;

/**
 * 元素反应触发上下文。
 *
 * <p>封装一次反应触发所需的数据：目标、攻击者/施法者、已有元素、触发元素、
 * 催化剂元素、反应定义和世界实例。</p>
 */
public class ReactionContext {

    private final Level level;
    private final LivingEntity target;
    @Nullable
    private final LivingEntity source;
    private final ElementType aura;
    private final ElementType trigger;
    @Nullable
    private final ElementType catalyst;
    private final ElementReaction reaction;

    public ReactionContext(Level level, LivingEntity target, @Nullable LivingEntity source,
                           ElementType aura, ElementType trigger, @Nullable ElementType catalyst,
                           ElementReaction reaction) {
        this.level = level;
        this.target = target;
        this.source = source;
        this.aura = aura;
        this.trigger = trigger;
        this.catalyst = catalyst;
        this.reaction = reaction;
    }

    public Level getLevel() {
        return level;
    }

    public LivingEntity getTarget() {
        return target;
    }

    @Nullable
    public LivingEntity getSource() {
        return source;
    }

    public ElementType getAura() {
        return aura;
    }

    public ElementType getTrigger() {
        return trigger;
    }

    @Nullable
    public ElementType getCatalyst() {
        return catalyst;
    }

    public ElementReaction getReaction() {
        return reaction;
    }
}
