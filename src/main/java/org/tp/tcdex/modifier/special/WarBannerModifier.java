package org.tp.tcdex.modifier.special;

import net.minecraft.network.chat.Component;
import org.tp.tcdex.Tcdex;
import org.tp.tcdex.modifier.base.TcdexBaseModifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 战争旗帜 (war_banner)：命运2 泰坦 Banner of War 效果词条。
 *
 * <p><b>击杀扬旗</b>（仅本词条持有者击杀触发，联动见 {@link org.tp.tcdex.event.WarBannerEvents}）：
 * <ul>
 *   <li>击杀任意敌人 → 扬起战争旗帜（层数 +1，上限 4，时长 8 秒，击杀刷新）</li>
 *   <li>旗帜期间：8 格内玩家（含自己）伤害 +8%/层、每秒治疗 0.5/层</li>
 *   <li>8 秒无击杀 → 效果到期，旗帜落地（层数清零）</li>
 * </ul></p>
 */
public class WarBannerModifier extends TcdexBaseModifier {

    /** 通过 Tinkers 注册事件注册此 Modifier */
    public static void registerModifier(ModifierManager.ModifierRegistrationEvent event) {
        event.registerStatic(new ModifierId(Tcdex.MODID, "war_banner"), new WarBannerModifier());
    }

    /** 无等级词条：显示名不附带等级 */
    @Override
    public Component getDisplayName(int level) {
        return super.getDisplayName();
    }
}
