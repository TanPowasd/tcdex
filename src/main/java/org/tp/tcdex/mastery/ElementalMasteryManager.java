package org.tp.tcdex.mastery;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.tp.tcdex.artifact.ArtifactManager;
import org.tp.tcdex.modifier.special.ElementalMasteryModifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 元素精通（Elemental Mastery）全局属性管理器。
 *
 * <p>元素精通是玩家全局属性，来源包括：
 * <ul>
 *   <li>玩家持久数据中的基础精通（命令/API 写入）</li>
 *   <li>已装备/手持匠魂工具上的 {@link ElementalMasteryModifier} 词条</li>
 * </ul>
 * 反应结算时读取 {@link #getMastery(Player)} 来增强反应伤害/范围/持续时间等。</p>
 */
public final class ElementalMasteryManager {

    /** 玩家持久数据中的基础元素精通 tag */
    public static final String MASTERY_TAG = "tcdex_elemental_mastery";

    /** 每个元素精通词条等级提供的精通值 */
    public static final int MASTERY_PER_LEVEL = 20;

    /** 每点精通提升的反应伤害比例（0.5% = 0.005） */
    private static final float DAMAGE_PER_MASTERY = 0.005f;
    /** 每点精通提升的反应持续时间比例（0.2% = 0.002） */
    private static final float DURATION_PER_MASTERY = 0.002f;
    /** 每点精通增加的反应范围（格） */
    private static final float RADIUS_PER_MASTERY = 0.01f;
    /** 每点精通降低的反应冷却比例（0.1% = 0.001） */
    private static final float COOLDOWN_REDUCTION_PER_MASTERY = 0.001f;
    /** 每点精通降低的附着消耗比例（0.1% = 0.001） */
    private static final float AURA_COST_REDUCTION_PER_MASTERY = 0.001f;

    private ElementalMasteryManager() {
    }

    /** 获取玩家当前总元素精通 */
    public static int getMastery(Player player) {
        int mastery = player.getPersistentData().getInt(MASTERY_TAG);
        for (ItemStack stack : allModifiableStacks(player)) {
            ToolStack tool = ToolStack.from(stack);
            for (ModifierEntry entry : tool.getModifierList()) {
                if (entry.getModifier() instanceof ElementalMasteryModifier) {
                    mastery += MASTERY_PER_LEVEL * entry.getLevel();
                }
            }
        }
        // 圣遗物提供的元素精通
        mastery += ArtifactManager.getTotalMastery(player);
        return Math.max(0, mastery);
    }

    /** 设置玩家基础元素精通（不会覆盖词条提供的精通） */
    public static void setMastery(Player player, int value) {
        player.getPersistentData().putInt(MASTERY_TAG, Math.max(0, value));
    }

    /** 增加玩家基础元素精通 */
    public static void addMastery(Player player, int amount) {
        setMastery(player, getBaseMastery(player) + amount);
    }

    /** 获取玩家基础元素精通（持久数据部分） */
    public static int getBaseMastery(Player player) {
        return player.getPersistentData().getInt(MASTERY_TAG);
    }

    /** 反应伤害倍率：1 + 精通 × 0.5% */
    public static float getDamageMultiplier(Player player) {
        return 1.0f + getMastery(player) * DAMAGE_PER_MASTERY;
    }

    /** 反应持续时间倍率：1 + 精通 × 0.2% */
    public static float getDurationMultiplier(Player player) {
        return 1.0f + getMastery(player) * DURATION_PER_MASTERY;
    }

    /** 反应范围加成（格）：精通 × 0.01 */
    public static float getRadiusBonus(Player player) {
        return getMastery(player) * RADIUS_PER_MASTERY;
    }

    /** 反应冷却倍率：1 - 精通 × 0.1%（最低不低于 0.2） */
    public static float getCooldownMultiplier(Player player) {
        return Math.max(0.2f, 1.0f - getMastery(player) * COOLDOWN_REDUCTION_PER_MASTERY);
    }

    /** 附着消耗倍率：1 - 精通 × 0.1%（最低不低于 0.1） */
    public static float getAuraCostMultiplier(Player player) {
        return Math.max(0.1f, 1.0f - getMastery(player) * AURA_COST_REDUCTION_PER_MASTERY);
    }

    /** 获取玩家所有已装备/手持的可用匠魂工具 */
    private static List<ItemStack> allModifiableStacks(Player player) {
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemStack stack : List.of(
                player.getMainHandItem(),
                player.getOffhandItem(),
                player.getItemBySlot(EquipmentSlot.HEAD),
                player.getItemBySlot(EquipmentSlot.CHEST),
                player.getItemBySlot(EquipmentSlot.LEGS),
                player.getItemBySlot(EquipmentSlot.FEET))) {
            if (!stack.isEmpty() && stack.getItem() instanceof IModifiable) {
                ToolStack tool = ToolStack.from(stack);
                if (!tool.isBroken()) {
                    stacks.add(stack);
                }
            }
        }
        return stacks;
    }
}
