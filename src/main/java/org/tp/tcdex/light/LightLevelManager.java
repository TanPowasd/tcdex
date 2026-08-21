package org.tp.tcdex.light;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.tp.tcdex.Tcdex;
import org.tp.tcdex.api.IDamageModifierProvider;
import org.tp.tcdex.api.IEntityLightLevelProvider;
import org.tp.tcdex.api.IItemLightLevelProvider;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 命运2风格的光等系统。
 *
 * 匠魂工具/盔甲拥有一个“光等”：
 * - 基础光等由材料阶级和强化等级自动计算。
 * - 可以通过“光之精华”灌注额外提升。
 * 怪物也拥有光等，战斗时根据玩家平均光等与怪物光等的差值修正伤害。
 */
public final class LightLevelManager {

    /** 匠魂工具持久数据中保存的额外灌注光等 */
    public static final ResourceLocation LIGHT_LEVEL_KEY = ResourceLocation.fromNamespaceAndPath(Tcdex.MODID, "light_level");
    /** 匠魂工具持久数据中保存的强制光等覆盖值，存在时优先于基础光等+灌注 */
    public static final ResourceLocation LIGHT_LEVEL_OVERRIDE_KEY = ResourceLocation.fromNamespaceAndPath(Tcdex.MODID, "light_level_override");
    /** 怪物持久数据中保存的基础光等 */
    public static final String MONSTER_BASE_LIGHT_TAG = "tcdex_monster_base_light";

    /** 默认怪物光等，可通过配置文件修改 */
    private static int defaultMonsterLight = 20;

    /** 基础光等最低值 */
    private static final int BASE_LIGHT_MIN = 1;
    /** 每个材料阶级提供的光等 */
    private static final int MATERIAL_TIER_POINTS = 8;
    /** 每个强化等级提供的光等 */
    private static final int MODIFIER_LEVEL_POINTS = 3;
    /** 基础光等固定部分 */
    private static final int BASE_LIGHT_CONSTANT = 10;
    /** 怪物生成光等随机浮动范围（±值） */
    private static int monsterSpawnRandomRange = 5;
    /** 世界光等场：出生点附近基线 */
    private static int worldBaseLight = 20;
    /** 地狱光等偏移 */
    private static int netherLightOffset = 25;
    /** 末地光等偏移 */
    private static int endLightOffset = 50;
    /** 其他维度（其他 mod 添加的维度）默认光等偏移 */
    private static int otherDimensionLightOffset = 30;
    /** 维度光等偏移表：维度注册名 → 偏移（覆盖内置与默认，兼容其他 mod 维度） */
    private static final Map<String, Integer> DIMENSION_LIGHT_OFFSETS = new HashMap<>();
    /** 生物群系基础光等表：biome id → 基础光等（未配置时使用 worldBaseLight） */
    private static final Map<String, Integer> BIOME_BASE_LIGHTS = new HashMap<>();
    /** 生物群系距离梯度表：biome id → 每 1000 格增加的光等（未配置时使用 distanceGradientStep） */
    private static final Map<String, Integer> BIOME_LIGHT_GRADIENTS = new HashMap<>();
    /** 距离梯度：每 1000 格增加的光等 */
    private static int distanceGradientStep = 3;
    /** 距离梯度上限 */
    private static int distanceGradientCap = 45;
    /** 时间压力：每多少活跃天数（服务器运行天数）+1 光等 */
    private static int daysPerTimeBonus = 5;
    /** 时间压力上限 */
    private static int maxTimeBonus = 30;
    /** 时间压力蔓延起始距离（此距离内不受时间影响，出生点安全区） */
    private static int timeSpreadStartDist = 2000;
    /** 时间压力蔓延完全生效距离 */
    private static int timeSpreadEndDist = 10000;
    /** 输出伤害：每高 1 光等 +1%，最多 +20%（命运2：光等优势上限 +20） */
    private static float dealtOverlevelStep = 0.01f;
    private static float dealtOverlevelCap = 20.0f;
    /** 输出伤害：每低 1 光等 -2%，最低保留 2%（压光惩罚可压到只剩 2% 伤害） */
    private static float dealtUnderlevelStep = 0.02f;
    private static float dealtUnderlevelMin = 0.02f;
    /** 受到伤害：每低 1 光等 +4%，最多 +100%（2倍，命运2实测值） */
    private static float takenUnderlevelStep = 0.04f;
    private static float takenUnderlevelCap = 2.0f;
    /** 受到伤害：每高 1 光等 -1%，最低 80%（对应优势上限 +20） */
    private static float takenOverlevelStep = 0.01f;
    private static float takenOverlevelMin = 0.8f;
    /** 调试开关：开启后会在聊天框输出光等伤害计算信息 */
    private static boolean debugEnabled = false;
    /** HUD 开关：开启后客户端屏幕显示光等信息 */
    private static boolean hudEnabled = true;

