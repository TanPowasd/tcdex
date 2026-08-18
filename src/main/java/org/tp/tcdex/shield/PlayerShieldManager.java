package org.tp.tcdex.shield;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.tp.tcdex.modifier.hook.TcdexHooks;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    /** 元素状态干扰：玩家带元素状态时回复速率系数（1.0 = 无影响；配置 playerShieldElementFactor） */
    private static float elementRegenFactor = 0.8f;

    /** 回复速率缓存：玩家 UUID → 本次恢复会话的回复速率（会话开始时计算一次，避免每 tick 遍历装备词条） */
    private static final Map<UUID, Float> REGEN_RATE_CACHE = new HashMap<>();

    private PlayerShieldManager() {
    }

    /** 从配置重载 */
    public static void reloadConfig(boolean enabled, double ratio, int delaySeconds, double rate, double elementFactor) {
        PlayerShieldManager.enabled = enabled;
        PlayerShieldManager.shieldRatio = (float) Math.max(0.0, ratio);
        PlayerShieldManager.regenDelayTicks = Math.max(0, delaySeconds) * 20;
        PlayerShieldManager.regenRate = (float) Math.max(0.0, rate);
        PlayerShieldManager.elementRegenFactor = (float) Math.max(0.0, Math.min(1.0, elementFactor));
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
     * 受击吸收：优先扣护盾（玩家穿戴装备/手持工具词条经 PLAYER_SHIELD hook 可调整吸收量）。
     *
     * @return 溢出伤害（护盾扣完后剩余的伤害，需继续结算到生命）
     */
    public static float absorbDamage(Player player, float amount) {
        float shield = getShield(player);
        float absorbed = Math.min(shield, amount);
        absorbed = dispatchModifyAbsorbed(player, amount, absorbed);
        float overflow = Math.max(0.0f, amount - absorbed);
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

    /**
     * 每 tick 回复：脱战且未满时回复。
     *
     * <p>回复速率在<b>每次恢复会话开始时计算一次并缓存</b>（基础速率 × 元素状态干扰，
     * 再经 PLAYER_SHIELD hook 派发给玩家装备/手持工具词条判断），回复过程中直接使用缓存值——
     * 避免每 tick 遍历装备词条；受击/攻击（非脱战）或护盾满时清除缓存，下次恢复重新派发。</p>
     */
    public static void tickRegen(Player player, long now) {
        if (!enabled) {
            return;
        }
        if (getShield(player) >= getMaxShield(player)) {
            REGEN_RATE_CACHE.remove(player.getUUID());
            return;
        }
        if (isOutOfCombat(player, now)) {
            UUID id = player.getUUID();
            Float rate = REGEN_RATE_CACHE.get(id);
            if (rate == null) {
                // 恢复会话开始：计算一次回复速率（元素状态干扰 + 装备词条 PLAYER_SHIELD hook）
                rate = regenRate * getElementRegenFactor(player);
                rate = dispatchModifyRegenRate(player, rate);
                REGEN_RATE_CACHE.put(id, rate);
            }
            setShield(player, getShield(player) + rate);
        } else {
            // 战斗中（未脱战）：清除缓存，下次恢复重新计算
            REGEN_RATE_CACHE.remove(player.getUUID());
        }
    }

    /** 元素状态干扰系数：带任意元素状态 → 回复速率 × elementRegenFactor（无状态 = 1.0） */
    private static float getElementRegenFactor(Player player) {
        if (elementRegenFactor >= 1.0f) {
            return 1.0f;
        }
        return org.tp.tcdex.modifier.elemental.IElementalEntity.of(player).getAllElementStates().isEmpty()
                ? 1.0f : elementRegenFactor;
    }

    // ===== PLAYER_SHIELD hook 派发 =====

    /** 玩家护盾吸收量调整：遍历玩家全部匠魂装备/手持工具词条（链式） */
    private static float dispatchModifyAbsorbed(Player player, float damageAmount, float absorbed) {
        for (ToolStack tool : playerModifiableTools(player)) {
            for (ModifierEntry entry : tool.getModifierList()) {
                absorbed = entry.getHook(TcdexHooks.PLAYER_SHIELD)
                        .modifyAbsorbed(tool, entry, player, damageAmount, absorbed);
            }
        }
        return absorbed;
    }

    /** 玩家护盾回复速率调整：遍历玩家全部匠魂装备/手持工具词条（链式） */
    private static float dispatchModifyRegenRate(Player player, float rate) {
        for (ToolStack tool : playerModifiableTools(player)) {
            for (ModifierEntry entry : tool.getModifierList()) {
                rate = entry.getHook(TcdexHooks.PLAYER_SHIELD)
                        .modifyRegenRate(tool, entry, player, rate);
            }
        }
        return rate;
    }

    /** 玩家全部匠魂装备/手持工具（护甲 4 槽 + 主手 + 副手，跳过非匠魂/损坏） */
    private static List<ToolStack> playerModifiableTools(Player player) {
        List<ToolStack> tools = new ArrayList<>();
        for (ItemStack stack : player.getArmorSlots()) {
            addModifiableTool(tools, stack);
        }
        addModifiableTool(tools, player.getMainHandItem());
        addModifiableTool(tools, player.getOffhandItem());
        return tools;
    }

    private static void addModifiableTool(List<ToolStack> tools, ItemStack stack) {
        if (!stack.isEmpty() && stack.getItem() instanceof IModifiable) {
            ToolStack tool = ToolStack.from(stack);
            if (!tool.isBroken()) {
                tools.add(tool);
            }
        }
    }
}
