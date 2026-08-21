package org.tp.tcdex.artifact;

/**
 * 圣遗物属性类型（第一版基础属性）。
 */
public enum ArtifactStat {
    MAX_HEALTH("max_health"),
    ATTACK_DAMAGE("attack_damage"),
    ARMOR("armor"),
    ELEMENTAL_MASTERY("elemental_mastery"),
    RECHARGE_EFFICIENCY("recharge_efficiency"),
    CRIT_RATE("crit_rate"),
    CRIT_DAMAGE("crit_damage"),
    ELEMENTAL_DAMAGE_BONUS("elemental_damage_bonus"),
    SHIELD_BONUS("shield_bonus"),
    HEALING_BONUS("healing_bonus"),
    LIGHT_LEVEL("light_level");

    private final String id;

    ArtifactStat(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static ArtifactStat fromId(String id) {
        for (ArtifactStat stat : values()) {
            if (stat.id.equals(id)) {
                return stat;
            }
        }
        return null;
    }
}
