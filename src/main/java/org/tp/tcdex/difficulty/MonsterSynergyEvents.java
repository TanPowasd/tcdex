package org.tp.tcdex.difficulty;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tp.tcdex.Tcdex;
import org.tp.tcdex.element.ElementManager;
import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.modifier.elemental.IElementalEntity;

/**
 * 怪物协同：同元素怪物聚集时会获得强化 Buff。
 */
@Mod.EventBusSubscriber(modid = Tcdex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MonsterSynergyEvents {

    private static final int INTERVAL = 40;
    private static final double RADIUS = 8.0;
    private static final int SYNERGY_COUNT = 2;

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || entity instanceof Player || !ElementManager.isMonster(entity)) {
            return;
        }
        if (entity.tickCount % INTERVAL != 0) {
            return;
        }
        ElementType element = IElementalEntity.of(entity).getAttackElement();
        if (element == null) {
            return;
        }
        int count = countNearbySameElement(entity, element);
        if (count >= SYNERGY_COUNT) {
            entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, false, true));
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, false, true));
            entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, false, true));
        }
    }

    private static int countNearbySameElement(LivingEntity self, ElementType element) {
        Level level = self.level();
        int count = 0;
        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class,
                self.getBoundingBox().inflate(RADIUS),
                e -> e != self && e.isAlive() && !(e instanceof Player) && ElementManager.isMonster(e))) {
            if (IElementalEntity.of(other).getAttackElement() == element) {
                count++;
            }
        }
        return count;
    }
}
