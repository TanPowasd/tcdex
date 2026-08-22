package org.tp.tcdex.reaction.module;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.tp.tcdex.modifier.elemental.IElementalEntity;
import org.tp.tcdex.reaction.ElementReaction;
import org.tp.tcdex.reaction.ElementReactionModule;
import org.tp.tcdex.reaction.ReactionContext;
import org.tp.tcdex.reaction.ReactionEffects;

/**
 * 扩散类反应模块。
 */
public class DiffusionReactionModule implements ElementReactionModule {

    private final ElementReaction reaction;

    public DiffusionReactionModule(ElementReaction reaction) {
        this.reaction = reaction;
    }

    @Override
    public ElementReaction getReaction() {
        return reaction;
    }

    @Override
    public void onTrigger(ReactionContext context) {
        LivingEntity target = context.getTarget();
        ElementReaction effective = context.getReaction();
        float radius = effective.getRadius() > 0 ? effective.getRadius() : 3.0f;
        Level level = context.getLevel();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                target.getBoundingBox().inflate(radius),
                e -> e != target && e.isAlive() && !(e instanceof Player))) {
            IElementalEntity.of(entity).addElementState(effective.getAuraElement(), 1, 100);
        }
        ReactionEffects.spawnParticles(target, effective.getAuraElement().getParticle());
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.HOSTILE, 0.6F, 1.4F);
    }
}
