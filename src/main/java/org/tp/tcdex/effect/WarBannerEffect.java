package org.tp.tcdex.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 战争旗帜（War Banner）：命运2 泰坦 Banner of War 效果。
 *
 * <p>玩家击杀任意敌人时自动扬起旗帜（施加/刷新本效果）：
 * <ul>
 *   <li><b>层数 = amplifier + 1</b>（击杀叠层，上限 4 层），每次击杀刷新 8 秒时长</li>
 *   <li>8 秒无击杀 → 效果到期，旗帜落地（层数清零）</li>
 *   <li>效果持有期间：附近玩家（8 格内，含自己）伤害 +8%/层，每秒治疗 0.5/层
 *       （联动逻辑见 {@link org.tp.tcdex.event.WarBannerEvents}）</li>
 * </ul></p>
 */
public class WarBannerEffect extends MobEffect {

    /** 效果时长（tick，8 秒；击杀刷新） */
    public static final int DURATION = 160;
    /** 层数上限（amplifier 上限 = 3，对应 4 层） */
    public static final int MAX_AMPLIFIER = 3;
    /** 旗帜作用半径（格） */
    public static final float RADIUS = 8.0f;
    /** 每层伤害加成（命运2 数值：+8%） */
    public static final float DAMAGE_PER_STACK = 0.08f;
    /** 每秒每层治疗量 */
    public static final float HEAL_PER_STACK = 0.5f;

    public WarBannerEffect() {
        // 增益效果，金色（旗帜色）
        super(MobEffectCategory.BENEFICIAL, 0xD4A017);
    }
}
