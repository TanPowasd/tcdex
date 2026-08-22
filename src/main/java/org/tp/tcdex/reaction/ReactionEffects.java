package org.tp.tcdex.reaction;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 元素反应通用视觉效果/范围效果工具。
 */
public final class ReactionEffects {

    private ReactionEffects() {
    }

    /** 在目标位置播放元素粒子 */
    public static void spawnParticles(LivingEntity target, ParticleOptions particle) {
        Level level = target.level();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(particle, target.getX(), target.getY() + 1.0, target.getZ(),
                    20, 0.4, 0.4, 0.4, 0.1);
        }
    }

    /** 把周围非玩家实体拉向中心（重力坍缩等） */
    public static void pullEntities(LivingEntity center, float radius, float intensity) {
        Level level = center.level();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                center.getBoundingBox().inflate(radius),
                e -> e != center && e.isAlive() && !(e instanceof Player))) {
            Vec3 toCenter = center.position().subtract(entity.position());
            if (toCenter.lengthSqr() < 0.0001) {
                continue;
            }
            toCenter = toCenter.normalize().scale(intensity);
            entity.setDeltaMovement(entity.getDeltaMovement().add(toCenter));
            entity.hurtMarked = true;
        }
    }
}
