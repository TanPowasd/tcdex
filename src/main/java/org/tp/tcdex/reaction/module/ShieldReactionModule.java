package org.tp.tcdex.reaction.module;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.tp.tcdex.reaction.ElementReaction;
import org.tp.tcdex.reaction.ElementReactionModule;
import org.tp.tcdex.reaction.ReactionContext;
import org.tp.tcdex.reaction.ReactionEffects;

/**
 * 护盾类反应模块。
 */
public class ShieldReactionModule implements ElementReactionModule {

    private final ElementReaction reaction;

    public ShieldReactionModule(ElementReaction reaction) {
        this.reaction = reaction;
    }

    @Override
    public ElementReaction getReaction() {
        return reaction;
    }

    @Override
    public void onTrigger(ReactionContext context) {
        ElementReaction effective = context.getReaction();
        LivingEntity receiver = context.getSource() != null ? context.getSource() : context.getTarget();
        int amplifier = Math.max(0, (int) effective.getIntensity() - 1);
        receiver.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, effective.getDuration(), amplifier, false, true));
        ReactionEffects.spawnParticles(receiver, effective.getAuraElement().getParticle());
        receiver.level().playSound(null, receiver.getX(), receiver.getY(), receiver.getZ(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.8F, 1.2F);
    }
}
