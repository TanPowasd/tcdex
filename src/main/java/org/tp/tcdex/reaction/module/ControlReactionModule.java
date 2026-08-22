package org.tp.tcdex.reaction.module;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.reaction.ElementReaction;
import org.tp.tcdex.reaction.ElementReactionModule;
import org.tp.tcdex.reaction.ReactionContext;
import org.tp.tcdex.reaction.ReactionEffects;

/**
 * 控制类反应模块。
 */
public class ControlReactionModule implements ElementReactionModule {

    private final ElementReaction reaction;

    public ControlReactionModule(ElementReaction reaction) {
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
        int duration = effective.getDuration();
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 6, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 3, false, true));

        if (target instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.setTarget(null);
        }
        target.setDeltaMovement(0, 0, 0);
        target.hurtMarked = true;

        ElementType aura = effective.getAuraElement();
        ElementType trigger = effective.getTriggerElement();

        boolean sinkstarVoid = (aura == ElementType.SINKSTAR && trigger == ElementType.VOID)
                || (aura == ElementType.VOID && trigger == ElementType.SINKSTAR);
        boolean sinkstarStasis = (aura == ElementType.SINKSTAR && trigger == ElementType.STASIS)
                || (aura == ElementType.STASIS && trigger == ElementType.SINKSTAR);
        boolean voidMistflow = (aura == ElementType.VOID && trigger == ElementType.MISTFLOW)
                || (aura == ElementType.MISTFLOW && trigger == ElementType.VOID);

        if (sinkstarVoid && effective.getRadius() > 0) {
            ReactionEffects.pullEntities(target, effective.getRadius(), effective.getIntensity());
        }

        if (sinkstarStasis) {
            target.addEffect(new MobEffectInstance(MobEffects.JUMP, duration, -10, false, true));
        }

        if (voidMistflow) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration, 0, false, true));
        }

        ReactionEffects.spawnParticles(target, effective.getAuraElement().getParticle());
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.HOSTILE, 0.8F, 1.2F);
    }
}
