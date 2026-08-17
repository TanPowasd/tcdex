package org.tp.tcdex.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 吞噬（Devour）：命运2 虚空关键词 buff。
 *
 * <p>玩家持有本效果时，击杀带虚空标记的目标 → 回复满生命值并刷新本效果时长
 * （联动逻辑见 {@link org.tp.tcdex.modifier.elemental.ElementalModifier} 的击杀分支）。
 * 目前没有获得方式（预留，供后续词条/物品/技能触发）。</p>
 */
public class DevourEffect extends MobEffect {

    public DevourEffect() {
        // 增益效果，紫色（虚空元素色）
        super(MobEffectCategory.BENEFICIAL, 0x9B59B6);
    }
}
