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
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.tp.tcdex.Tcdex;
import org.tp.tcdex.api.ITinkersBridge;
import org.tp.tcdex.damage.ModDamageSources;
import org.tp.tcdex.debug.TcdexDebug;
import org.tp.tcdex.element.ElementManager;
import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.energy.ElementEnergyManager;
import org.tp.tcdex.integration.tinkers.TinkersBridgeHolder;
import org.tp.tcdex.modifier.elemental.IElementalEntity;
import org.tp.tcdex.reaction.ElementReactionEvents;
import org.tp.tcdex.shield.PrismShieldConfig;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 元素状态受击联动（命运2 关键词结算）。
 *
 * <ul>
 *   <li>缚丝 STRAND：攻击者带缚丝标记 → 造成的伤害 -40%（Sever）</li>
 *   <li>冰影 STASIS：目标冻结中（层数满 100）受击 → 伤害 +50%（Shatter）</li>
 *   <li>虚空 VOID：目标受击 → Volatile 爆炸（10% 最大生命，AOE，吃元素抗性），目标自身吃一半</li>
 *   <li>电弧 ARC：目标受击 → Jolt 连锁闪电（2 格内最多 2 个目标，各 3.0 伤害）</li>
 *   <li>棱镜 PRISM：目标受击 → Refract 折射（本击 25% 溅射周围）</li>
 * </ul>
 *
 * <p>关键词结算数值可通过 Tinkers 词条桥接的 ELEMENTAL_KEYWORD 由工具词条链式调整
 * （倍率/伤害/半径），派发对象 = 事件中持有匠魂工具的玩家
 * （攻击者为玩家用攻击者的武器；Sever 反向结算用被攻击玩家的武器）。</p>
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
    /** 怪物元素攻击施加的层数缩放（怪物命中比玩家武器保守，标记型元素保底 1 层） */
    private static final float MONSTER_STACK_SCALE = 0.4f;
    /** 棱镜 Refract：带标记目标受击时溅射本击伤害的比例 */
    private static final float REFRACT_SPLASH_RATIO = 0.25f;
    /** 棱镜折射溅射半径 */
    private static final float REFRACT_RADIUS = 2.0f;

    /** 统计周围 radius 格内数量最多的元素怪物元素；没有则返回 null */
    @Nullable
    private static ElementType getDominantNearbyElement(LivingEntity self, double radius) {
        Map<ElementType, Integer> counts = new EnumMap<>(ElementType.class);
        Level level = self.level();
        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class,
                self.getBoundingBox().inflate(radius),
                e -> e != self && e.isAlive() && !(e instanceof Player) && ElementManager.isMonster(e))) {
            ElementType element = IElementalEntity.of(other).getShieldElement();
            if (element != null && element != ElementType.PRISM && element != ElementType.TIDE) {
                counts.merge(element, 1, Integer::sum);
            }
        }
        ElementType best = null;
        int bestCount = 0;
        for (Map.Entry<ElementType, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > bestCount) {
                best = entry.getKey();
                bestCount = entry.getValue();
            }
        }
        return best;
    }

    /**
     * 怪物生成时分配元素护盾。
     * 分配链：黑名单（绝对无盾）→ 附属 mod 提供器 → 加权随机（周围 16 格最多元素权重翻倍）。
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
                // 周围 16 格内最多的元素怪物的元素，其生成权重翻倍
                ElementType dominant = getDominantNearbyElement(living, 16.0);
                element = ElementManager.rollShieldElement(living.getRandom(), dominant);
            }
        }
        if (element != null) {
            IElementalEntity.of(living).setShield(element, living.getMaxHealth() * SHIELD_HEALTH_PERCENT);
            // 元素使徒：10% 概率拥有多层护盾
            if (living.getRandom().nextFloat() < 0.10f) {
                IElementalEntity.of(living).setShieldLayers(2 + living.getRandom().nextInt(2));
            }
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

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        Level level = target.level();
        if (level.isClientSide || event.isCanceled()) {
            return;
        }

        IElementalEntity targetData = IElementalEntity.of(target);

        // 缚丝 Sever：攻击者带缚丝标记 → 造成伤害 -40%（倍率可被词条调整；
        // 反向结算：攻击者是怪物时用被攻击玩家的武器增强防御）
        Entity sourceEntity = event.getSource().getEntity();
        if (sourceEntity instanceof LivingEntity attacker) {
            if (IElementalEntity.of(attacker).getElementStacks(ElementType.STRAND) > 0) {
                float multiplier = dispatchMultiplier(findEventTool(sourceEntity, target), ElementType.STRAND, SEVER_DAMAGE_MULTIPLIER);
                event.setAmount(event.getAmount() * multiplier);
                debugChat(sourceEntity, String.format(Locale.ROOT,
                        "[元素调试] Sever: %s 攻击带缚丝标记, 伤害 x%.2f",
                        target.getDisplayName().getString(), multiplier));
            }
        }

        // 冰影 Shatter：冻结中（层数满 100）受击 +50%
        if (targetData.getElementStacks(ElementType.STASIS) >= 100) {
            float multiplier = dispatchMultiplier(findEventTool(sourceEntity, target), ElementType.STASIS, SHATTER_MULTIPLIER);
            event.setAmount(event.getAmount() * multiplier);
            debugChat(sourceEntity, String.format(Locale.ROOT,
                    "[元素调试] Shatter: %s 冻结中受击, 伤害 x%.2f",
                    target.getDisplayName().getString(), multiplier));
        }

        // 虚空 Weaken：带虚空标记的目标受到的伤害 +15%（标记被 Volatile 消耗前生效）
        if (targetData.getElementStacks(ElementType.VOID) > 0) {
            float multiplier = dispatchMultiplier(findEventTool(sourceEntity, target), ElementType.VOID, WEAKEN_MULTIPLIER);
            event.setAmount(event.getAmount() * multiplier);
            debugChat(sourceEntity, String.format(Locale.ROOT,
                    "[元素调试] Weaken: %s 带虚空标记, 伤害 x%.2f",
                    target.getDisplayName().getString(), multiplier));
        }

        // 虚空 Volatile：受击 → 爆炸（目标自身吃一半，周围吃全额）
        if (targetData.getElementStacks(ElementType.VOID) > 0) {
            targetData.clearElementState(ElementType.VOID);
            ItemStack tool = findEventTool(sourceEntity, target);
            float resistance = ElementManager.getResistance(target, ElementType.VOID);
            float percent = dispatchDamage(tool, ElementType.VOID, VOLATILE_HEALTH_PERCENT);
            float radius = dispatchRadius(tool, ElementType.VOID, VOLATILE_RADIUS);
            float explosion = target.getMaxHealth() * percent * resistance;
            event.setAmount(event.getAmount() + explosion * 0.5f);
            explodeAround(target, explosion, radius);
            debugChat(sourceEntity, String.format(Locale.ROOT,
                    "[元素调试] Volatile: %s 护甲引爆! 爆炸 %.2f (本体 +%.2f)",
                    target.getDisplayName().getString(), explosion, explosion * 0.5f));
        }

        // 电弧 Jolt：受击 → 连锁闪电
        if (targetData.getElementStacks(ElementType.ARC) > 0) {
            targetData.clearElementState(ElementType.ARC);
            ItemStack tool = findEventTool(sourceEntity, target);
            float damage = dispatchDamage(tool, ElementType.ARC, JOLT_DAMAGE);
            float radius = dispatchRadius(tool, ElementType.ARC, JOLT_RADIUS);
            joltChain(target, damage, radius);
            debugChat(sourceEntity, String.format(Locale.ROOT,
                    "[元素调试] Jolt: %s 连锁闪电! (%.1f 伤害, %.1f 格)",
                    target.getDisplayName().getString(), damage, radius));
        }

        // 棱镜 Refract：受击 → 本击部分伤害折射溅射周围（不含玩家），清除标记
        if (targetData.getElementStacks(ElementType.PRISM) > 0) {
            targetData.clearElementState(ElementType.PRISM);
            ItemStack tool = findEventTool(sourceEntity, target);
            float ratio = dispatchDamage(tool, ElementType.PRISM, REFRACT_SPLASH_RATIO);
            float radius = dispatchRadius(tool, ElementType.PRISM, REFRACT_RADIUS);
            float splash = event.getAmount() * ratio;
            if (splash > 0.5f) {
                refractAround(target, splash, radius);
            }
            debugChat(sourceEntity, String.format(Locale.ROOT,
                    "[元素调试] Refract: %s 棱镜折射! 溅射 %.2f",
                    target.getDisplayName().getString(), splash));
        }
    }

    // ===== ELEMENTAL_KEYWORD hook 派发 =====

    /**
     * 定位事件中玩家持有的匠魂工具（用于关键词 hook 派发）：
     * 攻击者为玩家（或玩家弹射物）→ 攻击者的武器；否则被攻击目标为玩家 → 其武器（Sever 反向结算）；
     * 都不是（怪物互殴等）→ null（不派发，用默认值）。
     */
    @Nullable
    private static ItemStack findEventTool(Entity sourceEntity, LivingEntity target) {
        Player player = null;
        if (sourceEntity instanceof Player p) {
            player = p;
        } else if (sourceEntity instanceof Projectile projectile && projectile.getOwner() instanceof Player p) {
            player = p;
        } else if (target instanceof Player p) {
            player = p;
        }
        return player == null ? null : heldTool(player);
    }

    /** 玩家主手/副手第一个可用匠魂工具（主手优先） */
    @Nullable
    private static ItemStack heldTool(Player player) {
        if (!TinkersBridgeHolder.isAvailable()) {
            return null;
        }
        ITinkersBridge bridge = TinkersBridgeHolder.get();
        for (ItemStack stack : List.of(player.getMainHandItem(), player.getOffhandItem())) {
            if (bridge.isUsableTinkersTool(stack)) {
                return stack;
            }
        }
        return null;
    }

    /** 关键词倍率派发：工具上所有词条链式调整 */
    private static float dispatchMultiplier(@Nullable ItemStack tool, ElementType keyword, float value) {
        if (tool == null || !TinkersBridgeHolder.isAvailable()) {
            return value;
        }
        return TinkersBridgeHolder.get().modifyKeywordMultiplier(tool, keyword, value);
    }

    /** 关键词伤害派发：工具上所有词条链式调整 */
    private static float dispatchDamage(@Nullable ItemStack tool, ElementType keyword, float value) {
        if (tool == null || !TinkersBridgeHolder.isAvailable()) {
            return value;
        }
        return TinkersBridgeHolder.get().modifyKeywordDamage(tool, keyword, value);
    }

    /** 关键词半径派发：工具上所有词条链式调整 */
    private static float dispatchRadius(@Nullable ItemStack tool, ElementType keyword, float value) {
        if (tool == null || !TinkersBridgeHolder.isAvailable()) {
            return value;
        }
        return TinkersBridgeHolder.get().modifyKeywordRadius(tool, keyword, value);
    }

    /**
     * 元素怪物：带元素护盾的怪物（攻击元素 = 护盾元素，同源）命中玩家时，
     * 按概率给玩家施加对应元素状态（灼烧/减速冻结/标记……）。
     *
     * <p>玩家元素状态由 PlayerStateSyncPacket 自动同步到 Buff HUD，
     * 关键词联动（Shatter / Volatile / Weaken / Jolt / Sever）对玩家照常生效。</p>
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMonsterAttackPlayer(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        Level level = target.level();
        if (level.isClientSide) {
            return;
        }
        if (!(target instanceof Player)) {
            return; // 只对玩家施加（元素怪物攻击目标为玩家）
        }
        if (!ElementManager.isAttackEnabled()) {
            return;
        }
        // 伤害来源：近战为怪物本人，远程为射出弹射物的怪物（箭/火球/光束）
        Entity causer = event.getSource().getEntity();
        if (!(causer instanceof LivingEntity attacker) || attacker instanceof Player) {
            return;
        }
        // 元素攻击与元素护盾同源分配，攻击元素在护盾分配时固化；
        // 黑名单生物无护盾 → 无元素攻击；护盾被打破后攻击元素保留
        if (ElementManager.isShieldBlacklisted(attacker)) {
            return;
        }
        ElementType element = IElementalEntity.of(attacker).getAttackElement();
        if (element == null) {
            return;
        }
        // 命中概率（1.0 = 每次命中必施加）
        if (ElementManager.getAttackChance() < 1.0f && level.random.nextFloat() >= ElementManager.getAttackChance()) {
            return;
        }

        // 先尝试元素反应，再施加怪物元素攻击附着
        ElementReactionEvents.tryTriggerReaction(target, element, attacker);

        // 施加元素状态：层数按怪物系数缩放（标记型元素保底 1 层），时长同玩家武器
        float stacks = Math.max(1.0f, element.getStacksPerHit() * MONSTER_STACK_SCALE);
        IElementalEntity.of(target).addElementState(element, stacks, element.getStateDuration());

        // 玩家受到元素伤害时获得少量元素能量
        ElementEnergyManager.onPlayerDamagedByElement((Player) target, element);

        // 粒子反馈
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(element.getParticle(), target.getX(), target.getY() + 1.0, target.getZ(), 12, 0.3, 0.3, 0.3, 0.1);
        }
        debugChat(target, String.format(Locale.ROOT,
                "[元素调试] 元素怪物 %s 命中 %s, 施加 %s 状态 (%.0f 层)",
                attacker.getDisplayName().getString(), target.getDisplayName().getString(),
                element.getId(), stacks));
    }

    /** 目标死亡时清理元素状态（避免残留） */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide) {
            IElementalEntity.of(event.getEntity()).getAllElementStates().clear();
        }
    }

    /**
     * 棱镜盾非玩家伤害路径（凋零 / 末影龙 Boss 专属）：
     * <ul>
     *   <li>玩家攻击被 ElementalDamageEvents.handlePrismShield 完全吸收（磨损护盾，破盾前打不到血量），
     *       本事件跳过被取消的原伤害事件，不参与玩家攻击结算</li>
     *   <li>非玩家伤害（环境/灼烧 DoT/怪物互殴）：直接伤血，按 动能 90% / 其他非棱镜 50% 减免</li>
     * </ul>
     * 任何未取消的伤害都重置棱镜盾脱战计时（脱战 10 秒后回复）。
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onPrismShieldDamage(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        Level level = target.level();
        if (level.isClientSide || event.isCanceled()) {
            return;
        }
        IElementalEntity targetData = IElementalEntity.of(target);
        if (targetData.getShieldElement() != ElementType.PRISM) {
            return;
        }

        // 任何伤害都重置棱镜盾脱战计时（含盾值 0 期间——非棱镜打穿后回复中被打也会中断回复）
        targetData.markShieldHit(level.getGameTime());

        // 盾值 0（非棱镜打穿后、回复前）：不减免，正常伤血
        if (targetData.getShieldAmount() <= 0) {
            return;
        }

        // 棱镜伤害不减免（匹配破盾结算）
        if (event.getSource().is(ModDamageSources.PRISM_DAMAGE_TYPE)) {
            return;
        }

        // 动能伤害减免 90%，其余非棱镜伤害减免 50%（配置化，见 PrismShieldConfig）
        float original = event.getAmount();
        float reduction = event.getSource().is(ModDamageSources.KINETIC_DAMAGE_TYPE)
                ? PrismShieldConfig.getKineticReduction() : PrismShieldConfig.getElementReduction();
        event.setAmount(original * reduction);
        debugChat(event.getSource().getEntity(), String.format(Locale.ROOT,
                "[元素调试] 棱镜盾: %s 减免 %.0f%% (%.2f → %.2f)",
                target.getDisplayName().getString(), (1.0f - reduction) * 100.0f,
                original, event.getAmount()));
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

    /** Jolt 连锁闪电：半径内最多 2 个其他实体受到电弧伤害 + 致盲（Blind，不含玩家） */
    private static void joltChain(LivingEntity center, float damage, float radius) {
        Level level = center.level();
        DamageSource source = center.damageSources().indirectMagic(center, null);
        int count = 0;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, center.getBoundingBox().inflate(radius), e -> e != center && e.isAlive() && !(e instanceof Player))) {
            if (count >= JOLT_TARGETS) {
                break;
            }
            entity.hurt(source, damage);
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

    /** 棱镜折射溅射：对周围生物造成本击部分伤害（不含玩家，命运2 语义） */
    private static void refractAround(LivingEntity center, float damage, float radius) {
        Level level = center.level();
        DamageSource source = center.damageSources().indirectMagic(center, null);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, center.getBoundingBox().inflate(radius), e -> e != center && e.isAlive() && !(e instanceof Player))) {
            entity.hurt(source, damage);
        }
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FIREWORK, center.getX(), center.getY() + 0.5, center.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
        }
    }
}
