package org.tp.tcdex.integration.tinkers.modifier.special;

import net.minecraft.network.chat.Component;
import org.tp.tcdex.Tcdex;
import org.tp.tcdex.mastery.ElementalMasteryManager;
import org.tp.tcdex.integration.tinkers.modifier.base.TcdexBaseModifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 元素精通（elemental_mastery）：为玩家提供全局元素精通属性。
 *
 * <p>每个词条等级提供 {@link ElementalMasteryManager#MASTERY_PER_LEVEL} 点元素精通。
 * 元素精通会由 {@link org.tp.tcdex.mastery.ElementalMasteryManager} 统一汇总，
 * 并在元素反应结算时增强反应伤害/范围/持续时间/冷却/附着消耗。</p>
 */
public class ElementalMasteryModifier extends TcdexBaseModifier {

    /** 通过 Tinkers 注册事件注册此 Modifier */
    public static void registerModifier(ModifierManager.ModifierRegistrationEvent event) {
        event.registerStatic(new ModifierId(Tcdex.MODID, "elemental_mastery"), new ElementalMasteryModifier());
    }

    /** 无等级词条：显示名不附带等级 */
    @Override
    public Component getDisplayName(int level) {
        return super.getDisplayName();
    }

    @Override
    protected void modifierAddTooltip(IToolStackView tool, ModifierEntry modifier, net.minecraft.world.entity.player.Player player,
                                      java.util.List<Component> tooltip, slimeknights.mantle.client.TooltipKey tooltipKey,
                                      net.minecraft.world.item.TooltipFlag tooltipFlag) {
        tooltip.add(Component.translatable("modifier.tcdex.elemental_mastery.tooltip",
                ElementalMasteryManager.MASTERY_PER_LEVEL * modifier.getLevel()));
    }
}
