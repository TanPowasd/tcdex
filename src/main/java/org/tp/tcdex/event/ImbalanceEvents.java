package org.tp.tcdex.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tp.tcdex.Tcdex;
import org.tp.tcdex.damage.ModDamageSources;
import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.modifier.elemental.IElementalEntity;

/**
 * 破绽/失衡战斗机制：
 * - 怪物受击时累积失衡值
 * - 失衡满进入“破绽”状态：停止行动 + 受到伤害增加 50%
 */
@Mod.EventBusSubscriber(modid = Tcdex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ImbalanceEvents {

    private static final float ELEMENT_IMBALANCE = 15.0f;
    private static final float KINETIC_IMBALANCE = 10.0f;
    private static final float BREAK_DAMAGE_MULTIPLIER = 1.5f;

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide || target instanceof Player) {
            return;
        }
        IElementalEntity data = IElementalEntity.of(target);

        // 破绽期间：受到伤害增加 50%，且不再累积失衡
        if (data.isBroken()) {
            event.setAmount(event.getAmount() * BREAK_DAMAGE_MULTIPLIER);
            return;
        }

        Entity sourceEntity = event.getSource().getEntity();
        if (sourceEntity == null) {
            return;
        }
        ElementType element = ModDamageSources.getElement(event.getSource());
        data.addImbalance(element != null ? ELEMENT_IMBALANCE : KINETIC_IMBALANCE);
    }
}