    /** 附属 mod 注册的自定义物品光等提供器 */
    private static final List<IItemLightLevelProvider> ITEM_LIGHT_PROVIDERS = new ArrayList<>();
    /** 附属 mod 注册的自定义实体光等提供器 */
    private static final List<IEntityLightLevelProvider> ENTITY_LIGHT_PROVIDERS = new ArrayList<>();
    /** 附属 mod 注册的自定义伤害修正提供器 */
    private static final List<IDamageModifierProvider> DAMAGE_MODIFIER_PROVIDERS = new ArrayList<>();

    /** 内置的怪物基础光等表，key 为实体注册名 */
    private static final Map<String, Integer> MONSTER_BASE_LIGHTS = new HashMap<>();

    static {
        // 普通怪物
        MONSTER_BASE_LIGHTS.put("minecraft:zombie", 20);
        MONSTER_BASE_LIGHTS.put("minecraft:husk", 25);
        MONSTER_BASE_LIGHTS.put("minecraft:drowned", 25);
        MONSTER_BASE_LIGHTS.put("minecraft:skeleton", 20);
        MONSTER_BASE_LIGHTS.put("minecraft:stray", 25);
        MONSTER_BASE_LIGHTS.put("minecraft:creeper", 25);
        MONSTER_BASE_LIGHTS.put("minecraft:spider", 20);
        MONSTER_BASE_LIGHTS.put("minecraft:cave_spider", 25);
        MONSTER_BASE_LIGHTS.put("minecraft:slime", 15);
        MONSTER_BASE_LIGHTS.put("minecraft:magma_cube", 30);
        MONSTER_BASE_LIGHTS.put("minecraft:zombified_piglin", 30);
        MONSTER_BASE_LIGHTS.put("minecraft:piglin", 30);
        MONSTER_BASE_LIGHTS.put("minecraft:piglin_brute", 45);
        MONSTER_BASE_LIGHTS.put("minecraft:hoglin", 35);
        MONSTER_BASE_LIGHTS.put("minecraft:zoglin", 40);
        MONSTER_BASE_LIGHTS.put("minecraft:enderman", 45);
        MONSTER_BASE_LIGHTS.put("minecraft:endermite", 20);
        MONSTER_BASE_LIGHTS.put("minecraft:silverfish", 15);
        MONSTER_BASE_LIGHTS.put("minecraft:witch", 40);
        MONSTER_BASE_LIGHTS.put("minecraft:phantom", 35);
        MONSTER_BASE_LIGHTS.put("minecraft:shulker", 45);
        MONSTER_BASE_LIGHTS.put("minecraft:vex", 35);
        MONSTER_BASE_LIGHTS.put("minecraft:pillager", 35);
        MONSTER_BASE_LIGHTS.put("minecraft:vindicator", 40);
        MONSTER_BASE_LIGHTS.put("minecraft:evoker", 50);
        MONSTER_BASE_LIGHTS.put("minecraft:ravager", 55);
        // 下界
        MONSTER_BASE_LIGHTS.put("minecraft:blaze", 45);
        MONSTER_BASE_LIGHTS.put("minecraft:ghast", 50);
        MONSTER_BASE_LIGHTS.put("minecraft:wither_skeleton", 45);
        //  Boss
        MONSTER_BASE_LIGHTS.put("minecraft:wither", 100);
        MONSTER_BASE_LIGHTS.put("minecraft:ender_dragon", 120);
        MONSTER_BASE_LIGHTS.put("minecraft:warden", 150);
    }

