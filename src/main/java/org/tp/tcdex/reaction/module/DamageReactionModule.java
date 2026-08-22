package org.tp.tcdex.reaction.module;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.tp.tcdex.damage.ModDamageSources;
import org.tp.tcdex.reaction.ElementReaction;
import org.tp.tcdex.reaction.ElementReactionModule;
import org.tp.tcdex.reaction.ReactionContext;
import org.tp.tcdex.reaction.ReactionEffects;

/**
 * 伤害类反应模块。
 */
public class DamageReactionModule implements ElementReactionModule {

    private final ElementReaction reaction;

    public DamageReactionModule(ElementReaction reaction) {
        this.reaction = reaction;
    }

    @Override
    public ElementReaction getReaction() {
        return reaction;
    }

    @Override
    public void onTrigger(ReactionContext context) {
        LivingEntity target = context.getTarget();
        LivingEntity source = context.getSource();
        ElementReaction effective = context.getReaction();
        float damage = effective.getDamage() > 0 ? effective.getDamage() : 4.0f;
        net.minecraft.world.damagesource.DamageSource damageSource = source != null
                ? ModDamageSources.element(source, effective.getAuraElement())
                : target.damageSources().magic();
        target.hurt(damageSource, damage);

        if (effective.getRadius() > 0) {
            Level level = context.getLevel();
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                    target.getBoundingBox().inflate(effective.getRadius()),
                    e -> e != target && e.isAlive() && !(e instanceof Player))) {
                entity.hurt(damageSource, damage * 0.5f);
            }
        }

        ReactionEffects.spawnParticles(target, effective.getAuraElement().getParticle());
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.8F, 1.4F);
    }
}
