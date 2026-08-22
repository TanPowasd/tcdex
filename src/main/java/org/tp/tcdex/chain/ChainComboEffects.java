package org.tp.tcdex.chain;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.tp.tcdex.damage.ModDamageSources;
import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.modifier.elemental.IElementalEntity;
import org.tp.tcdex.reaction.ReactionType;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 连携二元组合专属效果执行器。
 */
public final class ChainComboEffects {

    private ChainComboEffects() {
    }

    /**
     * 应用链中所有二元连携组合的专属效果。
     *
     * @param finisher 是否为终结技（终结技效果增强）
     */
    public static void apply(Level level, Vec3 center, List<ElementType> elements,
                             @Nullable Player player, boolean finisher) {
        float multiplier = finisher ? 1.2f : 0.6f;
        int maxCombos = finisher ? 3 : 2;
        int applied = 0;
        for (ComboEffect effect : ComboEffectRegistry.findFor(elements)) {
            if (applied >= maxCombos) {
                break;
            }
            applied++;
            float radius = effect.radius() > 0 ? effect.radius() : 3.0f;
            AABB box = new AABB(center, center).inflate(radius);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e.isAlive() && !(e instanceof Player));

            switch (effect.type()) {
                case DAMAGE -> {
                    float damage = effect.damage() * multiplier;
                    for (LivingEntity target : targets) {
                        if (player != null) {
                            target.hurt(ModDamageSources.element(player, effect.first()), damage);
                        } else {
                            target.hurt(target.damageSources().magic(), damage);
                        }
                    }
                }
                case CONTROL -> {
                    int duration = effect.duration();
                    for (LivingEntity target : targets) {
                        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 2, false, true));
                        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 1, false, true));
                    }
                }
                case SHIELD -> {
                    if (player != null) {
                        int amplifier = Math.max(0, (int) effect.intensity() - 1);
                        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, effect.duration(), amplifier, false, true));
                    }
                }
                case DIFFUSION -> {
                    for (LivingEntity target : targets) {
                        IElementalEntity.of(target).addElementState(effect.first(), 1, 100);
                        IElementalEntity.of(target).addElementState(effect.second(), 1, 100);
                    }
                }
                case AMPLIFY -> {
                    if (player != null) {
                        int amplifier = Math.max(0, (int) effect.intensity() - 1);
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, effect.duration(), amplifier, false, true));
                    }
                }
            }
        }
    }
}
