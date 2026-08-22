package org.tp.tcdex.chain;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.tp.tcdex.api.ChainTriggerTime;
import org.tp.tcdex.api.GenericChainContext;
import org.tp.tcdex.api.GenericChainEffect;
import org.tp.tcdex.api.GenericChainReaction;
import org.tp.tcdex.api.TcdexChainRegistry;
import org.tp.tcdex.damage.ModDamageSources;
import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.modifier.elemental.IElementalEntity;
import org.tp.tcdex.reaction.ElementReactionEngine;
import org.tp.tcdex.reaction.ReactionType;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

/**
 * 通用元素链反应执行器。
 */
public final class GenericChainReactionExecutor {

    private GenericChainReactionExecutor() {
    }

    /**
     * 在指定时机触发所有匹配的通用元素链反应。
     *
     * @return 触发数量
     */
    public static int trigger(Player player, List<ElementType> elements,
                              ChainTriggerTime time, @Nullable LivingEntity target) {
        if (player.level().isClientSide || elements.isEmpty()) {
            return 0;
        }
        GenericChainContext context = new GenericChainContext(player, elements, time, target);
        List<GenericChainReaction> reactions = TcdexChainRegistry.getGenericReactions();
        reactions.sort(Comparator.comparingInt(GenericChainReaction::getPriority).reversed());

        int triggered = 0;
        for (GenericChainReaction reaction : reactions) {
            if (!reaction.getTriggerTimes().contains(time)) {
                continue;
            }
            if (!reaction.getMatcher().test(context)) {
                continue;
            }
            execute(reaction, context);
            triggered++;
        }
        return triggered;
    }

    private static void execute(GenericChainReaction reaction, GenericChainContext context) {
        if (reaction.getCallback() != null) {
            reaction.getCallback().accept(context);
        }
        GenericChainEffect effect = reaction.getEffect();
        if (effect == null) {
            return;
        }

        Player player = context.getPlayer();
        LivingEntity target = context.getTarget();
        Level level = context.getLevel();
        float radius = effect.getRadius() > 0 ? effect.getRadius() : 3.0f;
        AABB box = target != null
                ? target.getBoundingBox().inflate(radius)
                : player.getBoundingBox().inflate(radius);
        List<LivingEntity> areaTargets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && !(e instanceof Player));

        switch (effect.getType()) {
            case DAMAGE -> {
                float damage = effect.getDamage();
                for (LivingEntity targetEntity : areaTargets) {
                    targetEntity.hurt(ModDamageSources.element(player, context.getElements().get(0)), damage);
                }
            }
            case CONTROL -> {
                for (LivingEntity targetEntity : areaTargets) {
                    targetEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, effect.getDuration(), 2, false, true));
                    targetEntity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, effect.getDuration(), 1, false, true));
                }
                if (effect.isSelfBuff()) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, effect.getDuration(), 1, false, true));
                }
            }
            case SHIELD -> {
                LivingEntity receiver = effect.isSelfBuff() ? player : (target != null ? target : player);
                int amplifier = Math.max(0, (int) effect.getIntensity() - 1);
                receiver.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, effect.getDuration(), amplifier, false, true));
            }
            case AMPLIFY -> {
                LivingEntity receiver = effect.isSelfBuff() ? player : (target != null ? target : player);
                int amplifier = Math.max(0, (int) effect.getIntensity() - 1);
                receiver.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, effect.getDuration(), amplifier, false, true));
            }
            case DIFFUSION -> {
                for (LivingEntity targetEntity : areaTargets) {
                    for (ElementType element : context.getElements()) {
                        IElementalEntity.of(targetEntity).addElementState(element, 1, 100);
                    }
                }
            }
        }

        if (effect.isTriggerReactions()) {
            for (LivingEntity targetEntity : areaTargets) {
                for (ElementType element : context.getElements()) {
                    ElementReactionEngine.tryTriggerReaction(targetEntity, element, player);
                }
            }
        }
    }
}
