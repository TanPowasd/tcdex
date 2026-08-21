package org.tp.tcdex.echo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.tp.tcdex.damage.ModDamageSources;
import org.tp.tcdex.element.ElementType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 元素残响：元素攻击会在目标位置留下残响，后续攻击可引爆造成额外元素伤害。
 */
public final class ElementalEchoManager {

    private static final Map<ResourceKey<Level>, List<ElementalEcho>> ECHOES = new HashMap<>();
    private static final int DEFAULT_DURATION = 100;
    private static final double DETONATE_RADIUS = 2.0;
    private static final float ECHO_DAMAGE = 6.0f;

    private ElementalEchoManager() {
    }

    public record ElementalEcho(BlockPos pos, ElementType element, int remainingTicks) {
    }

    /** 添加一个元素残响 */
    public static void addEcho(Level level, BlockPos pos, ElementType element, int duration) {
        if (level.isClientSide || element == null) {
            return;
        }
        ECHOES.computeIfAbsent(level.dimension(), k -> new ArrayList<>())
                .add(new ElementalEcho(pos.immutable(), element, Math.max(1, duration)));
    }

    /** 每 tick 更新残响，移除过期并播放粒子 */
    public static void tick(Level level) {
        if (level.isClientSide) {
            return;
        }
        List<ElementalEcho> list = ECHOES.get(level.dimension());
        if (list == null || list.isEmpty()) {
            return;
        }
        Iterator<ElementalEcho> it = list.iterator();
        while (it.hasNext()) {
            ElementalEcho echo = it.next();
            ElementalEcho updated = new ElementalEcho(echo.pos(), echo.element(), echo.remainingTicks() - 1);
            if (updated.remainingTicks() <= 0) {
                it.remove();
            } else {
                // 用不可变替换（简化：直接改 list 不支持，重新 add 再 remove 旧元素）
                it.remove();
                list.add(updated);
                if (level.getGameTime() % 10 == 0 && level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(echo.element().getParticle(),
                            echo.pos().getX() + 0.5, echo.pos().getY() + 0.5, echo.pos().getZ() + 0.5,
                            2, 0.2, 0.2, 0.2, 0.01);
                }
            }
        }
    }

    /** 引爆目标附近的元素残响，返回引爆数量 */
    public static int detonateNear(Level level, BlockPos pos, LivingEntity attacker) {
        if (level.isClientSide) {
            return 0;
        }
        List<ElementalEcho> list = ECHOES.get(level.dimension());
        if (list == null || list.isEmpty()) {
            return 0;
        }
        List<ElementalEcho> detonated = new ArrayList<>();
        for (ElementalEcho echo : list) {
            double dist = echo.pos().distSqr(pos);
            if (dist <= DETONATE_RADIUS * DETONATE_RADIUS) {
                detonated.add(echo);
            }
        }
        if (detonated.isEmpty()) {
            return 0;
        }
        list.removeAll(detonated);
        for (ElementalEcho echo : detonated) {
            explodeEcho(level, echo, attacker);
        }
        return detonated.size();
    }

    private static void explodeEcho(Level level, ElementalEcho echo, LivingEntity attacker) {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(echo.element().getParticle(),
                    echo.pos().getX() + 0.5, echo.pos().getY() + 0.5, echo.pos().getZ() + 0.5,
                    20, 0.5, 0.5, 0.5, 0.1);
        }
        level.playSound(null, echo.pos().getX(), echo.pos().getY(), echo.pos().getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.7F, 1.2F);

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(echo.pos()).inflate(2.0),
                e -> e != attacker && e.isAlive() && !(e instanceof Player))) {
            target.hurt(ModDamageSources.element(attacker, echo.element()), ECHO_DAMAGE);
        }
    }
}
