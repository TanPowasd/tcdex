package org.tp.tcdex.chain;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.modifier.elemental.IElementalEntity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 连携领域管理：元素风暴与原命裂隙。
 */
public final class ChainAreaManager {

    /** 元素风暴持续 5 秒 */
    public static final int STORM_DURATION = 100;
    /** 元素风暴半径 */
    public static final float STORM_RADIUS = 6.0f;
    /** 原命裂隙持续 8 秒 */
    public static final int RIFT_DURATION = 160;
    /** 原命裂隙半径 */
    public static final float RIFT_RADIUS = 8.0f;
    /** 领域触发间隔 */
    public static final int TICK_INTERVAL = 10;
    /** 领域每次触发的基础伤害 */
    public static final float AREA_DAMAGE = 1.5f;

    private static final List<ActiveArea> AREAS = new ArrayList<>();

    private ChainAreaManager() {
    }

    private record ActiveArea(Vec3 center, List<ElementType> elements, int remainingTicks,
                              int tickCounter, float radius, boolean rift) {
    }

    /** 生成元素风暴 */
    public static void spawnStorm(Level level, Vec3 center, List<ElementType> elements) {
        if (level.isClientSide || elements.isEmpty()) {
            return;
        }
        AREAS.add(new ActiveArea(center, List.copyOf(elements), STORM_DURATION, 0, STORM_RADIUS, false));
        playSpawnSound(level, center, false);
    }

    /** 生成原命裂隙 */
    public static void spawnRift(Level level, Vec3 center, List<ElementType> elements) {
        if (level.isClientSide || elements.isEmpty()) {
            return;
        }
        AREAS.add(new ActiveArea(center, List.copyOf(elements), RIFT_DURATION, 0, RIFT_RADIUS, true));
        playSpawnSound(level, center, true);
    }

    /** 每 tick 更新领域 */
    public static void tick(Level level) {
        if (level.isClientSide) {
            return;
        }
        Iterator<ActiveArea> it = AREAS.iterator();
        while (it.hasNext()) {
            ActiveArea area = it.next();
            area = new ActiveArea(area.center(), area.elements(), area.remainingTicks() - 1,
                    area.tickCounter() + 1, area.radius(), area.rift());
            if (area.tickCounter() % TICK_INTERVAL == 0) {
                applyTick(level, area);
            }
            if (area.remainingTicks() <= 0) {
                it.remove();
            } else {
                // 由于 record 不可变，需要替换列表元素（简化：先删后加）
                it.remove();
                AREAS.add(area);
            }
        }
    }

    private static void applyTick(Level level, ActiveArea area) {
        AABB box = new AABB(area.center(), area.center()).inflate(area.radius());
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && !(e instanceof Player))) {
            for (ElementType element : area.elements()) {
                entity.hurt(entity.damageSources().magic(), AREA_DAMAGE);
                IElementalEntity.of(entity).addElementState(element, 1, 100);
            }
        }
        if (level instanceof ServerLevel serverLevel) {
            for (ElementType element : area.elements()) {
                serverLevel.sendParticles(element.getParticle(),
                        area.center().x, area.center().y + 0.5, area.center().z,
                        4, area.radius() * 0.25, area.radius() * 0.25, area.radius() * 0.25, 0.01);
            }
        }
    }

    private static void playSpawnSound(Level level, Vec3 center, boolean rift) {
        level.playSound(null, center.x, center.y, center.z,
                rift ? SoundEvents.DRAGON_FIREBALL_EXPLODE : SoundEvents.FIREWORK_ROCKET_LAUNCH,
                SoundSource.PLAYERS, 0.8F, 1.3F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(rift ? ParticleTypes.DRAGON_BREATH : ParticleTypes.FIREWORK,
                    center.x, center.y + 1, center.z, 40, 0.5, 0.5, 0.5, 0.1);
        }
    }
}
