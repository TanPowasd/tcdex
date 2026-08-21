package org.tp.tcdex.resonance;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.energy.ElementEnergyManager;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 元素共鸣：附近玩家根据各自当前元素触发团队共鸣 Buff。
 */
public final class ElementalResonanceManager {

    private static final int RADIUS = 12;
    private static final int INTERVAL = 40;

    private ElementalResonanceManager() {
    }

    public static void tick(Player player) {
        if (player.level().isClientSide) {
            return;
        }
        if (player.tickCount % INTERVAL != 0) {
            return;
        }
        Map<ElementType, Integer> counts = countNearbyElements(player);
        applyResonance(player, counts);
    }

    private static Map<ElementType, Integer> countNearbyElements(Player center) {
        Map<ElementType, Integer> counts = new EnumMap<>(ElementType.class);
        Level level = center.level();
        for (Player player : level.getEntitiesOfClass(Player.class, center.getBoundingBox().inflate(RADIUS))) {
            ElementType element = ElementEnergyManager.getCurrentElement(player);
            if (element != null && element != ElementType.PRISM && element != ElementType.TIDE) {
                counts.merge(element, 1, Integer::sum);
            }
        }
        return counts;
    }

    private static void applyResonance(Player player, Map<ElementType, Integer> counts) {
        int duration = 100;
        boolean any = false;

        if (counts.getOrDefault(ElementType.SOLAR, 0) >= 2) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 0, false, true));
            any = true;
        }
        if (counts.getOrDefault(ElementType.ARC, 0) >= 2) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 0, false, true));
            any = true;
        }
        if (counts.getOrDefault(ElementType.VOID, 0) >= 2) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, true));
            any = true;
        }
        if (counts.getOrDefault(ElementType.STASIS, 0) >= 2) {
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, 0, false, true));
            any = true;
        }
        if (counts.getOrDefault(ElementType.STRAND, 0) >= 2) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 0, false, true));
            any = true;
        }
        if (counts.getOrDefault(ElementType.SINKSTAR, 0) >= 2) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 1, false, true));
            any = true;
        }
        if (counts.getOrDefault(ElementType.MISTFLOW, 0) >= 2) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 1, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, duration, 0, false, true));
            any = true;
        }

        int distinct = (int) counts.values().stream().filter(v -> v > 0).count();
        if (distinct >= 4) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 0, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 0, false, true));
            any = true;
        }

        // 简单占位，避免 unused 警告
        if (any) {
            List<ElementType> list = new ArrayList<>(counts.keySet());
            if (!list.isEmpty()) {
                // no-op
            }
        }
    }
}
