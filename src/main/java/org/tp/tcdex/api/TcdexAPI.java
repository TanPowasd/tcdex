package org.tp.tcdex.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.tp.tcdex.light.LightLevelManager;

/**
 * TCDEX 对外 API。
 *
 * <p>附属 mod 可以通过这个类读取/设置光等、注册自定义光等提供器，以及修改伤害修正。</p>
 */
public final class TcdexAPI {

    private TcdexAPI() {
    }

    /** 获取物品光等（匠魂物品、已注册的自定义物品） */
    public static int getItemLightLevel(ItemStack stack) {
        return LightLevelManager.getLightLevel(stack);
    }

    /** 设置物品光等（匠魂物品、已注册的自定义物品） */
    public static void setItemLightLevel(ItemStack stack, int value) {
        LightLevelManager.setLightLevel(stack, value);
    }

    /** 获取实体光等 */
    public static int getEntityLightLevel(LivingEntity entity) {
        return LightLevelManager.getMonsterLightLevel(entity);
    }

    /** 设置实体光等 */
    public static void setEntityLightLevel(LivingEntity entity, int value) {
        LightLevelManager.setMonsterLightLevel(entity, value);
    }

    /** 获取玩家护甲平均光等（只算盔甲，不算武器） */
    public static int getPlayerArmorLightLevel(Player player) {
        return LightLevelManager.getPlayerArmorLightLevel(player);
    }

    /** 获取玩家武器光等（主手） */
    public static int getPlayerWeaponLightLevel(Player player) {
        return LightLevelManager.getPlayerWeaponLightLevel(player);
    }

    /** 获取玩家攻击光等 = (4个护甲槽 + 主手武器) / 5 */
    public static int getPlayerAttackLightLevel(Player player) {
        return LightLevelManager.getPlayerAttackLightLevel(player);
    }

    /** 注册自定义物品光等提供器 */
    public static void registerItemLightLevelProvider(IItemLightLevelProvider provider) {
        LightLevelManager.registerItemLightLevelProvider(provider);
    }

    /** 注册自定义实体光等提供器 */
    public static void registerEntityLightLevelProvider(IEntityLightLevelProvider provider) {
        LightLevelManager.registerEntityLightLevelProvider(provider);
    }

    /** 注册自定义伤害修正提供器 */
    public static void registerDamageModifierProvider(IDamageModifierProvider provider) {
        LightLevelManager.registerDamageModifierProvider(provider);
    }
}
