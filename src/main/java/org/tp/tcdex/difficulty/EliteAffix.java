package org.tp.tcdex.difficulty;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;

import java.util.Locale;

/**
 * 精英怪词缀。
 */
public enum EliteAffix {
    BARRIER("barrier"),
    REGENERATING("regenerating"),
    LIFESTEAL("lifesteal"),
    OVERLOAD("overload"),
    UNSTOPPABLE("unstoppable");

    private final String id;

    EliteAffix(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static EliteAffix fromId(String id) {
        for (EliteAffix affix : values()) {
            if (affix.id.equals(id)) {
                return affix;
            }
        }
        return null;
    }

    public void onSpawn(LivingEntity entity) {
        switch (this) {
            case BARRIER -> entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100000, 1, false, true));
            case UNSTOPPABLE -> entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100000, 0, false, true));
            default -> {
            }
        }
    }

    public void onTick(LivingEntity entity) {
        switch (this) {
            case BARRIER -> {
                if (entity.tickCount % 200 == 0 && entity.getEffect(MobEffects.ABSORPTION) == null) {
                    entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100000, 1, false, true));
                }
            }
            case REGENERATING -> {
                if (entity.tickCount % 40 == 0 && entity.getHealth() < entity.getMaxHealth()) {
                    entity.heal(0.5f);
                }
            }
            default -> {
            }
        }
    }

    public void onDeath(LivingEntity entity) {
        if (this == OVERLOAD) {
            Level level = entity.level();
            level.explode(entity, entity.getX(), entity.getY(), entity.getZ(), 2.0f, Level.ExplosionInteraction.NONE);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.0F, 1.0F);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION, entity.getX(), entity.getY() + 0.5, entity.getZ(), 20, 0.5, 0.5, 0.5, 0.05);
            }
        }
    }

    public void onDealDamage(LivingEntity attacker, float amount) {
        if (this == LIFESTEAL && amount > 0) {
            attacker.heal(Math.min(1.0f, amount * 0.05f));
        }
    }
}
