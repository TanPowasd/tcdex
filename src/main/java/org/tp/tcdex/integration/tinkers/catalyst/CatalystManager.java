package org.tp.tcdex.integration.tinkers.catalyst;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.tp.tcdex.Tcdex;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/**
 * 武器催化：匠魂武器通过元素攻击积累催化进度，提升元素反应效果。
 */
public final class CatalystManager {

    public static final ResourceLocation PROGRESS_KEY = ResourceLocation.fromNamespaceAndPath(Tcdex.MODID, "catalyst_progress");
    public static final ResourceLocation LEVEL_KEY = ResourceLocation.fromNamespaceAndPath(Tcdex.MODID, "catalyst_level");

    public static final int MAX_LEVEL = 5;
    public static final int PROGRESS_PER_LEVEL = 100;

    private CatalystManager() {
    }

    public static int getLevel(ToolStack tool) {
        return tool.getPersistentData().getInt(LEVEL_KEY);
    }

    public static int getProgress(ToolStack tool) {
        return tool.getPersistentData().getInt(PROGRESS_KEY);
    }

    /** 增加催化进度；升级时返回 true */
    public static boolean addProgress(ToolStack tool, ItemStack stack, int amount) {
        int level = getLevel(tool);
        if (level >= MAX_LEVEL) {
            return false;
        }
        int progress = getProgress(tool) + amount;
        boolean leveled = false;
        while (progress >= PROGRESS_PER_LEVEL && level < MAX_LEVEL) {
            progress -= PROGRESS_PER_LEVEL;
            level++;
            leveled = true;
        }
        tool.getPersistentData().putInt(PROGRESS_KEY, progress);
        tool.getPersistentData().putInt(LEVEL_KEY, level);
        if (leveled) {
            tool.updateStack(stack);
        }
        return leveled;
    }

    /** 反应伤害倍率：每级 +10% */
    public static float getDamageMultiplier(ToolStack tool) {
        return 1.0f + getLevel(tool) * 0.10f;
    }

    /** 反应持续时间倍率：每级 +5% */
    public static float getDurationMultiplier(ToolStack tool) {
        return 1.0f + getLevel(tool) * 0.05f;
    }

    /** 反应范围加成：每级 +0.5 格 */
    public static float getRadiusBonus(ToolStack tool) {
        return getLevel(tool) * 0.5f;
    }

    /** 反应冷却缩减：每级 -2 tick */
    public static int getCooldownReduction(ToolStack tool) {
        return getLevel(tool) * 2;
    }
}
