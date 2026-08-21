package org.tp.tcdex.energy;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.tp.tcdex.artifact.ArtifactManager;
import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.integration.tinkers.TinkersBridgeHolder;
import org.tp.tcdex.element.ElementManager;
import org.tp.tcdex.transcendence.TranscendenceManager;

import java.util.List;

/**
 * 元素能量 / 元素充能效率管理器（原神式大招资源）。
 *
 * <p>玩家拥有单一通用能量条（0~100），通过攻击、击杀、受到元素伤害获得能量；
 * 元素充能效率决定实际获取倍率。能量满后可手动释放七元素爆发（默认按键 X）。</p>
 */
public final class ElementEnergyManager {

    public static final String ENERGY_TAG = "tcdex_element_energy";
    public static final String RECHARGE_TAG = "tcdex_element_recharge";
    public static final String LAST_ELEMENT_TAG = "tcdex_last_element";

    public static final float MAX_ENERGY = 100.0f;
    public static final float HIT_GAIN = 2.0f;
    public static final float KILL_GAIN = 8.0f;
    public static final float HURT_GAIN = 1.0f;
    public static final int BURST_DURATION = 100;

    private ElementEnergyManager() {
    }

    // ===== 能量读写 =====

    public static float getEnergy(Player player) {
        return player.getPersistentData().getFloat(ENERGY_TAG);
    }

    public static void setEnergy(Player player, float value) {
        player.getPersistentData().putFloat(ENERGY_TAG, Math.max(0.0f, Math.min(MAX_ENERGY, value)));
    }

    public static void addEnergy(Player player, float amount) {
        setEnergy(player, getEnergy(player) + amount);
    }

    // ===== 充能效率 =====

    /** 默认 1.0 = 100%，并叠加圣遗物提供的充能效率 */
    public static float getRechargeEfficiency(Player player) {
        float base = player.getPersistentData().contains(RECHARGE_TAG)
                ? player.getPersistentData().getFloat(RECHARGE_TAG)
                : 1.0f;
        return base + ArtifactManager.getTotalRechargeEfficiency(player);
    }

    public static void setRechargeEfficiency(Player player, float value) {
        player.getPersistentData().putFloat(RECHARGE_TAG, Math.max(0.1f, value));
    }

    public static void addRechargeEfficiency(Player player, float amount) {
        setRechargeEfficiency(player, getRechargeEfficiency(player) + amount);
    }

    // ===== 能量获取 =====

    public static void onPlayerAttack(Player player, ElementType element) {
        if (element != null) {
            player.getPersistentData().putString(LAST_ELEMENT_TAG, element.getId());
        }
        addEnergy(player, HIT_GAIN * getRechargeEfficiency(player));
    }

    public static void onPlayerKill(Player player, ElementType element) {
        if (element != null) {
            player.getPersistentData().putString(LAST_ELEMENT_TAG, element.getId());
        }
        addEnergy(player, KILL_GAIN * getRechargeEfficiency(player));
    }

    public static void onPlayerDamagedByElement(Player player, ElementType element) {
        if (element != null) {
            player.getPersistentData().putString(LAST_ELEMENT_TAG, element.getId());
        }
        addEnergy(player, HURT_GAIN * getRechargeEfficiency(player));
    }

    // ===== 当前爆发元素 =====

    /** 获取玩家当前手持武器对应的元素；无法判定时返回最后使用的元素 */
    public static ElementType getCurrentElement(Player player) {
        if (TinkersBridgeHolder.isAvailable()) {
            var bridge = TinkersBridgeHolder.get();
            for (ItemStack stack : List.of(player.getMainHandItem(), player.getOffhandItem())) {
                ElementType element = bridge.getWeaponElement(stack);
                if (element != null) {
                    return element;
                }
            }
        }
        return ElementManager.parseElement(player.getPersistentData().getString(LAST_ELEMENT_TAG));
    }

    // ===== 爆发 =====

