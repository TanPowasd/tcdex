package org.tp.tcdex.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.ForgeMod;

import java.util.UUID;

/**
 * 增幅（Amplified）：命运2 电弧（Arc）关键词 buff。
 *
 * <p><b>独立 buff 机制</b>：击杀带<b>电弧标记</b>的目标 → 获得/刷新本效果
 * （联动逻辑见 {@link org.tp.tcdex.event.AmplifiedEvents}）：
 * <ul>
 *   <li>移动速度 +10%（命运2 增幅：更快）</li>
 *   <li>重力 -10%（跳跃增强，跳得更高）</li>
 *   <li>持有期间死亡 → 电弧爆发（3 格内敌人受电弧伤害 + 电弧标记，命运2 增幅死亡电爆）</li>
 * </ul></p>
 */
public class AmplifiedEffect extends MobEffect {

    /** 效果时长（tick，10 秒；击杀刷新） */
    public static final int DURATION = 200;
    /** 电弧爆发半径（格） */
    public static final float BURST_RADIUS = 3.0f;
    /** 电弧爆发伤害 */
    public static final float BURST_DAMAGE = 4.0f;

    private static final UUID SPEED_UUID = UUID.fromString("9c2f1b4e-7a3d-4f6b-8c1e-5d9a2b4c6f10");
    private static final UUID GRAVITY_UUID = UUID.fromString("1d4e7a9c-3b5f-4c8e-9a2d-6f1b3c5e7a90");

    public AmplifiedEffect() {
        // 增益效果，电弧蓝
        super(MobEffectCategory.BENEFICIAL, 0x5CC8FF);
        // 移动速度 +10%
        addAttributeModifier(Attributes.MOVEMENT_SPEED, SPEED_UUID.toString(), 0.1, AttributeModifier.Operation.MULTIPLY_TOTAL);
        // 重力 -10%（跳跃增强）
        addAttributeModifier(ForgeMod.ENTITY_GRAVITY.get(), GRAVITY_UUID.toString(), -0.1, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
