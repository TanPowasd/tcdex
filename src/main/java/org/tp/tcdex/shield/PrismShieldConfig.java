package org.tp.tcdex.shield;

/**
 * 棱镜盾参数配置（Boss 专属，来自 config/tcdex-common.toml，运行时重载）。
 *
 * <p>集中管理分散在伤害结算/减免/回复三处的棱镜盾数值：
 * <ul>
 *   <li>磨损效率：棱镜（匹配）/ 其他元素 / 动能——决定破盾速度</li>
 *   <li>非玩家伤害减免：元素 / 动能——护盾存在期间直接伤血的减免</li>
 *   <li>脱战回复：延迟（秒）/ 周期（tick）/ 每周期回复比例</li>
 * </ul></p>
 */
public final class PrismShieldConfig {

    /** 棱镜伤害磨损效率（匹配） */
    private static float matchEfficiency = 2.0f;
    /** 其他元素伤害磨损效率 */
    private static float elementEfficiency = 0.5f;
    /** 动能伤害磨损效率 */
    private static float kineticEfficiency = 0.1f;
    /** 非玩家元素伤害减免倍率（0.5 = 50% 减免） */
    private static float elementReduction = 0.5f;
    /** 非玩家动能伤害减免倍率（0.1 = 90% 减免） */
    private static float kineticReduction = 0.1f;
    /** 脱战回复延迟（tick，10 秒 = 200） */
    private static int regenDelayTicks = 200;
    /** 回复周期（tick） */
    private static int regenCycle = 5;
    /** 每周期回复 = 最大护盾值 × 比例 */
    private static float regenPercent = 0.1f;

    private PrismShieldConfig() {
    }

    /** 从 Forge 配置重载 */
    public static void reload(double matchEfficiency, double elementEfficiency, double kineticEfficiency,
                              double elementReduction, double kineticReduction,
                              int regenDelaySeconds, int regenCycle, double regenPercent) {
        PrismShieldConfig.matchEfficiency = (float) Math.max(0.0, matchEfficiency);
        PrismShieldConfig.elementEfficiency = (float) Math.max(0.0, elementEfficiency);
        PrismShieldConfig.kineticEfficiency = (float) Math.max(0.0, kineticEfficiency);
        PrismShieldConfig.elementReduction = (float) Math.max(0.0, Math.min(1.0, elementReduction));
        PrismShieldConfig.kineticReduction = (float) Math.max(0.0, Math.min(1.0, kineticReduction));
        PrismShieldConfig.regenDelayTicks = Math.max(0, regenDelaySeconds) * 20;
        PrismShieldConfig.regenCycle = Math.max(1, regenCycle);
        PrismShieldConfig.regenPercent = (float) Math.max(0.0, regenPercent);
    }

    public static float getMatchEfficiency() {
        return matchEfficiency;
    }

    public static float getElementEfficiency() {
        return elementEfficiency;
    }

    public static float getKineticEfficiency() {
        return kineticEfficiency;
    }

    public static float getElementReduction() {
        return elementReduction;
    }

    public static float getKineticReduction() {
        return kineticReduction;
    }

    public static int getRegenDelayTicks() {
        return regenDelayTicks;
    }

    public static int getRegenCycle() {
        return regenCycle;
    }

    public static float getRegenPercent() {
        return regenPercent;
    }
}
