package org.tp.tcdex.shield;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * 玩家脱战自动回复护盾（命运2 风格）。
 *
 * <p>玩家拥有独立的护盾层：受击先扣护盾（护盾吸收护甲减免后的伤害），
 * 脱战（一定时间未受伤且未攻击）后护盾自动快速回复。
 * 护盾值存玩家 persistentData（随存档），服务端权威，经网络包同步给客户端 HUD。</p>
 */
public final class PlayerShieldManager {

    /** 玩家护盾值 tag */
    public static final String SHIELD_TAG = "tcdex_player_shield";
    /** 最近一次受伤时间（gameTime tick）tag */
    public static final String LAST_HURT_TAG = "tcdex_player_last_hurt";
    /** 最近一次攻击时间（gameTime tick）tag */
    public static final String LAST_ATTACK_TAG = "tcdex_player_last_attack";

    /** 护盾系统开关 */
    private static boolean enabled = true;
    /** 护盾上限 = 最大生命 × 比例 */
    private static float shieldRatio = 1.0f;
    /** 脱战延迟（tick） */
    private static int regenDelayTicks = 100;
    /** 脱战后每 tick 回复量 */
    private static float regenRate = 0.4f;

    private PlayerShieldManager() {
    }

    /** 从配置重载 */
    public static void reloadConfig(boolean enabled, double ratio, int delaySeconds, double rate) {
        PlayerShieldManager.enabled = enabled;
        PlayerShieldManager.shieldRatio = (float) Math.max(0.0, ratio);
        PlayerShieldManager.regenDelayTicks = Math.max(0, delaySeconds) * 20;
        PlayerShieldManager.regenRate = (float) Math.max(0.0, rate);
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /** 护盾上限 */
    public static float getMaxShield(Player player) {
        return player.getMaxHealth() * shieldRatio;
    }

    /** 当前护盾值 */
    public static float getShield(Player player) {
        return player.getPersistentData().getFloat(SHIELD_TAG);
    }

    /** 设置护盾值（钳制到 0 ~ 上限） */
    public static void setShield(Player player, float value) {
        player.getPersistentData().putFloat(SHIELD_TAG, Math.max(0.0f, Math.min(getMaxShield(player), value)));
    }

    /**
     * 受击吸收：优先扣护盾。
     *
     * @return 溢出伤害（护盾扣完后剩余的伤害，需继续结算到生命）
     */
    public static float absorbDamage(Player player, float amount) {
        float shield = getShield(player);
        float absorbed = Math.min(shield, amount);
        float overflow = amount - absorbed;
        if (absorbed > 0) {
            setShield(player, shield - absorbed);
        }
        return overflow;
    }

    /** 记录受伤时间（重置脱战计时） */
    public static void markHurt(Player player, long now) {
        player.getPersistentData().putLong(LAST_HURT_TAG, now);
    }

    /** 记录攻击时间（重置脱战计时） */
    public static void markAttack(Player player, long now) {
        player.getPersistentData().putLong(LAST_ATTACK_TAG, now);
    }

    /** 是否脱战（超过脱战延迟未受伤且未攻击） */
    public static boolean isOutOfCombat(Player player, long now) {
        CompoundTag data = player.getPersistentData();
        return now - data.getLong(LAST_HURT_TAG) > regenDelayTicks
                && now - data.getLong(LAST_ATTACK_TAG) > regenDelayTicks;
    }

    /** 每 tick 回复：脱战且未满时回复 */
    public static void tickRegen(Player player, long now) {
        if (!enabled) {
            return;
        }
        if (getShield(player) >= getMaxShield(player)) {
            return;
        }
        if (isOutOfCombat(player, now)) {
            setShield(player, getShield(player) + regenRate);
        }
    }
}
