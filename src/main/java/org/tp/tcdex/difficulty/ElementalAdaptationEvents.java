package org.tp.tcdex.difficulty;

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
 * 元素适应：怪物受到某种元素伤害后逐渐获得对该元素的抗性。
 */
@Mod.EventBusSubscriber(modid = Tcdex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ElementalAdaptationEvents {

    private static final float ADAPTATION_PER_HIT = 0.02f;

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide || target instanceof Player) {
            return;
        }
        ElementType element = ModDamageSources.getElement(event.getSource());
        if (element == null) {
            return;
        }
        IElementalEntity.of(target).addElementAdaptation(element, ADAPTATION_PER_HIT);
    }
}
