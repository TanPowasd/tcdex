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

    private static final Map<String, Map<ElementType, Float>> RESISTANCES = new HashMap<>();

    static {
        // 烈焰人：抗烈日，弱虚空/冰影
        Map<ElementType, Float> blaze = new EnumMap<>(ElementType.class);
        blaze.put(ElementType.SOLAR, 0.5f);
        blaze.put(ElementType.VOID, 1.5f);
        blaze.put(ElementType.STASIS, 1.5f);
        RESISTANCES.put("minecraft:blaze", blaze);

        // 雪傀儡：弱烈日，抗冰影
        Map<ElementType, Float> snowGolem = new EnumMap<>(ElementType.class);
        snowGolem.put(ElementType.SOLAR, 1.5f);
        snowGolem.put(ElementType.STASIS, 0.5f);
        RESISTANCES.put("minecraft:snow_golem", snowGolem);

        // 末影人：弱虚空，抗缚丝
        Map<ElementType, Float> enderman = new EnumMap<>(ElementType.class);
        enderman.put(ElementType.VOID, 1.5f);
        enderman.put(ElementType.STRAND, 0.5f);
        RESISTANCES.put("minecraft:enderman", enderman);

        // 凋灵：抗虚空
        Map<ElementType, Float> wither = new EnumMap<>(ElementType.class);
        wither.put(ElementType.VOID, 0.5f);
        RESISTANCES.put("minecraft:wither", wither);
    }

    /** 元素护盾表：entity id → 护盾元素（护盾量 = 最大生命 × 50%，命运2 固定比例） */
    private static final Map<String, ElementType> SHIELDS = new HashMap<>();

    static {
        // 烈焰人/岩浆怪/末影人：虚空护盾
        SHIELDS.put("minecraft:blaze", ElementType.VOID);
        SHIELDS.put("minecraft:magma_cube", ElementType.VOID);
        SHIELDS.put("minecraft:enderman", ElementType.VOID);
        // 雪傀儡：烈日护盾（灼烧克冰）
        SHIELDS.put("minecraft:snow_golem", ElementType.SOLAR);
        // 凋灵：冰影护盾
        SHIELDS.put("minecraft:wither", ElementType.STASIS);
        // 常见敌对生物（方便验证/覆盖更多战斗场景）
        SHIELDS.put("minecraft:zombie", ElementType.VOID);
        SHIELDS.put("minecraft:husk", ElementType.VOID);
        SHIELDS.put("minecraft:skeleton", ElementType.STASIS);
        SHIELDS.put("minecraft:creeper", ElementType.ARC);
        SHIELDS.put("minecraft:guardian", ElementType.ARC);
        SHIELDS.put("minecraft:piglin", ElementType.SOLAR);
        SHIELDS.put("minecraft:phantom", ElementType.STRAND);
    }

    /** 护盾黑名单：这些生物永远不带元素盾（兼容其他 mod 生物，用 entity id 匹配） */
    private static final Set<String> SHIELD_BLACKLIST = new HashSet<>();

    /** 元素盾生成权重（默认各 1，0 = 不生成该元素盾） */
    private static final Map<ElementType, Integer> SHIELD_WEIGHTS = new EnumMap<>(ElementType.class);

    static {
        for (ElementType type : ElementType.values()) {
            SHIELD_WEIGHTS.put(type, 1);
        }
    }

    /** 元素充能随机元素权重（默认各 1，0 = 不会随机到该元素） */
    private static final Map<ElementType, Integer> ELEMENT_WEIGHTS = new EnumMap<>(ElementType.class);

    static {
        for (ElementType type : ElementType.values()) {
            ELEMENT_WEIGHTS.put(type, 1);
        }
    }

    /** 附属 mod 注册的护盾提供器（优先级高于静态表/随机，低于黑名单） */
    private static final List<IElementShieldProvider> SHIELD_PROVIDERS = new ArrayList<>();

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
            SHIELD_WEIGHTS.putIfAbsent(type, 1);
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

    /** 获取实体的元素护盾类型（静态表"指定覆盖"，无则返回 null） */
    @javax.annotation.Nullable
    public static ElementType getShieldElement(LivingEntity entity) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (key != null) {
            return SHIELDS.get(key.toString());
        }
        return null;
    }

    /** 从配置重载元素充能随机权重 */
    public static void reloadElementWeights(Map<ElementType, Integer> weights) {
        ELEMENT_WEIGHTS.clear();
        if (weights != null && !weights.isEmpty()) {
            ELEMENT_WEIGHTS.putAll(weights);
        }
        for (ElementType type : ElementType.values()) {
            ELEMENT_WEIGHTS.putIfAbsent(type, 1);
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
            ElementType[] values = ElementType.values();
            return values[random.nextInt(values.length)];
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
