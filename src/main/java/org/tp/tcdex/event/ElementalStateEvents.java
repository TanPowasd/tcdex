package org.tp.tcdex.event;

import com.mojang.logging.LogUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.tp.tcdex.Tcdex;
import org.tp.tcdex.debug.TcdexDebug;
import org.tp.tcdex.element.ElementManager;
import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.modifier.elemental.IElementalEntity;

import java.util.Locale;

/**
 * 元素状态受击联动（命运2 关键词结算）。
 *
 * <ul>
 *   <li>缚丝 STRAND：攻击者带缚丝标记 → 造成的伤害 -40%（Sever）</li>
 *   <li>冰影 STASIS：目标冻结中（层数满 100）受击 → 伤害 +50%（Shatter）</li>
 *   <li>虚空 VOID：目标受击 → Volatile 爆炸（10% 最大生命，AOE，吃元素抗性），目标自身吃一半</li>
 *   <li>电弧 ARC：目标受击 → Jolt 连锁闪电（2 格内最多 2 个目标，各 3.0 伤害）</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = Tcdex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ElementalStateEvents {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 缚丝 Sever：敌人带缚丝标记时造成的伤害 -40% */
    private static final float SEVER_DAMAGE_MULTIPLIER = 0.6f;
    /** 虚空 Volatile：爆炸 = 目标最大生命 × 10% */
    private static final float VOLATILE_HEALTH_PERCENT = 0.1f;
    /** 虚空爆炸半径 */
    private static final float VOLATILE_RADIUS = 1.5f;
    /** 电弧 Jolt：连锁伤害 */
    private static final float JOLT_DAMAGE = 3.0f;
    /** 电弧连锁半径 */
    private static final float JOLT_RADIUS = 2.0f;
    /** 电弧连锁目标上限 */
    private static final int JOLT_TARGETS = 2;
    /** 冰影 Shatter：冻结中受击增伤 */
    private static final float SHATTER_MULTIPLIER = 1.5f;
    /** 虚空 Weaken：带虚空标记的目标受到的伤害 +15% */
    private static final float WEAKEN_MULTIPLIER = 1.15f;
    /** 电弧 Blind：Jolt 连锁命中目标致盲时长（tick） */
    private static final int BLIND_DURATION = 60;
    /** 护盾量占最大生命的比例 */
    private static final float SHIELD_HEALTH_PERCENT = 0.5f;

    /**
     * 怪物生成时分配元素护盾。
     * 分配链：黑名单（绝对无盾）→ 附属 mod 提供器 → 静态表指定 → 加权随机。
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity living) || !ElementManager.isMonster(living)) {
            return;
        }
        ElementType element = null;
        if (!ElementManager.isShieldBlacklisted(living)) {
            element = ElementManager.getProviderShieldElement(living);
            if (element == null) {
                element = ElementManager.getShieldElement(living);
                if (element == null) {
                    element = ElementManager.rollShieldElement(living.getRandom());
                }
            }
        }
        if (element != null) {
            IElementalEntity.of(living).setShield(element, living.getMaxHealth() * SHIELD_HEALTH_PERCENT);
            if (TcdexDebug.isElementalEnabled()) {
                LOGGER.info("[元素调试] {} 生成, 分配护盾: {} ({}点)", living.getType().getDescription().getString(),
                        element.getId(), living.getMaxHealth() * SHIELD_HEALTH_PERCENT);
            }
        }
    }

    /** 调试输出：发送给攻击者（玩家），同时写服务端日志 */
    private static void debugChat(Entity source, String message) {
        if (!TcdexDebug.isElementalEnabled()) {
            return;
        }
        LOGGER.info(message);
        if (source instanceof Player player) {
            player.sendSystemMessage(Component.literal(message));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        Level level = target.level();
        if (level.isClientSide) {
            return;
        }

        IElementalEntity targetData = IElementalEntity.of(target);

        // 缚丝 Sever：攻击者带缚丝标记 → 造成伤害 -40%
        Entity sourceEntity = event.getSource().getEntity();
        if (sourceEntity instanceof LivingEntity attacker) {
            if (IElementalEntity.of(attacker).getElementStacks(ElementType.STRAND) > 0) {
                event.setAmount(event.getAmount() * SEVER_DAMAGE_MULTIPLIER);
                debugChat(sourceEntity, String.format(Locale.ROOT,
                        "[元素调试] Sever: %s 攻击带缚丝标记, 伤害 x%.2f",
                        target.getDisplayName().getString(), SEVER_DAMAGE_MULTIPLIER));
            }
        }

        // 冰影 Shatter：冻结中（层数满 100）受击 +50%
        if (targetData.getElementStacks(ElementType.STASIS) >= 100) {
            event.setAmount(event.getAmount() * SHATTER_MULTIPLIER);
            debugChat(sourceEntity, String.format(Locale.ROOT,
                    "[元素调试] Shatter: %s 冻结中受击, 伤害 x%.2f",
                    target.getDisplayName().getString(), SHATTER_MULTIPLIER));
        }

        // 虚空 Weaken：带虚空标记的目标受到的伤害 +15%（标记被 Volatile 消耗前生效）
        if (targetData.getElementStacks(ElementType.VOID) > 0) {
            event.setAmount(event.getAmount() * WEAKEN_MULTIPLIER);
            debugChat(sourceEntity, String.format(Locale.ROOT,
                    "[元素调试] Weaken: %s 带虚空标记, 伤害 x%.2f",
                    target.getDisplayName().getString(), WEAKEN_MULTIPLIER));
        }

        // 虚空 Volatile：受击 → 爆炸（目标自身吃一半，周围吃全额）
        if (targetData.getElementStacks(ElementType.VOID) > 0) {
            targetData.clearElementState(ElementType.VOID);
            float resistance = ElementManager.getResistance(target, ElementType.VOID);
            float explosion = target.getMaxHealth() * VOLATILE_HEALTH_PERCENT * resistance;
            event.setAmount(event.getAmount() + explosion * 0.5f);
            explodeAround(target, explosion, VOLATILE_RADIUS);
            debugChat(sourceEntity, String.format(Locale.ROOT,
                    "[元素调试] Volatile: %s 护甲引爆! 爆炸 %.2f (本体 +%.2f)",
                    target.getDisplayName().getString(), explosion, explosion * 0.5f));
        }

        // 电弧 Jolt：受击 → 连锁闪电
        if (targetData.getElementStacks(ElementType.ARC) > 0) {
            targetData.clearElementState(ElementType.ARC);
            joltChain(target);
            debugChat(sourceEntity, String.format(Locale.ROOT,
                    "[元素调试] Jolt: %s 连锁闪电!",
                    target.getDisplayName().getString()));
        }
    }

    /** 目标死亡时清理元素状态（避免残留） */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide) {
            IElementalEntity.of(event.getEntity()).getAllElementStates().clear();
        }
    }

    /** Volatile 爆炸：对周围实体造成伤害（不包含自身与玩家，命运2：挥发爆炸不伤玩家） */
    private static void explodeAround(LivingEntity center, float damage, float radius) {
        Level level = center.level();
        DamageSource source = center.damageSources().indirectMagic(center, null);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, center.getBoundingBox().inflate(radius), e -> e != center && e.isAlive() && !(e instanceof Player))) {
            entity.hurt(source, damage);
        }
        level.playSound(null, center.getX(), center.getY(), center.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.6F, 1.2F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF, center.getX(), center.getY() + 0.5, center.getZ(), 10, 0.3, 0.3, 0.3, 0.02);
        }
    }

    /** Jolt 连锁闪电：2 格内最多 2 个其他实体受到电弧伤害 + 致盲（Blind，不含玩家） */
    private static void joltChain(LivingEntity center) {
        Level level = center.level();
        DamageSource source = center.damageSources().indirectMagic(center, null);
        int count = 0;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, center.getBoundingBox().inflate(JOLT_RADIUS), e -> e != center && e.isAlive() && !(e instanceof Player))) {
            if (count >= JOLT_TARGETS) {
                break;
            }
            entity.hurt(source, JOLT_DAMAGE);
            // Blind：连锁命中目标致盲 + 丢失攻击目标
            entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLIND_DURATION, 0, false, true));
            if (entity instanceof Mob mob) {
                mob.setTarget(null);
            }
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, entity.getX(), entity.getY() + 0.8, entity.getZ(), 6, 0.2, 0.2, 0.2, 0.05);
            }
            count++;
        }
    }
}
