package org.tp.tcdex.artifact;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.ArrayList;
import java.util.List;

/**
 * 圣遗物管理器：读取玩家已装备的 Curios 圣遗物，并汇总属性。
 */
public final class ArtifactManager {

    private static final String[] SLOT_IDS = {
            "artifact_flower",
            "artifact_plume",
            "artifact_sands",
            "artifact_goblet",
            "artifact_circlet"
    };

    private ArtifactManager() {
    }

    /** 获取玩家已装备的全部圣遗物（Curios 未安装时返回空列表） */
    public static List<ItemStack> getEquippedArtifacts(Player player) {
        List<ItemStack> result = new ArrayList<>();
        if (!ModList.get().isLoaded("curios")) {
            return result;
        }
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            for (String slot : SLOT_IDS) {
                for (SlotResult slotResult : handler.findCurios(slot)) {
                    result.add(slotResult.stack());
                }
            }
        });
        return result;
    }

    /** 获取某类圣遗物属性总值 */
    public static float getTotalStat(Player player, ArtifactStat stat) {
        float total = 0;
        for (ItemStack stack : getEquippedArtifacts(player)) {
            total += ArtifactItem.getStat(stack, stat);
        }
        return total;
    }

    /** 圣遗物提供的元素精通总值 */
    public static int getTotalMastery(Player player) {
        return (int) getTotalStat(player, ArtifactStat.ELEMENTAL_MASTERY);
    }

    /** 圣遗物提供的元素充能效率（百分比小数，如 20 = 0.2） */
    public static float getTotalRechargeEfficiency(Player player) {
        return getTotalStat(player, ArtifactStat.RECHARGE_EFFICIENCY) / 100.0f;
    }

    /** 圣遗物提供的光等总值 */
    public static int getTotalLight(Player player) {
        return (int) getTotalStat(player, ArtifactStat.LIGHT_LEVEL);
    }

    /** 圣遗物提供的元素伤害加成（百分比小数，如 30 = 0.3） */
    public static float getTotalElementDamageBonus(Player player) {
        return getTotalStat(player, ArtifactStat.ELEMENTAL_DAMAGE_BONUS) / 100.0f;
    }

    /** 圣遗物提供的护盾加成（百分比小数） */
    public static float getTotalShieldBonus(Player player) {
        return getTotalStat(player, ArtifactStat.SHIELD_BONUS) / 100.0f;
    }

    /** 圣遗物提供的治疗加成（百分比小数） */
    public static float getTotalHealingBonus(Player player) {
        return getTotalStat(player, ArtifactStat.HEALING_BONUS) / 100.0f;
    }
}
