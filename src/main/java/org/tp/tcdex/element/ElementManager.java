package org.tp.tcdex.element;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.tp.tcdex.api.IElementShieldProvider;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 怪物元素抗性/弱点管理。
 *
 * <p>数值含义：1.0 = 正常，大于 1.0 = 弱点（受到更多元素伤害），小于 1.0 = 抗性（受到更少元素伤害）。</p>
 */
public final class ElementManager {

    /** 怪物元素抗性/弱点表（entity id → 元素 → 倍率；配置 monsterElementResistances，运行时重载） */
    private static final Map<String, Map<ElementType, Float>> RESISTANCES = new HashMap<>();

    /** 元素附着量自然衰减速度（每 tick 减少量；默认 0.01，约 5 秒从 1.0 衰减到 0） */
    private static float auraDecayPerTick = 0.01f;

    /** 获取当前元素附着量衰减速度 */
    public static float getAuraDecayPerTick() {
        return auraDecayPerTick;
    }

    /** 设置元素附着量衰减速度（后续可通过配置开放） */
    public static void setAuraDecayPerTick(float decay) {
        auraDecayPerTick = Math.max(0.0f, decay);
    }

    /**
     * 从 Forge 配置重载元素抗性表。
     *
     * @param entries 格式 "entity_registry_name:element_id=multiplier"，
     *                如 "minecraft:blaze:solar=0.5"（抗烈日）、"minecraft:enderman:void=1.5"（弱虚空）
     */
    public static void reloadResistances(List<? extends String> entries) {
        RESISTANCES.clear();
        if (entries == null) {
            return;
        }
        for (String entry : entries) {
            if (entry == null) {
                continue;
            }
            String trimmed = entry.trim();
            int eq = trimmed.lastIndexOf('=');
            if (eq <= 0 || eq == trimmed.length() - 1) {
                continue;
            }
            String target = trimmed.substring(0, eq).trim();
            String value = trimmed.substring(eq + 1).trim();
            int colon = target.lastIndexOf(':');
            if (colon <= 0 || colon == target.length() - 1) {
                continue;
            }
            String entityId = target.substring(0, colon);
            String elementId = target.substring(colon + 1);
            ElementType element = parseElement(elementId);
            if (element == null) {
                continue;
            }
            try {
                float multiplier = Float.parseFloat(value);
                RESISTANCES.computeIfAbsent(entityId, k -> new EnumMap<>(ElementType.class))
                        .put(element, Math.max(0.0f, multiplier));
            } catch (NumberFormatException ignored) {
                // 忽略无法解析的值
            }
        }
    }

    /** 按元素 id 解析（solar/arc/void/stasis/strand/prism/sinkstar/mistflow），无效返回 null */
    @javax.annotation.Nullable
    private static ElementType parseElement(String id) {
        for (ElementType type : ElementType.values()) {
            if (type.getId().equals(id)) {
                return type;
            }
        }
        return null;
    }

    // 护盾分配链 = 黑名单（绝对无盾）→ 提供器 → 加权随机（已取消静态表指定）

    /** 怪物元素攻击总开关（配置 monsterElementalAttacks；攻击元素 = 护盾元素，同源） */
    private static boolean attackEnabled = true;
    /** 怪物元素攻击命中概率（0-1，配置 monsterElementalAttackChance；1 = 每次命中必施加） */
    private static float attackChance = 1.0f;

    /** 护盾黑名单：这些生物永远不带元素盾（兼容其他 mod 生物，用 entity id 匹配） */
    private static final Set<String> SHIELD_BLACKLIST = new HashSet<>();

    /** 元素盾生成权重（默认各 1，0 = 不生成该元素盾；棱镜暂不参与随机盾） */
    private static final Map<ElementType, Integer> SHIELD_WEIGHTS = new EnumMap<>(ElementType.class);

    static {
        for (ElementType type : ElementType.values()) {
            SHIELD_WEIGHTS.put(type, type == ElementType.PRISM ? 0 : 1);
        }
    }

    /** 元素充能随机元素权重（默认各 1，0 = 不会随机到该元素；棱镜不可通过元素充能词条获得） */
    private static final Map<ElementType, Integer> ELEMENT_WEIGHTS = new EnumMap<>(ElementType.class);

    static {
        for (ElementType type : ElementType.values()) {
            ELEMENT_WEIGHTS.put(type, type == ElementType.PRISM ? 0 : 1);
        }
    }

    /** 附属 mod 注册的护盾提供器（优先级高于加权随机，低于黑名单） */
    private static final List<IElementShieldProvider> SHIELD_PROVIDERS = new ArrayList<>();

    static {
        // 内置：凋零 / 末影龙 100% 棱镜盾（Boss 专属；棱镜盾 = 非棱镜伤害减免 + 脱战回复）
        registerShieldProvider(entity -> {
            ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
            if (key != null && ("minecraft:wither".equals(key.toString()) || "minecraft:ender_dragon".equals(key.toString()))) {
                return ElementType.PRISM;
            }
            return null;
        });
    }

    private ElementManager() {
    }

    /** 从配置重载护盾黑名单与元素权重 */
    public static void reloadShieldConfig(List<? extends String> blacklist, Map<ElementType, Integer> weights) {
        SHIELD_BLACKLIST.clear();
        if (blacklist != null) {
            SHIELD_BLACKLIST.addAll(blacklist);
        }
        SHIELD_WEIGHTS.clear();
        if (weights != null && !weights.isEmpty()) {
            SHIELD_WEIGHTS.putAll(weights);
        }
        for (ElementType type : ElementType.values()) {
            // 配置缺项时兜底：棱镜默认不参与随机盾
            SHIELD_WEIGHTS.putIfAbsent(type, type == ElementType.PRISM ? 0 : 1);
        }
    }