    /**
     * 从 Forge 配置重新加载怪物光等表。
     *
     * @param entries      格式为 "实体注册名=光等" 的配置项列表
     * @param defaultLight 未在表中配置时的默认光等
     */
    public static void reloadFromConfig(List<? extends String> entries, int defaultLight) {
        defaultMonsterLight = Math.max(1, defaultLight);
        MONSTER_BASE_LIGHTS.clear();
        for (String entry : entries) {
            if (entry == null) {
                continue;
            }
            String trimmed = entry.trim();
            int eq = trimmed.indexOf('=');
            if (eq <= 0 || eq == trimmed.length() - 1) {
                continue;
            }
            String id = trimmed.substring(0, eq).trim();
            String value = trimmed.substring(eq + 1).trim();
            try {
                int light = Integer.parseInt(value);
                MONSTER_BASE_LIGHTS.put(id, Math.max(1, light));
            } catch (NumberFormatException ignored) {
                // 忽略无法解析的行
            }
        }
    }

    /** 设置怪物生成光等随机浮动范围 */
    public static void setMonsterSpawnRandomRange(int range) {
        monsterSpawnRandomRange = Math.max(0, range);
    }

    /** 从 Forge 配置重新加载光等伤害修正系数 */
    public static void reloadDamageConfig(double dealtOverlevelStep, double dealtOverlevelCap, double dealtUnderlevelStep, double dealtUnderlevelMin,
                                         double takenUnderlevelStep, double takenUnderlevelCap,
                                         double takenOverlevelStep, double takenOverlevelMin) {
        LightLevelManager.dealtOverlevelStep = (float) dealtOverlevelStep;
        LightLevelManager.dealtOverlevelCap = (float) dealtOverlevelCap;
        LightLevelManager.dealtUnderlevelStep = (float) dealtUnderlevelStep;
        LightLevelManager.dealtUnderlevelMin = (float) dealtUnderlevelMin;
        LightLevelManager.takenUnderlevelStep = (float) takenUnderlevelStep;
        LightLevelManager.takenUnderlevelCap = (float) takenUnderlevelCap;
        LightLevelManager.takenOverlevelStep = (float) takenOverlevelStep;
        LightLevelManager.takenOverlevelMin = (float) takenOverlevelMin;
    }

    /** 从 Forge 配置重新加载世界光等场参数（基线/维度/距离梯度/时间压力） */
    public static void reloadWorldLightConfig(int worldBaseLight, int netherLightOffset, int endLightOffset,
                                              int distanceGradientStep, int distanceGradientCap,
                                              int daysPerTimeBonus, int maxTimeBonus,
                                              int timeSpreadStartDist, int timeSpreadEndDist) {
        LightLevelManager.worldBaseLight = Math.max(1, worldBaseLight);
        LightLevelManager.netherLightOffset = Math.max(0, netherLightOffset);
        LightLevelManager.endLightOffset = Math.max(0, endLightOffset);
        LightLevelManager.distanceGradientStep = Math.max(0, distanceGradientStep);
        LightLevelManager.distanceGradientCap = Math.max(0, distanceGradientCap);
        LightLevelManager.daysPerTimeBonus = Math.max(1, daysPerTimeBonus);
        LightLevelManager.maxTimeBonus = Math.max(0, maxTimeBonus);
        LightLevelManager.timeSpreadStartDist = Math.max(0, timeSpreadStartDist);
        LightLevelManager.timeSpreadEndDist = Math.max(Math.max(1, timeSpreadStartDist), timeSpreadEndDist);
    }

