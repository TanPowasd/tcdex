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
 * 增幅类反应模块。
 */
public class AmplifyReactionModule implements ElementReactionModule {

    private final ElementReaction reaction;

    public AmplifyReactionModule(ElementReaction reaction) {
        this.reaction = reaction;
    }

    @Override
    public ElementReaction getReaction() {
        return reaction;
    }

    @Override
    public void onTrigger(ReactionContext context) {
        ElementReaction effective = context.getReaction();
        LivingEntity source = context.getSource();
        if (source != null) {
            int amplifier = Math.max(0, (int) effective.getIntensity() - 1);
            source.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, effective.getDuration(), amplifier, false, true));
        }
        LivingEntity target = context.getTarget();
        ReactionEffects.spawnParticles(target, effective.getAuraElement().getParticle());
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.7F, 1.2F);
    }
}
