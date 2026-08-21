package org.tp.tcdex.energy;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tp.tcdex.ModItems;
import org.tp.tcdex.Tcdex;
import org.tp.tcdex.element.ElementManager;
import org.tp.tcdex.element.ElementType;

/**
 * 元素能量球：元素怪物死亡时掉落，玩家拾取后按充能效率获得元素能量。
 */
@Mod.EventBusSubscriber(modid = Tcdex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EnergyOrbEvents {

    private static final float ORB_ENERGY = 5.0f;

    @SubscribeEvent
    public static void onMonsterDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide || event.getEntity() instanceof Player) {
            return;
        }
        if (!ElementManager.isMonster(event.getEntity())) {
            return;
        }
        int count = 1 + event.getEntity().level().random.nextInt(3);
        for (int i = 0; i < count; i++) {
            ItemEntity orb = new ItemEntity(event.getEntity().level(),
                    event.getEntity().getX(), event.getEntity().getY() + 0.5, event.getEntity().getZ(),
                    new ItemStack(ModItems.ELEMENT_ENERGY_ORB.get()));
            event.getEntity().level().addFreshEntity(orb);
        }
    }

    @SubscribeEvent
    public static void onPickup(EntityItemPickupEvent event) {
        ItemStack stack = event.getItem().getItem();
        if (stack.isEmpty() || stack.getItem() != ModItems.ELEMENT_ENERGY_ORB.get()) {
            return;
        }
        Player player = event.getEntity();
        float amount = ORB_ENERGY * ElementEnergyManager.getRechargeEfficiency(player);
        ElementEnergyManager.addEnergy(player, amount);
        event.getItem().discard();
        event.setCanceled(true);
    }
}