    /**
     * 从 Forge 配置重新加载维度光等偏移表与其他维度默认偏移。
     *
     * @param entries    格式为 "维度注册名=偏移" 的配置项列表（兼容其他 mod 维度）
     * @param otherOffset 未列出维度的默认偏移（其他 mod 维度）
     */
    public static void reloadDimensionConfig(List<? extends String> entries, int otherOffset) {
        otherDimensionLightOffset = Math.max(0, otherOffset);
        DIMENSION_LIGHT_OFFSETS.clear();
        for (String entry : entries) {
            if (entry == null) {
                continue;
            }
            String trimmed = entry.trim();
            int eq = trimmed.indexOf('=');
            if (eq <= 0 || eq == trimmed.length() - 1) {
                continue;
            }
            String id = trimmed.substring(0, eq).trim();
            String value = trimmed.substring(eq + 1).trim();
            try {
                DIMENSION_LIGHT_OFFSETS.put(id, Math.max(0, Integer.parseInt(value)));
            } catch (NumberFormatException ignored) {
                // 忽略无法解析的行
            }
        }
    }

    /**
     * 从 Forge 配置重新加载生物群系光等表。
     *
     * @param baseEntries     格式为 "biome_id=基础光等"，如 "minecraft:desert=35"
     * @param gradientEntries 格式为 "biome_id=每千格增加光等"，如 "minecraft:desert=6"
     */
    public static void reloadBiomeConfig(List<? extends String> baseEntries, List<? extends String> gradientEntries) {
        BIOME_BASE_LIGHTS.clear();
        if (baseEntries != null) {
            for (String entry : baseEntries) {
                if (entry == null) {
                    continue;
                }
                String trimmed = entry.trim();
                int eq = trimmed.indexOf('=');
                if (eq <= 0 || eq == trimmed.length() - 1) {
                    continue;
                }
                String id = trimmed.substring(0, eq).trim();
                String value = trimmed.substring(eq + 1).trim();
                try {
                    BIOME_BASE_LIGHTS.put(id, Math.max(1, Integer.parseInt(value)));
                } catch (NumberFormatException ignored) {
                    // 忽略无法解析的行
                }
            }
        }

        BIOME_LIGHT_GRADIENTS.clear();
        if (gradientEntries != null) {
            for (String entry : gradientEntries) {
                if (entry == null) {
                    continue;
                }
                String trimmed = entry.trim();
                int eq = trimmed.indexOf('=');
                if (eq <= 0 || eq == trimmed.length() - 1) {
                    continue;
                }
                String id = trimmed.substring(0, eq).trim();
                String value = trimmed.substring(eq + 1).trim();
                try {
                    BIOME_LIGHT_GRADIENTS.put(id, Math.max(0, Integer.parseInt(value)));
                } catch (NumberFormatException ignored) {
                    // 忽略无法解析的行
                }
            }
        }
    }

    /** 注册自定义物品光等提供器 */
    public static void registerItemLightLevelProvider(IItemLightLevelProvider provider) {
        if (provider != null && !ITEM_LIGHT_PROVIDERS.contains(provider)) {
            ITEM_LIGHT_PROVIDERS.add(provider);
        }
    }

    /** 注册自定义实体光等提供器 */
    public static void registerEntityLightLevelProvider(IEntityLightLevelProvider provider) {
        if (provider != null && !ENTITY_LIGHT_PROVIDERS.contains(provider)) {
            ENTITY_LIGHT_PROVIDERS.add(provider);
        }
    }

    /** 注册自定义伤害修正提供器 */
    public static void registerDamageModifierProvider(IDamageModifierProvider provider) {
        if (provider != null && !DAMAGE_MODIFIER_PROVIDERS.contains(provider)) {
            DAMAGE_MODIFIER_PROVIDERS.add(provider);
        }
    }

    /** 是否开启光等调试输出 */
    public static boolean isDebugEnabled() {
        return debugEnabled;
    }

