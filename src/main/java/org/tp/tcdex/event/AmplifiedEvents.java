package org.tp.tcdex.event;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tp.tcdex.Tcdex;
import org.tp.tcdex.damage.ModDamageSources;
import org.tp.tcdex.effect.AmplifiedEffect;
import org.tp.tcdex.effect.TcdexEffects;
import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.modifier.elemental.IElementalEntity;

/**
 * 增幅效果联动（命运2 Amplified，独立 buff 非词条）：
 * <ul>
 *   <li><b>击杀获得</b>：玩家击杀带<b>电弧标记</b>的目标 → 获得/刷新增幅（10 秒）</li>
 *   <li><b>死亡电爆</b>：持有增幅时死亡 → 3 格内敌人受电弧伤害 + 电弧标记（Jolt），
 *       并播放闪电音效与电弧粒子</li>
 * </ul>
 * 移速/跳跃属性加成见 {@link AmplifiedEffect}（MobEffect 属性）。
 */
@Mod.EventBusSubscriber(modid = Tcdex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AmplifiedEvents {

    /** 击杀带电弧标记的目标 → 获得/刷新增幅 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onKill(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player killer)) {
            return;
        }
        if (IElementalEntity.of(event.getEntity()).getElementStacks(ElementType.ARC) > 0) {
            killer.addEffect(new MobEffectInstance(TcdexEffects.AMPLIFIED.get(),
                    AmplifiedEffect.DURATION, 0, false, true));
        }
    }

    /** 持有增幅死亡 → 电弧爆发（只影响非玩家实体，命运2 语义） */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Level level = entity.level();
        if (level.isClientSide) {
            return;
        }
        if (!(entity instanceof Player player) || !player.hasEffect(TcdexEffects.AMPLIFIED.get())) {
            return;
        }
        // 电弧伤害源（TCDEX 类型，元素转化递归保护自动跳过，不会二次结算）
        DamageSource source = ModDamageSources.element(player, ElementType.ARC);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(AmplifiedEffect.BURST_RADIUS),
                e -> e != player && e.isAlive() && !(e instanceof Player))) {
            target.hurt(source, AmplifiedEffect.BURST_DAMAGE);
            IElementalEntity.of(target).addElementState(ElementType.ARC, 1, 100); // Jolt 标记
        }
        // 演出：闪电音效 + 电弧粒子
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.AMBIENT, 0.6F, 1.6F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    30, 0.6, 0.6, 0.6, 0.1);
        }
    }
}
