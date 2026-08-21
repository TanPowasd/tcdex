package org.tp.tcdex.integration.tinkers.modifier.melee;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import org.tp.tcdex.Tcdex;
import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.integration.tinkers.modifier.ModifierHelper;
import org.tp.tcdex.integration.tinkers.modifier.base.TcdexBaseModifier;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;

/**
 * 电弧增幅 (arc_amplifier)：命运2 电弧关键词强化词条。
 *
 * <p><b>前置依赖：需要元素充能（电弧）</b>——工具必须带有元素充能词条且固化的元素为电弧
 * （由 {@link ModifierHelper} 判定，词条工作台拒绝不满足依赖的添加）。</p>
 *
 * <p>通过 TCDEX 自定义 {@link org.tp.tcdex.integration.tinkers.modifier.hook.TcdexHooks#ELEMENTAL_KEYWORD}
 * 增强电弧 Jolt 连锁闪电：
 * <ul>
 *   <li>连锁伤害 +50%（3.0 → 4.5）</li>
 *   <li>连锁半径 +1 格（2.0 → 3.0）</li>
 * </ul>
 * 元素充能（电弧）命中目标叠加电弧标记，目标受击时 Jolt 连锁结算
 * （{@link org.tp.tcdex.event.ElementalStateEvents}）自动走本词条调整后的数值。</p>
 */
public class ArcAmplifierModifier extends TcdexBaseModifier {

    /** 连锁伤害加成倍率（+50%） */
    private static final float DAMAGE_BONUS = 1.5f;
    /** 连锁半径加成（格） */
    private static final float RADIUS_BONUS = 1.0f;

    /** 通过 Tinkers 注册事件注册此 Modifier */
    public static void registerModifier(ModifierManager.ModifierRegistrationEvent event) {
        event.registerStatic(new ModifierId(Tcdex.MODID, "arc_amplifier"), new ArcAmplifierModifier());
    }

    /** 无等级词条：显示名不附带等级 */
    @Override
    public Component getDisplayName(int level) {
        return super.getDisplayName();
    }

    /** 依赖校验：工具没有元素充能（电弧）时返回提示（词条工作台拒绝添加） */
    @Override
    protected Component modifierValidate(IToolStackView tool, ModifierEntry modifier) {
        if (!ModifierHelper.hasElementalCharge(tool, ElementType.ARC)) {
            return Component.translatable("modifier.tcdex.requires.elemental_arc");
        }
        return null;
    }

    /** Tooltip：未满足依赖（没有元素充能电弧）时显示红色需求提示 */
    @Override
    protected void modifierAddTooltip(IToolStackView tool, ModifierEntry modifier, Player player, List<Component> tooltip, TooltipKey tooltipKey, TooltipFlag tooltipFlag) {
        if (!ModifierHelper.hasElementalCharge(tool, ElementType.ARC)) {
            tooltip.add(Component.translatable("modifier.tcdex.requires.elemental_arc")
                    .withStyle(style -> style.withColor(TextColor.fromRgb(0xFFFF5555))));
        }
    }

    /** Jolt 连锁伤害 +50%（仅电弧关键词 + 依赖满足时生效；命令强加时兜底） */
    @Override
    protected float modifierModifyKeywordDamage(IToolStackView tool, ModifierEntry modifier, ElementType keyword, float damage) {
        if (keyword == ElementType.ARC && ModifierHelper.hasElementalCharge(tool, ElementType.ARC)) {
            return damage * DAMAGE_BONUS;
        }
        return damage;
    }

    /** Jolt 连锁半径 +1 格（仅电弧关键词 + 依赖满足时生效；命令强加时兜底） */
    @Override
    protected float modifierModifyKeywordRadius(IToolStackView tool, ModifierEntry modifier, ElementType keyword, float radius) {
        if (keyword == ElementType.ARC && ModifierHelper.hasElementalCharge(tool, ElementType.ARC)) {
            return radius + RADIUS_BONUS;
        }
        return radius;
    }
}
