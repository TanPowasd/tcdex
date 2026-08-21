package org.tp.tcdex.reaction;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tp.tcdex.Tcdex;
import org.tp.tcdex.damage.ModDamageSources;
import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.modifier.elemental.ElementStatus;
import org.tp.tcdex.modifier.elemental.IElementalEntity;
import org.tp.tcdex.modifier.hook.TcdexHooks;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * TCDEX 元素反应触发与结算。
 *
 * <p>标准版元素附着量模型：目标身上已有元素 A 的附着量，本次攻击/法术使用元素 B，
 * 若存在 A+B 反应且附着量足够、冷却结束，则消耗 A 的附着量并触发反应。
 * 与命运2 关键词共存，不替代现有 ElementalStateEvents。</p>
 *
 * <p>触发来源：TCDEX 元素伤害事件自动触发；铁魔法等软联动在 CompatEvents 中显式调用
 * {@link #tryTriggerReaction}；怪物元素攻击在 ElementalStateEvents 中显式调用。</p>
 */
@Mod.EventBusSubscriber(modid = Tcdex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ElementReactionEvents {

    /** 岚流扩散半径 */
    private static final float DIFFUSION_RADIUS = 3.0f;

    /** 元素反应总开关（配置 elementReactionsEnabled） */
    private static boolean enabled = true;

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    private ElementReactionEvents() {
    }

    /** TCDEX 元素伤害命中时自动尝试触发反应 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!enabled || event.getEntity().level().isClientSide || event.isCanceled()) {
            return;
        }
        ElementType trigger = ModDamageSources.getElement(event.getSource());
        if (trigger == null) {
            return;
        }
        Entity sourceEntity = event.getSource().getEntity();
        LivingEntity source = sourceEntity instanceof LivingEntity living ? living : null;
        tryTriggerReaction(event.getEntity(), trigger, source);
    }

    /**
     * 尝试触发一次元素反应。
     *
     * @param target  反应目标（身上已有元素附着）
     * @param trigger 本次触发元素
     * @param source  攻击者/施法者，可为 null
     */
    public static void tryTriggerReaction(LivingEntity target, ElementType trigger, @Nullable LivingEntity source) {
        Level level = target.level();
        if (!enabled || level.isClientSide || trigger == null || trigger == ElementType.PRISM) {
            return;
        }
        IElementalEntity data = IElementalEntity.of(target);
        long now = level.getGameTime();

        // 岚流扩散：岚流触发任意已有元素附着，把该元素扩散给周围敌人
        if (trigger == ElementType.MISTFLOW) {
            for (Map.Entry<ElementType, ElementStatus> entry : data.getAllElementStates().entrySet()) {
                ElementType aura = entry.getKey();
                ElementStatus status = entry.getValue();
                if (aura == ElementType.PRISM || aura == ElementType.MISTFLOW || status.aura <= 0) {
                    continue;
                }
                if (now - status.lastReactionTime < 40) {
                    continue;
                }
                float consumed = data.consumeAura(aura, 1.0f);
                if (consumed <= 0) {
                    continue;
                }
                data.markReaction(aura, now);
                data.markReaction(ElementType.MISTFLOW, now);
                diffuse(target, aura);
                return;
            }
            return;
        }

        // 普通反应：查找 目标已有元素 aura + 本次触发元素 trigger
        for (Map.Entry<ElementType, ElementStatus> entry : data.getAllElementStates().entrySet()) {
            ElementType aura = entry.getKey();
            ElementStatus status = entry.getValue();
            if (aura == trigger || aura == ElementType.PRISM || trigger == ElementType.PRISM || status.aura <= 0) {
                continue;
            }
            ElementReaction reaction = ElementReactionRegistry.find(aura, trigger);
            if (reaction == null) {
                continue;
            }
            // 允许词条通过 REACTION hook 调整冷却/持续时间/范围/强度
            reaction = adjustReaction(source, reaction);
            if (now - status.lastReactionTime < reaction.getCooldownTicks()) {
                continue;
            }
            float consumed = data.consumeAura(aura, reaction.getAuraCost());
            if (consumed <= 0) {
                continue;
            }
            data.markReaction(aura, now);
            data.markReaction(trigger, now);
            applyReaction(target, reaction, source);
            return;
        }
    }

    /** 通过攻击者手持匠魂工具上的 REACTION hook 调整反应参数 */
    private static ElementReaction adjustReaction(@Nullable LivingEntity source, ElementReaction reaction) {
        if (!(source instanceof Player player)) {
            return reaction;
        }
        float duration = reaction.getDuration();
        float radius = reaction.getRadius();
        float intensity = reaction.getIntensity();
        int cooldown = reaction.getCooldownTicks();
        for (ItemStack stack : List.of(player.getMainHandItem(), player.getOffhandItem())) {
            if (stack.isEmpty() || !(stack.getItem() instanceof IModifiable)) {
                continue;
            }
            ToolStack tool = ToolStack.from(stack);
            if (tool.isBroken()) {
                continue;
            }
            for (ModifierEntry entry : tool.getModifierList()) {
                duration = entry.getHook(TcdexHooks.REACTION)
                        .modifyReactionDuration(tool, entry, reaction, duration);
                radius = entry.getHook(TcdexHooks.REACTION)
                        .modifyReactionRadius(tool, entry, reaction, radius);
                intensity = entry.getHook(TcdexHooks.REACTION)
                        .modifyReactionIntensity(tool, entry, reaction, intensity);
                cooldown = entry.getHook(TcdexHooks.REACTION)
                        .modifyReactionCooldown(tool, entry, reaction, cooldown);
            }
        }
        return new ElementReaction(reaction.getAuraElement(), reaction.getTriggerElement(), reaction.getType(),
                reaction.getAuraCost(),
                Math.max(0, cooldown),
                Math.max(0, (int) duration),
                Math.max(0.0f, radius),
                intensity);
    }

    private static void applyReaction(LivingEntity target, ElementReaction reaction, @Nullable LivingEntity source) {
        switch (reaction.getType()) {
            case CONTROL -> applyControl(target, reaction);
            case DAMAGE -> applyDamage(target, reaction, source);
            case AMPLIFY -> applyAmplify(target, reaction, source);
            case SHIELD -> applyShield(target, reaction, source);
            case DIFFUSION -> diffuse(target, reaction.getAuraElement());
        }
    }

    /** 控制类反应：强减速 + 虚弱，并针对不同反应追加导航停止/致盲/禁跳等控制 */
    private static void applyControl(LivingEntity target, ElementReaction reaction) {
        int duration = reaction.getDuration();
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 6, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 3, false, true));

        // 所有控制都让生物停止寻路并打断当前移动
        if (target instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.setTarget(null);
        }
        target.setDeltaMovement(0, 0, 0);
        target.hurtMarked = true;

        // 重力坍缩：额外把周围敌人拉向中心
        if (reaction.getAuraElement() == ElementType.SINKSTAR
                && reaction.getTriggerElement() == ElementType.VOID
                && reaction.getRadius() > 0) {
            pullEntities(target, reaction.getRadius(), reaction.getIntensity());
        }

        // 沉霜镇压：额外禁止跳跃（负跳跃提升）
        if (reaction.getAuraElement() == ElementType.SINKSTAR
                && reaction.getTriggerElement() == ElementType.STASIS) {
            target.addEffect(new MobEffectInstance(MobEffects.JUMP, duration, -10, false, true));
        }

        // 岚蚀恐惧：额外致盲，模拟恐惧失控
        if (reaction.getAuraElement() == ElementType.VOID
                && reaction.getTriggerElement() == ElementType.MISTFLOW) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration, 0, false, true));
        }

        spawnParticles(target, reaction.getAuraElement().getParticle());
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.HOSTILE, 0.8F, 1.2F);
    }

    /** 伤害类反应：对目标造成额外伤害，可带小范围 AOE */
    private static void applyDamage(LivingEntity target, ElementReaction reaction, @Nullable LivingEntity source) {
        float damage = reaction.getIntensity() > 0 ? reaction.getIntensity() : 4.0f;
        net.minecraft.world.damagesource.DamageSource damageSource = source != null
                ? ModDamageSources.element(source, reaction.getAuraElement())
                : target.damageSources().magic();
        target.hurt(damageSource, damage);

        if (reaction.getRadius() > 0) {
            Level level = target.level();
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                    target.getBoundingBox().inflate(reaction.getRadius()),
                    e -> e != target && e.isAlive() && !(e instanceof Player))) {
                entity.hurt(damageSource, damage * 0.5f);
            }
        }

        spawnParticles(target, reaction.getAuraElement().getParticle());
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.8F, 1.4F);
    }

    /** 增幅类反应：给攻击者施加临时伤害提升 */
    private static void applyAmplify(LivingEntity target, ElementReaction reaction, @Nullable LivingEntity source) {
        if (source != null) {
            int amplifier = Math.max(0, (int) reaction.getIntensity() - 1);
            source.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, reaction.getDuration(), amplifier, false, true));
        }
        spawnParticles(target, reaction.getAuraElement().getParticle());
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.7F, 1.2F);
    }

    /** 护盾类反应：给攻击者（或目标）附加临时伤害吸收 */
    private static void applyShield(LivingEntity target, ElementReaction reaction, @Nullable LivingEntity source) {
        LivingEntity receiver = source != null ? source : target;
        int amplifier = Math.max(0, (int) reaction.getIntensity() - 1);
        receiver.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, reaction.getDuration(), amplifier, false, true));
        spawnParticles(receiver, reaction.getAuraElement().getParticle());
        receiver.level().playSound(null, receiver.getX(), receiver.getY(), receiver.getZ(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.8F, 1.2F);
    }

    /** 把周围非玩家实体拉向中心（重力坍缩） */
    private static void pullEntities(LivingEntity center, float radius, float intensity) {
        Level level = center.level();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                center.getBoundingBox().inflate(radius),
                e -> e != center && e.isAlive() && !(e instanceof Player))) {
            Vec3 toCenter = center.position().subtract(entity.position());
            if (toCenter.lengthSqr() < 0.0001) {
                continue;
            }
            toCenter = toCenter.normalize().scale(intensity);
            entity.setDeltaMovement(entity.getDeltaMovement().add(toCenter));
            entity.hurtMarked = true;
        }
    }

    /** 岚流扩散：把目标身上的元素附着施加给周围非玩家实体 */
    private static void diffuse(LivingEntity target, ElementType aura) {
        Level level = target.level();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                target.getBoundingBox().inflate(DIFFUSION_RADIUS),
                e -> e != target && e.isAlive() && !(e instanceof Player))) {
            // 扩散时施加少量元素状态与附着量（stacks 传 1 作为标记，aura 由 addElementState 自动附加）
            IElementalEntity.of(entity).addElementState(aura, 1, 100);
        }
        spawnParticles(target, aura.getParticle());
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.HOSTILE, 0.6F, 1.4F);
    }

    private static void spawnParticles(LivingEntity target, ParticleOptions particle) {
        Level level = target.level();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(particle, target.getX(), target.getY() + 1.0, target.getZ(),
                    20, 0.4, 0.4, 0.4, 0.1);
        }
    }
}
