package org.tp.tcdex.element;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

/**
 * 命运2元素伤害类型。
 */
public enum ElementType {

    SOLAR("solar", 3.0f, 0, 0, ParticleTypes.FLAME, null, 3),
    ARC("arc", 3.0f, 60, 0, ParticleTypes.ELECTRIC_SPARK, MobEffects.MOVEMENT_SLOWDOWN, 0),
    VOID("void", 4.0f, 100, 0, ParticleTypes.SCULK_SOUL, MobEffects.WEAKNESS, 0),
    STASIS("stasis", 3.0f, 100, 1, ParticleTypes.SNOWFLAKE, MobEffects.MOVEMENT_SLOWDOWN, 0),
    STRAND("strand", 3.0f, 60, 0, ParticleTypes.ENCHANT, MobEffects.LEVITATION, 0);

    private final String id;
    private final float baseDamage;
    private final int statusDuration;
    private final int statusAmplifier;
    private final ParticleOptions particle;
    private final MobEffect effect;
    private final int fireSeconds;

    ElementType(String id, float baseDamage, int statusDuration, int statusAmplifier,
                ParticleOptions particle, MobEffect effect, int fireSeconds) {
        this.id = id;
        this.baseDamage = baseDamage;
        this.statusDuration = statusDuration;
        this.statusAmplifier = statusAmplifier;
        this.particle = particle;
        this.effect = effect;
        this.fireSeconds = fireSeconds;
    }

    public String getId() {
        return id;
    }

    public float getBaseDamage() {
        return baseDamage;
    }

    public int getStatusDuration() {
        return statusDuration;
    }

    public int getStatusAmplifier() {
        return statusAmplifier;
    }

    public ParticleOptions getParticle() {
        return particle;
    }

    public MobEffect getEffect() {
        return effect;
    }

    public int getFireSeconds() {
        return fireSeconds;
    }
}