    /** 注册护盾提供器 */
    public static void registerShieldProvider(IElementShieldProvider provider) {
        if (provider != null && !SHIELD_PROVIDERS.contains(provider)) {
            SHIELD_PROVIDERS.add(provider);
        }
    }

    /** 遍历提供器，返回第一个非 null 的护盾元素 */
    @javax.annotation.Nullable
    public static ElementType getProviderShieldElement(LivingEntity entity) {
        for (IElementShieldProvider provider : SHIELD_PROVIDERS) {
            ElementType element = provider.getShieldElement(entity);
            if (element != null) {
                return element;
            }
        }
        return null;
    }

    public static void addShieldBlacklist(String entityId) {
        SHIELD_BLACKLIST.add(entityId);
    }

    public static void removeShieldBlacklist(String entityId) {
        SHIELD_BLACKLIST.remove(entityId);
    }

    public static boolean isShieldBlacklisted(String entityId) {
        return SHIELD_BLACKLIST.contains(entityId);
    }

    /** 实体是否被判定为不带护盾（按注册名匹配黑名单） */
    public static boolean isShieldBlacklisted(LivingEntity entity) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return key != null && SHIELD_BLACKLIST.contains(key.toString());
    }

    // ===== 元素怪物：元素攻击（与元素护盾同源） =====

    /** 怪物元素攻击是否开启 */
    public static boolean isAttackEnabled() {
        return attackEnabled;
    }

    /** 当前怪物元素攻击命中概率（0-1） */
    public static float getAttackChance() {
        return attackChance;
    }

    /** 设置怪物元素攻击命中概率（钳制 0-1） */
    public static void setAttackChance(float chance) {
        attackChance = Math.max(0.0f, Math.min(1.0f, chance));
    }

    /** 从配置重载怪物元素攻击开关与命中概率 */
    public static void reloadAttackConfig(boolean enabled, double chance) {
        attackEnabled = enabled;
        attackChance = (float) Math.max(0.0, Math.min(1.0, chance));
    }

    public static void setShieldWeight(ElementType element, int weight) {
        SHIELD_WEIGHTS.put(element, Math.max(0, weight));
    }

    public static Map<ElementType, Integer> getShieldWeights() {
        return new EnumMap<>(SHIELD_WEIGHTS);
    }

    /** 获取实体对某元素的伤害倍率 */
    public static float getResistance(LivingEntity entity, ElementType element) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (key != null) {
            Map<ElementType, Float> map = RESISTANCES.get(key.toString());
            if (map != null) {
                return map.getOrDefault(element, 1.0f);
            }
        }
        return 1.0f;
    }

    /** 从配置重载元素充能随机权重 */
    public static void reloadElementWeights(Map<ElementType, Integer> weights) {
        ELEMENT_WEIGHTS.clear();
        if (weights != null && !weights.isEmpty()) {
            ELEMENT_WEIGHTS.putAll(weights);
        }
        for (ElementType type : ElementType.values()) {
            // 配置缺项时兜底：棱镜默认不参与元素充能随机
            ELEMENT_WEIGHTS.putIfAbsent(type, type == ElementType.PRISM ? 0 : 1);
        }
    }

    public static void setElementWeight(ElementType element, int weight) {
        ELEMENT_WEIGHTS.put(element, Math.max(0, weight));
    }

    public static Map<ElementType, Integer> getElementWeights() {
        return new EnumMap<>(ELEMENT_WEIGHTS);
    }

    /** 按权重随机分配元素充能元素（权重全 0 时退化为等概率） */
    public static ElementType rollElement(net.minecraft.util.RandomSource random) {
        return weightedRoll(random, ELEMENT_WEIGHTS);
    }

    /** 按权重随机分配护盾元素（权重全 0 时退化为等概率） */
    public static ElementType rollShieldElement(net.minecraft.util.RandomSource random) {
        return weightedRoll(random, SHIELD_WEIGHTS);
    }

    /** 通用加权随机 */
    private static ElementType weightedRoll(net.minecraft.util.RandomSource random, Map<ElementType, Integer> weights) {
        int total = 0;
        for (Integer weight : weights.values()) {
            total += weight;
        }
        if (total <= 0) {
            // 全 0 时退化为等概率，但 Prism 不参与随机分配
            java.util.List<ElementType> candidates = new ArrayList<>();
            for (ElementType type : ElementType.values()) {
                if (type != ElementType.PRISM) {
                    candidates.add(type);
                }
            }
            return candidates.get(random.nextInt(candidates.size()));
        }
        int roll = random.nextInt(total);
        for (Map.Entry<ElementType, Integer> entry : weights.entrySet()) {
            roll -= entry.getValue();
            if (roll < 0) {
                return entry.getKey();
            }
        }
        return ElementType.values()[0];
    }

    /** 是否为敌对生物（兼容其他 mod 的敌对生物：Enemy 接口或 MONSTER 分类） */
    public static boolean isMonster(LivingEntity entity) {
        return entity instanceof net.minecraft.world.entity.monster.Enemy
                || entity.getType().getCategory() == net.minecraft.world.entity.MobCategory.MONSTER;
    }
}
