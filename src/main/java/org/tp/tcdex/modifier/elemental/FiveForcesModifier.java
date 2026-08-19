package org.tp.tcdex.modifier.elemental;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.tp.tcdex.Tcdex;
import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.modifier.ModifierExclusivity;
import org.tp.tcdex.modifier.base.TcdexBaseModifier;
import org.tp.tcdex.modifier.hook.TcdexHooks;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/**
 * 五项之力 (five_forces)：每次攻击将动能伤害随机化为一种元素伤害。
 *
 * <p>与元素充能（固化单一元素，不可改变）相对：本词条<b>每次攻击重新随机</b>
 * （烈日/电弧/虚空/冰影/缚丝，权重遵循 elementWeight 配置，棱镜不可 roll 到），
 * 命中同时施加所 roll 元素的元素状态（触发关键词联动）。近战与远程均生效
 * （随机与转化在 {@link org.tp.tcdex.event.ElementalDamageEvents} 统一处理，
 * 保证伤害元素与状态元素一致）。</p>
 *
 * <p>与元素充能 / 棱镜共鸣互斥（伤害类型决定权唯一）；与动能词条（震颤/虹吸）不互斥——
 * 可混搭为"随机元素攻击 + 动能特性"的 hybrid build（动能词条按工具判定，不受影响）。</p>
 */
public class FiveForcesModifier extends TcdexBaseModifier {

    /** 通过 Tinkers 注册事件注册此 Modifier */
    public static void registerModifier(ModifierManager.ModifierRegistrationEvent event) {
        event.registerStatic(new ModifierId(Tcdex.MODID, "five_forces"), new FiveForcesModifier());
    }

    /** 无等级词条：显示名不附带等级 */
    @Override
    public Component getDisplayName(int level) {
        return super.getDisplayName();
    }

    /** 互斥校验：与元素充能/棱镜共鸣互斥（伤害类型决定权唯一） */
    @Override
    protected Component modifierValidate(IToolStackView tool, ModifierEntry modifier) {
        return ModifierExclusivity.validate(tool, modifier);
    }

    /**
     * 命中施加本次 roll 到的元素状态（由 ElementalDamageEvents 转化时调用，
     * 保证与伤害元素同源一致；近战/远程通用）。
     *
     * <p>粒子 + ELEMENTAL_STATE_APPLY hook 链式调整层数/时长 + 叠加状态，
     * 与 {@link ElementalModifier} 的命中施加逻辑同一套体系。</p>
     *
     * @param tool    攻击工具（转化处实例；null 时跳过 hook 派发）
     * @param target  受击目标
     * @param element 本次攻击 roll 到的元素
     */
    public static void applyHitState(ToolStack tool, LivingEntity target, ElementType element) {
        if (target.level().isClientSide) {
            return;
        }
        // 粒子
        if (target.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(element.getParticle(),
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    15, 0.3, 0.3, 0.3, 0.1);
        }
        // 元素状态施加（ELEMENTAL_STATE_APPLY hook 链式调整层数/时长）
        float stacks = element.getStacksPerHit();
        int duration = element.getStateDuration();
        if (tool != null) {
            for (ModifierEntry entry : tool.getModifierList()) {
                stacks = entry.getHook(TcdexHooks.ELEMENTAL_STATE_APPLY)
                        .modifyStateStacks(tool, entry, element, stacks);
                duration = entry.getHook(TcdexHooks.ELEMENTAL_STATE_APPLY)
                        .modifyStateDuration(tool, entry, element, duration);
            }
        }
        if (stacks > 0 && duration > 0) {
            IElementalEntity.of(target).addElementState(element, stacks, duration);
        }
    }
}
