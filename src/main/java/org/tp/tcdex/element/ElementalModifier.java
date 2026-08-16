package org.tp.tcdex.element;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.tp.tcdex.Tcdex;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 命运2元素伤害 Modifier：烈日 / 电弧 / 虚空 / 冰影 / 缚丝。
 *
 * <p>近战命中时附加额外元素伤害，施加对应状态效果，并播放元素粒子。
 * 额外伤害会受到怪物元素抗性/弱点影响。</p>
 */
public class ElementalModifier extends NoLevelsModifier implements MeleeHitModifierHook {

    private final ElementType element;

    public ElementalModifier(ElementType element) {
        this.element = element;
    }

    /** 注册全部元素 Modifier */
    public static void registerModifiers(ModifierManager.ModifierRegistrationEvent event) {
        for (ElementType type : ElementType.values()) {
            event.registerStatic(new ModifierId(Tcdex.MODID, type.getId()), new ElementalModifier(type));
        }
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder builder) {
        super.registerHooks(builder);
        builder.addHook(this, new ModuleHook[]{
                ModifierHooks.MELEE_HIT
        });
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        LivingEntity attacker = context.getAttacker();
        LivingEntity target = context.getLivingTarget();
        if (attacker == null || target == null || target.level().isClientSide) {
            return;
        }

        // 额外元素伤害，受怪物元素抗性/弱点影响
        float resistance = ElementManager.getResistance(target, element);
        float bonusDamage = element.getBaseDamage() * resistance;
        if (bonusDamage > 0) {
            target.hurt(target.damageSources().indirectMagic(attacker, attacker), bonusDamage);
        }

        // 烈日：点燃
        if (element.getFireSeconds() > 0) {
            target.setSecondsOnFire(element.getFireSeconds());
        }

        // 状态效果
        if (element.getEffect() != null && element.getStatusDuration() > 0) {
            target.addEffect(new MobEffectInstance(element.getEffect(), element.getStatusDuration(), element.getStatusAmplifier()));
        }

        // 粒子
        if (target.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(element.getParticle(),
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    15, 0.3, 0.3, 0.3, 0.1);
        }
    }
}
