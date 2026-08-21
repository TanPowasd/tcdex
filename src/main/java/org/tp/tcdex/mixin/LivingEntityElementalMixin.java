package org.tp.tcdex.mixin;

import com.mojang.logging.LogUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.tp.tcdex.debug.TcdexDebug;
import org.tp.tcdex.element.ElementManager;
import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.modifier.elemental.ElementStatus;
import org.tp.tcdex.modifier.elemental.IElementalEntity;
import org.tp.tcdex.network.MonsterAuraSyncPacket;
import org.tp.tcdex.network.MonsterShieldSyncPacket;
import org.tp.tcdex.network.PacketHandler;
import org.tp.tcdex.shield.PrismShieldConfig;
import net.minecraftforge.network.PacketDistributor;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 元素状态 Mixin：为所有 LivingEntity 注入元素状态存储，并在实体 tick 中结算状态。
 *
 * <p>状态规则（命运2 关键词简化版）：
 * <ul>
 *   <li>烈日 SOLAR：层数叠加，满 100 触发 Ignite（AOE 爆炸 + 点燃），清层</li>
 *   <li>冰影 STASIS：层数叠加，满 100 冻结（强减速 3 秒）；冻结中受击 Shatter +50%（见 ElementalStateEvents）</li>
 *   <li>虚空 VOID：标记，受击时 Volatile 爆炸（见 ElementalStateEvents）</li>
 *   <li>电弧 ARC：标记，受击时 Jolt 连锁闪电（见 ElementalStateEvents）</li>
 *   <li>缚丝 STRAND：标记，攻击者带此标记时伤害 -40%（见 ElementalStateEvents）</li>
 * </ul>
 * 状态为运行时数据（随实体消失），不写入存档。</p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityElementalMixin implements IElementalEntity {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Unique
    private final Map<ElementType, ElementStatus> tcdex$elementStates = new EnumMap<>(ElementType.class);

    /** 灼烧 DoT 计时器：每 10 tick 结算一跳（0.5 秒） */
    @Unique
    private int tcdex$scorchCounter;

    /** 元素护盾：护盾元素（null = 无护盾） */
    @Unique
    private ElementType tcdex$shieldElement;

    /** 元素护盾：剩余护盾值 */
    @Unique
    private float tcdex$shieldAmount;

    /** 元素攻击元素：护盾分配时固化（与护盾同源），护盾被打破后保留（元素攻击不随护盾消失） */
    @Unique
    private ElementType tcdex$attackElement;

    /** 元素护盾：是否已从护盾表初始化 */
    @Unique
    private boolean tcdex$shieldInit;

    /** 棱镜盾：最近一次受击 gameTime（脱战回复计时） */
    @Unique
    private long tcdex$lastShieldHitTime;

    /** 棱镜盾：回复计时器（每回复周期一跳） */
    @Unique
    private int tcdex$shieldRegenCounter;

    // ===== IElementalEntity 实现 =====

    @Override
    public float getElementStacks(ElementType type) {
        ElementStatus status = tcdex$elementStates.get(type);
        return status == null ? 0 : status.stacks;
    }

    @Override
    public int getElementDuration(ElementType type) {
        ElementStatus status = tcdex$elementStates.get(type);
        return status == null ? 0 : status.duration;
    }

    @Override
    public void addElementState(ElementType type, float stacks, int duration) {
        ElementStatus status = tcdex$elementStates.computeIfAbsent(type, t -> new ElementStatus(0, 0));
        status.stacks = Math.min(ElementStatus.MAX_STACKS, status.stacks + stacks);
        status.duration = Math.max(status.duration, duration);
        // 元素反应附着量：每次施加元素时按元素基础附着量累加
        status.aura += Math.max(0.0f, type.getAuraPerHit());
        tcdex$broadcastAura();
    }

    @Override
    public void clearElementState(ElementType type) {
        tcdex$broadcastAuraClear(type);
        tcdex$elementStates.remove(type);
    }

    @Override
    public Map<ElementType, ElementStatus> getAllElementStates() {
        return tcdex$elementStates;
    }

    // ===== 元素附着量实现（用于 TCDEX 元素反应） =====

    @Override
    public float getAura(ElementType type) {
        ElementStatus status = tcdex$elementStates.get(type);
        return status == null ? 0 : status.aura;
    }

    @Override
    public void addAuraAmount(ElementType type, float amount) {
        if (amount <= 0) {
            return;
        }
        ElementStatus status = tcdex$elementStates.computeIfAbsent(type, t -> new ElementStatus(0, 0));
        status.aura += amount;
        tcdex$broadcastAura();
    }

    @Override
    public void addAura(ElementType type, float amount, int duration) {
        if (amount <= 0) {
            return;
        }
        ElementStatus status = tcdex$elementStates.computeIfAbsent(type, t -> new ElementStatus(0, 0));
        status.aura += amount;
        status.duration = Math.max(status.duration, duration);
        tcdex$broadcastAura();
    }

    @Override
    public float consumeAura(ElementType type, float amount) {
        ElementStatus status = tcdex$elementStates.get(type);
        if (status == null || status.aura <= 0 || amount <= 0) {
            return 0;
        }
        float consumed = Math.min(status.aura, amount);
        status.aura -= consumed;
        if (status.aura <= 0) {
            tcdex$broadcastAuraClear(type);
        } else {
            tcdex$broadcastAura();
        }
        // 附着量归零时保留 keyword stacks/duration，仅清除“附着”本身
        return consumed;
    }

    @Override
    public long getLastReactionTime(ElementType type) {
        ElementStatus status = tcdex$elementStates.get(type);
        return status == null ? 0 : status.lastReactionTime;
    }

    @Override
    public void markReaction(ElementType type, long gameTime) {
        ElementStatus status = tcdex$elementStates.get(type);
        if (status != null) {
            status.lastReactionTime = gameTime;
        }
    }

    // ===== 元素护盾实现（懒加载：首次访问时查 ElementManager 护盾表） =====

    @Override
    public ElementType getShieldElement() {
        tcdex$initShield();
        return tcdex$shieldElement;
    }

    @Override
    public float getShieldAmount() {
        tcdex$initShield();
        return tcdex$shieldAmount;
    }

    @Override
    public float consumeShield(float damage) {
        return consumeShield(damage, true);
    }

    @Override
    public float consumeShield(float damage, boolean permanent) {
        tcdex$initShield();
        float overflow = Math.max(0.0f, damage - tcdex$shieldAmount);
        tcdex$shieldAmount = Math.max(0.0f, tcdex$shieldAmount - damage);
        // 护盾完全耗尽（完全破坏）：
        // - permanent（棱镜伤害打穿）：清除护盾元素 → 永久失效，不再回复
        // - 非永久（非棱镜伤害打穿）：保留护盾元素 → 脱战回复可重新长满
        // 元素攻击不受影响（攻击元素固化于分配时，独立于护盾状态）
        if (tcdex$shieldAmount <= 0 && tcdex$shieldElement != null && permanent) {
            tcdex$shieldElement = null;
        }
        tcdex$broadcastShield();
        return overflow;
    }

    @Override
    public void destroyShield() {
        tcdex$shieldElement = null;
        tcdex$shieldAmount = 0.0f;
        tcdex$shieldInit = true;
        tcdex$broadcastShield();
        // 注意：攻击元素保留（固化于分配时，不随护盾销毁而消失）
    }

    @Override
    public void setShield(ElementType element, float amount) {
        tcdex$shieldElement = element;
        tcdex$attackElement = element; // 攻击元素与护盾同源，分配时固化
        tcdex$shieldAmount = Math.max(0.0f, amount);
        tcdex$shieldInit = true;
        tcdex$broadcastShield();
    }

    @Override
    public ElementType getAttackElement() {
        tcdex$initShield(); // 保证攻击元素已随护盾初始化（与护盾同源分配）
        return tcdex$attackElement;
    }

    @Override
    public void markShieldHit(long gameTime) {
        tcdex$lastShieldHitTime = gameTime;
    }

    @Override
    public long getShieldLastHurtTime() {
        return tcdex$lastShieldHitTime;
    }

    /** 护盾变化广播给追踪该实体的玩家（客户端 HUD 缓存显示） */
    @Unique
    private void tcdex$broadcastShield() {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide) {
            return;
        }
        PacketHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> self),
                new MonsterShieldSyncPacket(self.getId(),
                        (byte) (tcdex$shieldElement != null ? tcdex$shieldElement.ordinal() + 1 : 0),
                        tcdex$shieldAmount));
    }

    /** 元素附着变化广播给追踪该实体的玩家（客户端元素附着 HUD） */
    @Unique
    private void tcdex$broadcastAura() {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide) {
            return;
        }
        for (Map.Entry<ElementType, ElementStatus> entry : tcdex$elementStates.entrySet()) {
            if (entry.getValue().aura > 0) {
                PacketHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> self),
                        new MonsterAuraSyncPacket(self.getId(),
                                (byte) (entry.getKey().ordinal() + 1),
                                entry.getValue().aura));
            }
        }
    }

    /** 清除指定元素的附着显示 */
    @Unique
    private void tcdex$broadcastAuraClear(ElementType type) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide) {
            return;
        }
        PacketHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> self),
                new MonsterAuraSyncPacket(self.getId(), (byte) (type.ordinal() + 1), 0.0f));
    }

    /**
     * 懒加载兜底：敌对生物加权随机护盾，其余无盾。
     * 正常情况下生成时已由 ElementalStateEvents 分配（setShield），此处仅兜底。
     */
    @Unique
    private void tcdex$initShield() {
        if (tcdex$shieldInit) {
            return;
        }
        tcdex$shieldInit = true;
        LivingEntity self = (LivingEntity) (Object) this;
        // 黑名单生物绝对无盾（含懒加载路径：防止被攻击/查询时兜底分配护盾）
        if (ElementManager.isShieldBlacklisted(self)) {
            return;
        }
        ElementType element = ElementManager.isMonster(self)
                ? ElementManager.rollShieldElement(self.getRandom())
                : null;
        if (element != null) {
            tcdex$shieldElement = element;
            tcdex$attackElement = element; // 攻击元素同源固化
            // 护盾量 = 最大生命 × 50%（命运2 固定比例护盾）
            tcdex$shieldAmount = self.getMaxHealth() * 0.5f;
            tcdex$broadcastShield();
        }
    }

    // ===== tick 结算 =====

    @Inject(method = "tick", at = @At("HEAD"))
    private void tcdex$elementTick(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        Level level = self.level();
        if (level.isClientSide) {
            return;
        }

        Iterator<Map.Entry<ElementType, ElementStatus>> iterator = tcdex$elementStates.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ElementType, ElementStatus> entry = iterator.next();
            ElementType type = entry.getKey();
            ElementStatus status = entry.getValue();

            // 计时衰减
            status.duration--;
            if (status.duration <= 0) {
                tcdex$broadcastAuraClear(type);
                iterator.remove();
                continue;
            }

            // 元素附着量衰减（用于元素反应）
            if (status.aura > 0) {
                float oldAura = status.aura;
                status.aura = Math.max(0.0f, status.aura - ElementManager.getAuraDecayPerTick());
                if (oldAura > 0 && status.aura <= 0) {
                    tcdex$broadcastAuraClear(type);
                }
            }

            // 烈日：满 100 层 → Ignite 引爆
            if (type == ElementType.SOLAR && status.stacks >= ElementStatus.MAX_STACKS) {
                tcdex$ignite(self, type);
                iterator.remove();
                if (TcdexDebug.isElementalEnabled()) {
                    LOGGER.info("[元素调试] {} Ignite 引爆!", self.getDisplayName().getString());
                }
                continue;
            }

            // 烈日：灼烧 DoT（每 10 tick 一跳 = 0.5s；每跳伤害 = 层数 × 系数 × 10，保持 DPS）
            // 用 scorch 伤害源（bypasses_invulnerability tag）：无视无敌帧稳定结算，
            // 结算后释放无敌帧，避免吞掉玩家的攻击
            if (type == ElementType.SOLAR && type.getDoTPerStack() > 0) {
                tcdex$scorchCounter++;
                if (tcdex$scorchCounter >= 10) {
                    tcdex$scorchCounter = 0;
                    float resistance = ElementManager.getResistance(self, type);
                    float dot = status.stacks * type.getDoTPerStack() * 10f;
                    if (dot > 0.01f) {
                        self.hurt(org.tp.tcdex.damage.ModDamageSources.scorch(self), dot);
                        self.invulnerableTime = 0; // 释放无敌帧，玩家攻击不受影响
                    }
                }
            }

            // 冰影：渐进减速（≥50 缓慢 I / ≥75 缓慢 II），满 100 → 冻结（强减速 3 秒）
            if (type == ElementType.STASIS) {
                if (status.stacks >= ElementStatus.MAX_STACKS) {
                    MobEffectInstance current = self.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
                    if (current == null || current.getAmplifier() != 250) {
                        self.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 250, false, true));
                        if (TcdexDebug.isElementalEnabled()) {
                            LOGGER.info("[元素调试] {} 冻结!", self.getDisplayName().getString());
                        }
                    }
                } else if (status.stacks >= 50) {
                    self.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, status.stacks >= 75 ? 1 : 0, false, true));
                }
            }

            // 缚丝：满 100 层 → 悬挂（Suspend）
            // 定身不升空：锁位移 + 停止寻路 + 强减速，目标停留在原高度，
            // 玩家近战/远程均可正常攻击（命运2 Suspend 语义）。
            // 不清标记——直到状态 duration 到期自然解除（期间 Sever 持续生效）
            if (type == ElementType.STRAND && status.stacks >= ElementStatus.MAX_STACKS) {
                self.setDeltaMovement(0, 0, 0);
                self.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 5, false, true));
                if (self instanceof net.minecraft.world.entity.Mob mob) {
                    mob.getNavigation().stop();
                }
                if (level instanceof ServerLevel serverLevel && level.getGameTime() % 20 == 0) {
                    serverLevel.sendParticles(type.getParticle(), self.getX(), self.getY() + 0.5, self.getZ(), 4, 0.3, 0.3, 0.3, 0.02);
                }
                if (TcdexDebug.isElementalEnabled()) {
                    LOGGER.info("[元素调试] {} 悬挂!", self.getDisplayName().getString());
                }
            }
        }

        // ===== 棱镜盾：脱战回复（参数配置化，见 PrismShieldConfig） =====
        // 受击（markShieldHit）后脱战延迟未受伤 → 每回复周期回 10% 最大护盾值（Boss 棱镜盾专属）。
        // 棱镜伤害打穿（permanent）会清除元素 → 永久不再回复；
        // 非棱镜伤害打穿会保留元素 → 护盾从 0 开始重新回复（长满）。
        if (tcdex$shieldElement == ElementType.PRISM) {
            float maxShield = self.getMaxHealth() * 0.5f;
            if (tcdex$shieldAmount < maxShield
                    && level.getGameTime() - tcdex$lastShieldHitTime >= PrismShieldConfig.getRegenDelayTicks()) {
                tcdex$shieldRegenCounter++;
                if (tcdex$shieldRegenCounter >= PrismShieldConfig.getRegenCycle()) {
                    tcdex$shieldRegenCounter = 0;
                    tcdex$shieldAmount = Math.min(maxShield, tcdex$shieldAmount + maxShield * PrismShieldConfig.getRegenPercent());
                    tcdex$broadcastShield();
                }
            } else {
                tcdex$shieldRegenCounter = 0;
            }
        }
    }

    /** Ignite：以实体为中心 AOE 爆炸（基础 100 × 元素抗性），波及点燃。 */
    @Unique
    private static void tcdex$ignite(LivingEntity self, ElementType type) {
        Level level = self.level();
        float resistance = ElementManager.getResistance(self, type);
        float damage = 100.0f * resistance;

        DamageSource source = self.damageSources().indirectMagic(self, null);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, self.getBoundingBox().inflate(4.0), e -> e != self && e.isAlive() && !(e instanceof net.minecraft.world.entity.player.Player))) {
            entity.hurt(source, damage);
            entity.setSecondsOnFire(3);
        }

        // 演出：爆炸音效 + 粒子
        level.playSound(null, self.getX(), self.getY(), self.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.0F, 1.0F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, self.getX(), self.getY() + 0.5, self.getZ(), 1, 0, 0, 0, 0);
        }
    }
}
