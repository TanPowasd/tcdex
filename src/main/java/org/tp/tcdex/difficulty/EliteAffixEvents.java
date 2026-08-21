package org.tp.tcdex.difficulty;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tp.tcdex.Tcdex;
import org.tp.tcdex.element.ElementManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 精英怪词缀：怪物生成时随机获得词缀，并驱动词缀效果。
 */
@Mod.EventBusSubscriber(modid = Tcdex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EliteAffixEvents {

    private static final String AFFIX_TAG = "tcdex_elite_affixes";
    private static final float ELITE_CHANCE = 0.15f;

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        if (!ElementManager.isMonster(living) || living instanceof Player) {
            return;
        }
        if (living.getRandom().nextFloat() >= ELITE_CHANCE) {
            return;
        }
        List<String> affixes = new ArrayList<>();
        EliteAffix[] values = EliteAffix.values();
        int count = 1 + living.getRandom().nextInt(2);
        for (int i = 0; i < count; i++) {
            EliteAffix affix = values[living.getRandom().nextInt(values.length)];
            if (!affixes.contains(affix.getId())) {
                affixes.add(affix.getId());
                affix.onSpawn(living);
            }
        }
        living.getPersistentData().putString(AFFIX_TAG, String.join(",", affixes));
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) {
            return;
        }
        for (EliteAffix affix : getAffixes(entity)) {
            affix.onTick(entity);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        for (EliteAffix affix : getAffixes(event.getEntity())) {
            affix.onDeath(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            for (EliteAffix affix : getAffixes(attacker)) {
                affix.onDealDamage(attacker, event.getAmount());
            }
        }
    }

    private static List<EliteAffix> getAffixes(LivingEntity entity) {
        List<EliteAffix> result = new ArrayList<>();
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(AFFIX_TAG)) {
            return result;
        }
        String raw = data.getString(AFFIX_TAG);
        if (raw.isEmpty()) {
            return result;
        }
        for (String id : raw.split(",")) {
            EliteAffix affix = EliteAffix.fromId(id.trim());
            if (affix != null) {
                result.add(affix);
            }
        }
        return result;
    }
}
