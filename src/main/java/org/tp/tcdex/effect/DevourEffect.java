package org.tp.tcdex.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 吞噬（Devour）：命运2 虚空关键词 buff。
 *
 * <p><b>独立 buff 机制（不依赖任何词条）</b>：玩家持有本效果时，<b>无论用什么方式击杀</b>
 * （近战/远程、匠魂/原版武器均可）→ 回复满生命值并刷新本效果时长
 * （联动逻辑见 {@link org.tp.tcdex.event.DevourEvents}）。
 * 目前没有获得方式（预留，供后续物品/技能/触发条件赋予）。</p>
 */
public class DevourEffect extends MobEffect {

    /** 吞噬刷新时长（tick，10 秒；击杀刷新） */
    public static final int DURATION = 200;

    public DevourEffect() {
        // 增益效果，紫色（虚空元素色）
        super(MobEffectCategory.BENEFICIAL, 0x9B59B6);
    }
}
