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
import org.tp.tcdex.transcendence.TranscendenceManager;

/**
 * 光能微粒：怪物死亡时有概率掉落，拾取后同时充能超越光暗能量和元素能量。
 */
@Mod.EventBusSubscriber(modid = Tcdex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LightOrbEvents {

    private static final float LIGHT_ENERGY = 5.0f;
    private static final float ELEMENT_ENERGY = 5.0f;

    @SubscribeEvent
    public static void onMonsterDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide || event.getEntity() instanceof Player) {
            return;
        }
        if (!ElementManager.isMonster(event.getEntity())) {
            return;
        }
        if (event.getEntity().level().random.nextFloat() < 0.25f) {
            ItemEntity orb = new ItemEntity(event.getEntity().level(),
                    event.getEntity().getX(), event.getEntity().getY() + 0.5, event.getEntity().getZ(),
                    new ItemStack(ModItems.LIGHT_ORB.get()));
            event.getEntity().level().addFreshEntity(orb);
        }
    }

    @SubscribeEvent
    public static void onPickup(EntityItemPickupEvent event) {
        ItemStack stack = event.getItem().getItem();
        if (stack.isEmpty() || stack.getItem() != ModItems.LIGHT_ORB.get()) {
            return;
        }
        Player player = event.getEntity();
        TranscendenceManager.gainEnergy(player, null, 0.0f, 0.0f, LIGHT_ENERGY);
        ElementEnergyManager.addEnergy(player, ELEMENT_ENERGY * ElementEnergyManager.getRechargeEfficiency(player));
        event.getItem().discard();
        event.setCanceled(true);
    }
}
