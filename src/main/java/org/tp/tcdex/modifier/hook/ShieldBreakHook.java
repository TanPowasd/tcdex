package org.tp.tcdex.modifier.hook;

import net.minecraft.world.entity.LivingEntity;
import org.tp.tcdex.element.ElementType;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * 破盾 Hook：护盾被打穿时，武器上的词条可调整破盾爆炸伤害或触发额外联动
 * （由 {@link org.tp.tcdex.event.ElementalDamageEvents} 在普通盾/棱镜盾破盾时派发）。
 */
public interface ShieldBreakHook {

    /**
     * 调整破盾爆炸伤害（AOE 基础值 = 目标最大生命 × 10%，元素抗性在调用方内部另行应用）。
     *
     * @param target        被打穿护盾的目标
     * @param shieldElement 护盾元素
     * @param damage        当前爆炸基础伤害
     * @return 调整后的爆炸基础伤害
     */
    default float modifyBreakExplosion(IToolStackView tool, ModifierEntry modifier,
                                       LivingEntity target, ElementType shieldElement, float damage) {
        return damage;
    }

    /**
     * 破盾回调：护盾被打穿后触发（可施加额外效果）。
     *
     * @param attacker 破盾的攻击者（环境伤害时为 null）
     */
    default void onShieldBreak(IToolStackView tool, ModifierEntry modifier,
                               LivingEntity target, ElementType shieldElement, @Nullable LivingEntity attacker) {
    }

    /** 合并器：逐个调用所有实现（AllMerger） */
    record AllMerger(Collection<ShieldBreakHook> modules) implements ShieldBreakHook {
        @Override
        public float modifyBreakExplosion(IToolStackView tool, ModifierEntry modifier,
                                          LivingEntity target, ElementType shieldElement, float damage) {
            for (ShieldBreakHook module : modules) {
                damage = module.modifyBreakExplosion(tool, modifier, target, shieldElement, damage);
            }
            return damage;
        }

        @Override
        public void onShieldBreak(IToolStackView tool, ModifierEntry modifier,
                                  LivingEntity target, ElementType shieldElement, @Nullable LivingEntity attacker) {
            for (ShieldBreakHook module : modules) {
                module.onShieldBreak(tool, modifier, target, shieldElement, attacker);
            }
        }
    }
}
