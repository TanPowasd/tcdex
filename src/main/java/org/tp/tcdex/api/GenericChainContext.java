package org.tp.tcdex.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.tp.tcdex.element.ElementType;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 通用元素链反应上下文。
 */
public final class GenericChainContext {

    private final Player player;
    private final List<ElementType> elements;
    private final ChainTriggerTime triggerTime;
    @Nullable
    private final LivingEntity target;
    private final Level level;

    public GenericChainContext(Player player, List<ElementType> elements,
                               ChainTriggerTime triggerTime, @Nullable LivingEntity target) {
        this.player = player;
        this.elements = List.copyOf(elements);
        this.triggerTime = triggerTime;
        this.target = target;
        this.level = player.level();
    }

    public Player getPlayer() {
        return player;
    }

    public List<ElementType> getElements() {
        return elements;
    }

    public ChainTriggerTime getTriggerTime() {
        return triggerTime;
    }

    @Nullable
    public LivingEntity getTarget() {
        return target;
    }

    public Level getLevel() {
        return level;
    }
}