    /** 尝试释放元素爆发；若超越双槽也满则释放棱镜融合爆发 */
    public static boolean tryActivateBurst(Player player, long now) {
        if (getEnergy(player) < MAX_ENERGY) {
            return false;
        }
        ElementType element = getCurrentElement(player);
        if (element == null) {
            return false;
        }
        // 超越 + 元素能量双满：棱镜融合爆发
        if (TranscendenceManager.isReady(player)) {
            TranscendenceManager.tryActivate(player, now);
            setEnergy(player, 0.0f);
            applyPrismaticBurst(player);
            return true;
        }
        setEnergy(player, 0.0f);
        applyBurst(player, element);
        return true;
    }

    /** 棱镜融合爆发：全元素 AOE + 强控制 */
    private static void applyPrismaticBurst(Player player) {
        Level level = player.level();
        float radius = 8.0f;
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                e -> e != player && e.isAlive() && !(e instanceof Player));
        for (LivingEntity target : targets) {
            target.hurt(player.damageSources().indirectMagic(player, null), 20.0f);
            target.setSecondsOnFire(3);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 4, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 2, false, true));
        }
        playBurst(player, ParticleTypes.FIREWORK, SoundEvents.DRAGON_FIREBALL_EXPLODE, 60);
    }

    /** 应用七元素爆发效果 */
    private static void applyBurst(Player player, ElementType element) {
        Level level = player.level();
        float radius = 5.0f;
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                e -> e != player && e.isAlive() && !(e instanceof Player));

        switch (element) {
            case SOLAR -> {
                for (LivingEntity target : targets) {
                    target.hurt(player.damageSources().indirectMagic(player, null), 12.0f);
                    target.setSecondsOnFire(5);
                }
                playBurst(player, ParticleTypes.FLAME, SoundEvents.GENERIC_EXPLODE, 30);
            }
            case ARC -> {
                int count = 0;
                for (LivingEntity target : targets) {
                    if (count >= 5) {
                        break;
                    }
                    target.hurt(player.damageSources().indirectMagic(player, null), 8.0f);
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2, false, true));
                    count++;
                }
                playBurst(player, ParticleTypes.ELECTRIC_SPARK, SoundEvents.LIGHTNING_BOLT_THUNDER, 30);
            }
            case VOID -> {
                for (LivingEntity target : targets) {
                    target.hurt(player.damageSources().indirectMagic(player, null), 10.0f);
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 2, false, true));
                }
                playBurst(player, ParticleTypes.SCULK_SOUL, SoundEvents.WITHER_HURT, 30);
            }
            case STASIS -> {
                for (LivingEntity target : targets) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 6, false, true));
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 3, false, true));
                    if (target instanceof net.minecraft.world.entity.Mob mob) {
                        mob.getNavigation().stop();
                    }
                }
                playBurst(player, ParticleTypes.SNOWFLAKE, SoundEvents.GLASS_BREAK, 30);
            }
            case STRAND -> {
                for (LivingEntity target : targets) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 5, false, true));
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 2, false, true));
                }
                playBurst(player, ParticleTypes.ENCHANT, SoundEvents.ENDERMAN_TELEPORT, 30);
            }
            case SINKSTAR -> {
                for (LivingEntity target : targets) {
                    Vec3 toCenter = player.position().subtract(target.position()).normalize().scale(1.2);
                    target.setDeltaMovement(target.getDeltaMovement().add(toCenter));
                    target.hurtMarked = true;
                    target.hurt(player.damageSources().indirectMagic(player, null), 8.0f);
                }
                playBurst(player, ParticleTypes.END_ROD, SoundEvents.WITHER_BREAK_BLOCK, 30);
            }
            case MISTFLOW -> {
                for (LivingEntity target : targets) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3, false, true));
                    target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0, false, true));
                }
                playBurst(player, ParticleTypes.CLOUD, SoundEvents.FIREWORK_ROCKET_LAUNCH, 30);
            }
            default -> {
                for (LivingEntity target : targets) {
                    target.hurt(player.damageSources().indirectMagic(player, null), 10.0f);
                }
                playBurst(player, ParticleTypes.FIREWORK, SoundEvents.GENERIC_EXPLODE, 30);
            }
        }
    }

    private static void playBurst(Player player, ParticleOptions particle, SoundEvent sound, int count) {
        Level level = player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(particle, player.getX(), player.getY() + 1.0, player.getZ(),
                    count, 0.5, 0.5, 0.5, 0.1);
        }
    }
}
