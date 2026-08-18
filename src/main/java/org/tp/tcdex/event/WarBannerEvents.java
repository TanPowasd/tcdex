package org.tp.tcdex.event;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tp.tcdex.Tcdex;
import org.tp.tcdex.effect.TcdexEffects;
import org.tp.tcdex.effect.WarBannerEffect;

/**
 * 战争旗帜效果联动（命运2 Banner of War，独立 buff 非词条）：
 * <ul>
 *   <li><b>击杀扬旗</b>：玩家击杀任意敌人 → 施加/刷新战争旗帜效果（层数 +1，上限 4，时长 8 秒）</li>
 *   <li><b>盟友增伤</b>：玩家攻击生物时，按附近 8 格内（含自己）最高旗帜层数，每层伤害 +8%</li>
 *   <li><b>旗帜治疗</b>：持旗玩家每 1 秒治疗附近玩家 0.5 × 层数</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = Tcdex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WarBannerEvents {

    // ===== 击杀扬旗 =====

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onKill(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player killer)) {
            return;
        }
        // 层数 = amplifier + 1（上限 4 层）；每次击杀刷新 8 秒时长
        MobEffectInstance current = killer.getEffect(TcdexEffects.WAR_BANNER.get());
        int amplifier = current != null ? Math.min(WarBannerEffect.MAX_AMPLIFIER, current.getAmplifier() + 1) : 0;
        killer.addEffect(new MobEffectInstance(TcdexEffects.WAR_BANNER.get(),
                WarBannerEffect.DURATION, amplifier, false, true));
    }

    // ===== 盟友增伤（最终伤害，覆盖近战/远程、匠魂/原版武器） =====

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onPlayerAttack(LivingHurtEvent event) {
        if (event.isCanceled() || event.getEntity().level().isClientSide) {
            return;
        }
        // 目标为生物（旗帜增益不作用于玩家）
        if (event.getEntity() instanceof Player) {
            return;
        }
        // 攻击者：近战为玩家本人，远程为弹射物（归属玩家）
        Entity direct = event.getSource().getDirectEntity();
        Player attacker;
        if (direct instanceof Player p) {
            attacker = p;
        } else if (direct instanceof Projectile projectile && projectile.getOwner() instanceof Player p) {
            attacker = p;
        } else {
            return;
        }
        // 附近旗帜最高层数（含自己；NORMAL 优先级：元素转化 rehurt 之后统一加成，无双重结算）
        int stacks = getNearbyBannerStacks(attacker);
        if (stacks > 0) {
            event.setAmount(event.getAmount() * (1.0f + stacks * WarBannerEffect.DAMAGE_PER_STACK));
        }
    }

    // ===== 旗帜治疗 =====

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        Player holder = event.player;
        MobEffectInstance banner = holder.getEffect(TcdexEffects.WAR_BANNER.get());
        if (banner == null) {
            return;
        }
        if (holder.level().getGameTime() % 20 != 0) {
            return;
        }
        // 每 1 秒治疗附近玩家（含自己）
        float heal = WarBannerEffect.HEAL_PER_STACK * (banner.getAmplifier() + 1);
        for (Player player : holder.level().getEntitiesOfClass(Player.class, holder.getBoundingBox().inflate(WarBannerEffect.RADIUS))) {
            player.heal(heal);
        }
    }

    /** 附近 8 格内（含自己）持旗玩家的最高层数（amplifier + 1） */
    private static int getNearbyBannerStacks(Player center) {
        int best = 0;
        for (Player player : center.level().getEntitiesOfClass(Player.class, center.getBoundingBox().inflate(WarBannerEffect.RADIUS))) {
            MobEffectInstance banner = player.getEffect(TcdexEffects.WAR_BANNER.get());
            if (banner != null) {
                best = Math.max(best, banner.getAmplifier() + 1);
            }
        }
        return best;
    }
}
