package org.tp.tcdex.element;

/**
 * 怪物元素攻击管理。
 */
public final class ElementMonsterAttackManager {

    private static boolean attackEnabled = true;
    private static float attackChance = 1.0f;

    private ElementMonsterAttackManager() {
    }

    public static boolean isAttackEnabled() {
        return attackEnabled;
    }

    public static float getAttackChance() {
        return attackChance;
    }

    public static void setAttackChance(float chance) {
        attackChance = Math.max(0.0f, Math.min(1.0f, chance));
    }

    public static void reloadAttackConfig(boolean enabled, double chance) {
        attackEnabled = enabled;
        attackChance = (float) Math.max(0.0, Math.min(1.0, chance));
    }
}
