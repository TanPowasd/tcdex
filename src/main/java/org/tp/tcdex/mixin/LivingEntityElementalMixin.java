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

    /** 元素护盾：是否已从护盾表初始化 */
    @Unique
    private boolean tcdex$shieldInit;

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
    }

    @Override
    public void clearElementState(ElementType type) {
        tcdex$elementStates.remove(type);
    }

    @Override
    public Map<ElementType, ElementStatus> getAllElementStates() {
        return tcdex$elementStates;
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
        tcdex$initShield();
        float overflow = Math.max(0.0f, damage - tcdex$shieldAmount);
        tcdex$shieldAmount = Math.max(0.0f, tcdex$shieldAmount - damage);
        return overflow;
    }

    @Override
    public void destroyShield() {
        tcdex$shieldElement = null;
        tcdex$shieldAmount = 0.0f;
        tcdex$shieldInit = true;
    }

    @Override
    public void setShield(ElementType element, float amount) {
        tcdex$shieldElement = element;
        tcdex$shieldAmount = Math.max(0.0f, amount);
        tcdex$shieldInit = true;
    }

    /**
     * 懒加载兜底：静态表覆盖 → 敌对生物随机护盾 → 其余无盾。
     * 正常情况下生成时已由 ElementalStateEvents 分配（setShield），此处仅兜底。
     */
    @Unique
    private void tcdex$initShield() {
        if (tcdex$shieldInit) {
            return;
        }
        tcdex$shieldInit = true;
        LivingEntity self = (LivingEntity) (Object) this;
        ElementType element = ElementManager.getShieldElement(self);
        if (element == null && ElementManager.isMonster(self)) {
            element = ElementManager.rollShieldElement(self.getRandom());
        }
        if (element != null) {
            tcdex$shieldElement = element;
            // 护盾量 = 最大生命 × 50%（命运2 固定比例护盾）
            tcdex$shieldAmount = self.getMaxHealth() * 0.5f;
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
                iterator.remove();
                continue;
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