    /** 设置光等调试开关 */
    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
    }

    /** 切换光等调试开关，返回切换后的状态 */
    public static boolean toggleDebug() {
        debugEnabled = !debugEnabled;
        return debugEnabled;
    }

    /** 是否显示光等 HUD */
    public static boolean isHudEnabled() {
        return hudEnabled;
    }

    /** 设置光等 HUD 开关 */
    public static void setHudEnabled(boolean enabled) {
        hudEnabled = enabled;
    }

    private LightLevelManager() {
    }

    /** 判断物品是否为匠魂可改造工具/盔甲 */
    public static boolean isTinkersItem(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof IModifiable;
    }

    /** 判断是否为已经初始化的匠魂工具/盔甲 */
    public static boolean isTinkersToolOrArmor(ItemStack stack) {
        return isTinkersItem(stack) && ToolStack.isInitialized(stack);
    }

    /**
     * 计算匠魂物品的基础光等。
     * 基础光等 = 10 + 材料阶级点数 + 强化等级点数。
     */
    public static int getBaseLightLevel(ItemStack stack) {
        if (!isTinkersToolOrArmor(stack)) {
            return 0;
        }
        ToolStack tool = ToolStack.from(stack);

        int materialPoints = 0;
        for (MaterialVariant variant : tool.getMaterials()) {
            if (!variant.isUnknown()) {
                IMaterial material = variant.get();
                materialPoints += material.getTier() * MATERIAL_TIER_POINTS;
            }
        }

        int modifierPoints = 0;
        for (ModifierEntry entry : tool.getModifiers()) {
            modifierPoints += entry.getLevel() * MODIFIER_LEVEL_POINTS;
        }

        return Math.max(BASE_LIGHT_MIN, BASE_LIGHT_CONSTANT + materialPoints + modifierPoints);
    }

    /** 获取匠魂物品当前额外灌注光等 */
    public static int getInfusionLevel(ItemStack stack) {
        if (!isTinkersToolOrArmor(stack)) {
            return 0;
        }
        return ToolStack.from(stack).getPersistentData().getInt(LIGHT_LEVEL_KEY);
    }

    /** 获取物品最终光等 = 基础光等 + 灌注光等；若存在强制覆盖值则直接返回覆盖值；也支持附属 mod 注册的物品 */
    public static int getLightLevel(ItemStack stack) {
        if (isTinkersToolOrArmor(stack)) {
            ToolStack tool = ToolStack.from(stack);
            if (tool.getPersistentData().contains(LIGHT_LEVEL_OVERRIDE_KEY)) {
                return tool.getPersistentData().getInt(LIGHT_LEVEL_OVERRIDE_KEY);
            }
            return getBaseLightLevel(stack) + getInfusionLevel(stack);
        }
        for (IItemLightLevelProvider provider : ITEM_LIGHT_PROVIDERS) {
            if (provider.canProvide(stack)) {
                return provider.getLightLevel(stack);
            }
        }
        return 0;
    }

    /** 强制将物品光等设置为指定值（匠魂写入覆盖值，自定义物品调用对应提供器） */
    public static void setLightLevel(ItemStack stack, int value) {
        if (isTinkersToolOrArmor(stack)) {
            ToolStack tool = ToolStack.from(stack);
            tool.getPersistentData().putInt(LIGHT_LEVEL_OVERRIDE_KEY, Math.max(1, value));
            tool.updateStack(stack);
            return;
        }
        for (IItemLightLevelProvider provider : ITEM_LIGHT_PROVIDERS) {
            if (provider.canProvide(stack)) {
                provider.setLightLevel(stack, value);
                return;
            }
        }
    }

    /** 是否设置了强制光等覆盖值 */
    public static boolean hasLightLevelOverride(ItemStack stack) {
        if (!isTinkersToolOrArmor(stack)) {
            return false;
        }
        return ToolStack.from(stack).getPersistentData().contains(LIGHT_LEVEL_OVERRIDE_KEY);
    }

    /** 移除强制光等覆盖值，恢复为基础光等+灌注 */
    public static void removeLightLevelOverride(ItemStack stack) {
        if (!isTinkersToolOrArmor(stack)) {
            return;
        }
        ToolStack tool = ToolStack.from(stack);
        tool.getPersistentData().remove(LIGHT_LEVEL_OVERRIDE_KEY);
        tool.updateStack(stack);
    }

    /** 给匠魂物品增加灌注光等，并写回物品 NBT */
    public static void addInfusionLevel(ItemStack stack, int amount) {
        if (!isTinkersToolOrArmor(stack)) {
            return;
        }
        ToolStack tool = ToolStack.from(stack);
        int current = tool.getPersistentData().getInt(LIGHT_LEVEL_KEY);
        tool.getPersistentData().putInt(LIGHT_LEVEL_KEY, Math.max(0, current + amount));
        tool.updateStack(stack);
    }

    /** 计算玩家平均光等：统计已装备的匠魂/自定义装备 */
    public static int getPlayerLightLevel(Player player) {
        int total = 0;
        int count = 0;

        for (ItemStack stack : player.getArmorSlots()) {
            int light = getLightLevel(stack);
            if (light > 0) {
                total += light;
                count++;
            }
        }

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        int mainLight = getLightLevel(mainHand);
        int offLight = getLightLevel(offHand);
        if (mainLight > 0) {
            total += mainLight;
            count++;
        }
        if (offLight > 0) {
            total += offLight;
            count++;
        }

        return count == 0 ? 0 : (int) Math.round((double) total / count);
    }

    /** 计算玩家护甲平均光等：只统计身上穿戴的盔甲，不统计武器 */
    public static int getPlayerArmorLightLevel(Player player) {
        int total = 0;
        int count = 0;
        for (ItemStack stack : player.getArmorSlots()) {
            int light = getLightLevel(stack);
            if (light > 0) {
                total += light;
                count++;
            }
        }
        return count == 0 ? 0 : (int) Math.round((double) total / count);
    }

    /** 计算玩家武器光等：只看主手物品 */
    public static int getPlayerWeaponLightLevel(Player player) {
        return getLightLevel(player.getMainHandItem());
    }

    /**
     * 计算玩家攻击光等。
     * 按 5 个部位加权平均：4 个护甲槽 + 主手武器槽。
     * 例如：护甲光等 0、武器光等 20 → (0*4 + 20) / 5 = 4
     */
    public static int getPlayerAttackLightLevel(Player player) {
        int total = 0;
        int count = 0;

        // 4 个护甲槽，空槽或非匠魂按 0 计算
        for (ItemStack stack : player.getArmorSlots()) {
            total += getLightLevel(stack);
            count++;
        }

        // 主手武器槽
        total += getLightLevel(player.getMainHandItem());
        count++;

        return count == 0 ? 0 : (int) Math.round((double) total / count);
    }

    /** 计算当前服务器所有在线玩家的护甲平均光等（只算盔甲，不算武器） */
    public static int getAllPlayersArmorAverage(Level level) {
        if (level.isClientSide || level.getServer() == null) {
            return 0;
        }
        var players = level.getServer().getPlayerList().getPlayers();
        if (players.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (Player player : players) {
            total += getPlayerArmorLightLevel(player);
        }
        return (int) Math.round((double) total / players.size());
    }

    /** 获取世界光等场中的时间压力加成：按活跃天数（服务器运行天数）递增，封顶 */
    public static int getWorldTimeBonus(Level level) {
        if (level.isClientSide) {
            return 0;
        }
        long activeDays = level.getGameTime() / 24000L;
        return Math.min(maxTimeBonus, (int) (activeDays / daysPerTimeBonus));
    }

    /** 时间压力蔓延权重：起始距离内为 0（出生点安全区），结束距离及以上为 1，中间线性 */
    public static float getTimeSpreadFactor(Level level, BlockPos pos) {
        double distance = Math.sqrt(pos.distSqr(level.getSharedSpawnPos()));
        if (distance <= timeSpreadStartDist) {
            return 0.0f;
        }
        if (distance >= timeSpreadEndDist) {
            return 1.0f;
        }
        return (float) ((distance - timeSpreadStartDist) / (timeSpreadEndDist - timeSpreadStartDist));
    }

    /**
     * 维度光等偏移：
     * 1. 配置表命中（含其他 mod 维度）→ 表值
     * 2. 地狱/末地 → 内置偏移
     * 3. 主世界 → 0（基线已含）
     * 4. 其他维度（其他 mod 添加的维度）→ 默认偏移（30）
     */
    public static int getDimensionLightOffset(Level level) {
        ResourceKey<Level> dimension = level.dimension();
        Integer configured = DIMENSION_LIGHT_OFFSETS.get(dimension.location().toString());
        if (configured != null) {
            return configured;
        }
        if (dimension == Level.NETHER) {
            return netherLightOffset;
        }
        if (dimension == Level.END) {
            return endLightOffset;
        }
        if (dimension == Level.OVERWORLD) {
            return 0;
        }
        return otherDimensionLightOffset;
    }

    /** 获取位置所在生物群系注册名（如 minecraft:plains），无法解析时返回 null */
    @javax.annotation.Nullable
    private static String getBiomeId(Level level, BlockPos pos) {
        return level.getBiome(pos).unwrapKey()
                .map(key -> key.location().toString())
                .orElse(null);
    }

    /** 获取该生物群系的基础光等；未配置时回退到全局 worldBaseLight */
    public static int getBiomeBaseLight(Level level, BlockPos pos) {
        String biome = getBiomeId(level, pos);
        if (biome != null) {
            Integer value = BIOME_BASE_LIGHTS.get(biome);
            if (value != null) {
                return Math.max(1, value);
            }
        }
        return worldBaseLight;
    }

    /** 获取该生物群系的距离光等增加速度（每 1000 格）；未配置时回退到全局 distanceGradientStep */
    public static int getBiomeDistanceGradientStep(Level level, BlockPos pos) {
        String biome = getBiomeId(level, pos);
        if (biome != null) {
            Integer value = BIOME_LIGHT_GRADIENTS.get(biome);
            if (value != null) {
                return Math.max(0, value);
            }
        }
        return distanceGradientStep;
    }

    /** 距离梯度：距出生点每 1000 格增加光等，封顶；速度可按生物群系配置 */
    public static int getDistanceLightGradient(Level level, BlockPos pos) {
        double distance = Math.sqrt(pos.distSqr(level.getSharedSpawnPos()));
        int gradient = (int) (distance / 1000.0 * getBiomeDistanceGradientStep(level, pos));
        return Math.min(distanceGradientCap, gradient);
    }

    /** 生物强度系数：按最大生命值估算，兼容其他 mod 的大型生物 */
    public static float getCreatureMultiplier(LivingEntity entity) {
        float health = entity.getMaxHealth();
        if (health >= 100) {
            return 2.0f;
        }
        if (health >= 50) {
            return 1.5f;
        }
        return 1.0f;
    }

    /**
     * 世界光等场基础值（不含随机浮动）：
     * 地形基础光等（可被生物群系覆盖） + 维度偏移 + 距离梯度 + 时间压力×蔓延权重。
     * 与玩家光等完全解耦：怪物光等由世界位置与时间决定，玩家升级后可碾压低级区域。
     */
    public static int getBaseWorldLight(Level level, BlockPos pos) {
        int light = getBiomeBaseLight(level, pos) + getDimensionLightOffset(level) + getDistanceLightGradient(level, pos);
        int timeBonus = Math.round(getWorldTimeBonus(level) * getTimeSpreadFactor(level, pos));
        return Math.max(1, light + timeBonus);
    }

    /**
     * 生成怪物光等：配置表优先；未配置的生物按世界光等场 × 生物系数，
     * 再叠加小范围随机浮动。首次生成后由调用方锁定到实体 NBT。
     */
    public static int rollMonsterSpawnLight(Level level, BlockPos pos, LivingEntity entity) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        Integer override = key != null ? MONSTER_BASE_LIGHTS.get(key.toString()) : null;
        int base = override != null ? override : Math.round(getBaseWorldLight(level, pos) * getCreatureMultiplier(entity));
        int roll = monsterSpawnRandomRange <= 0 ? 0 : level.random.nextInt(monsterSpawnRandomRange * 2 + 1) - monsterSpawnRandomRange;
        return Math.max(1, base + roll);
    }

    /** 获取怪物当前基础光等 */
    public static int getMonsterLightLevel(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (data.contains(MONSTER_BASE_LIGHT_TAG)) {
            return data.getInt(MONSTER_BASE_LIGHT_TAG);
        }
        for (IEntityLightLevelProvider provider : ENTITY_LIGHT_PROVIDERS) {
            if (provider.canProvide(entity)) {
                return provider.getLightLevel(entity);
            }
        }
        return getDefaultMonsterBaseLight(entity);
    }

    /** 强制设置怪物的基础光等 */
    public static void setMonsterLightLevel(LivingEntity entity, int value) {
        entity.getPersistentData().putInt(MONSTER_BASE_LIGHT_TAG, Math.max(1, value));
        for (IEntityLightLevelProvider provider : ENTITY_LIGHT_PROVIDERS) {
            if (provider.canProvide(entity)) {
                provider.setLightLevel(entity, value);
            }
        }
    }

    /**
     * 玩家攻击怪物时的伤害倍率（命运2风格）：
     * - 高光等：每高 1 光等 +1%，最多 +20%（命运2 优势上限）
     * - 低光等：每低 1 光等 -2%，最低 2%
     */
    public static float getDealtDamageMultiplier(int attackerLight, int defenderLight) {
        float delta = attackerLight - defenderLight;
        float multiplier;
        if (delta >= 0) {
            multiplier = 1.0f + Math.min(delta, dealtOverlevelCap) * dealtOverlevelStep;
        } else {
            multiplier = Math.max(dealtUnderlevelMin, 1.0f + delta * dealtUnderlevelStep);
        }
        for (IDamageModifierProvider provider : DAMAGE_MODIFIER_PROVIDERS) {
            multiplier = provider.modifyDealtDamage(multiplier, attackerLight, defenderLight);
        }
        return multiplier;
    }

    /**
     * 怪物攻击玩家时的伤害倍率（命运2风格）：
     * - 玩家低于怪物：每低 1 光等多受 4% 伤害，最多 2 倍（命运2 实测压光曲线）
     * - 玩家高于怪物：每高 1 光等少受 1% 伤害，最低 80%（对应优势上限 +20）
     */
    public static float getTakenDamageMultiplier(int attackerLight, int defenderLight) {
        float delta = attackerLight - defenderLight;
        float multiplier;
        if (delta >= 0) {
            multiplier = Math.min(takenUnderlevelCap, 1.0f + delta * takenUnderlevelStep);
        } else {
            multiplier = Math.max(takenOverlevelMin, 1.0f + delta * takenOverlevelStep);
        }
        for (IDamageModifierProvider provider : DAMAGE_MODIFIER_PROVIDERS) {
            multiplier = provider.modifyTakenDamage(multiplier, attackerLight, defenderLight);
        }
        return multiplier;
    }

    /**
     * 获取怪物的默认基础光等（无锁定 NBT 时的回退）。
     * 优先查配置文件；未配置的生物按世界光等场 × 生物系数估算；
     * 客户端无法访问世界光等场时回退到默认光等，保证名字标签等渲染稳定。
     */
    public static int getDefaultMonsterBaseLight(LivingEntity entity) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (key != null) {
            Integer value = MONSTER_BASE_LIGHTS.get(key.toString());
            if (value != null) {
                return value;
            }
        }
        if (entity.level().isClientSide) {
            return defaultMonsterLight;
        }
        int light = Math.round(getBaseWorldLight(entity.level(), entity.blockPosition()) * getCreatureMultiplier(entity));
        return Math.max(defaultMonsterLight, Math.min(200, light));
    }

    /** 是否为怪物分类（用于生成时附加光等），兼容其他 mod 的敌对生物 */
    public static boolean isMonster(LivingEntity entity) {
        return entity instanceof net.minecraft.world.entity.monster.Enemy
                || entity.getType().getCategory() == net.minecraft.world.entity.MobCategory.MONSTER;
    }
}
