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
 * 元素护盾管理。
 *
 * <p>负责护盾破盾效率、护盾黑名单、护盾提供器、护盾生成权重、多层护盾随机。</p>
 */
public final class ElementShieldManager {

    private static final Map<String, Map<String, Float>> SHIELD_EFFICIENCY_TABLE = new HashMap<>();

    private static final float DEFAULT_SAME_EFFICIENCY = 0.5f;
    private static final float DEFAULT_COUNTER_EFFICIENCY = 3.0f;
    private static final float DEFAULT_REVERSE_EFFICIENCY = 0.25f;
    private static final float DEFAULT_NEUTRAL_EFFICIENCY = 1.0f;

    private static final Set<String> SHIELD_BLACKLIST = new HashSet<>();

    private static final Map<ElementType, Integer> SHIELD_WEIGHTS = new EnumMap<>(ElementType.class);

    static {
        for (ElementType type : ElementType.values()) {
            SHIELD_WEIGHTS.put(type, type == ElementType.PRISM || type == ElementType.TIDE || type == ElementType.MOON ? 0 : 1);
        }
    }

    private static final List<IElementShieldProvider> SHIELD_PROVIDERS = new ArrayList<>();

    static {
        registerShieldProvider(entity -> {
            ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
            if (key != null && ("minecraft:wither".equals(key.toString()) || "minecraft:ender_dragon".equals(key.toString()))) {
                return ElementType.PRISM;
            }
            return null;
        });
    }

    private ElementShieldManager() {
    }

    public static void reloadShieldEfficiencyTable(List<? extends String> entries) {
        SHIELD_EFFICIENCY_TABLE.clear();
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
            String pair = trimmed.substring(0, eq).trim();
            String value = trimmed.substring(eq + 1).trim();
            int colon = pair.indexOf(':');
            if (colon <= 0 || colon == pair.length() - 1) {
                continue;
            }
            String shieldId = pair.substring(0, colon).trim();
            String attackId = pair.substring(colon + 1).trim();
            try {
                float multiplier = Float.parseFloat(value);
                SHIELD_EFFICIENCY_TABLE.computeIfAbsent(shieldId, k -> new HashMap<>())
                        .put(attackId, Math.max(0.0f, multiplier));
            } catch (NumberFormatException ignored) {
                // 忽略无法解析的行
            }
        }
    }

    public static float getShieldEfficiency(ElementType shield, ElementType attack) {
        return getShieldEfficiency(shield, attack, true);
    }

    public static float getShieldEfficiency(ElementType shield, ElementType attack, boolean allowCounter) {
        String shieldId = shield.getId();
        String attackId = attack != null ? attack.getId() : "kinetic";
        Map<String, Float> map = SHIELD_EFFICIENCY_TABLE.get(shieldId);
        if (map != null) {
            Float explicit = map.get(attackId);
            if (explicit != null) {
                return explicit;
            }
        }
        if (attack == null) {
            return DEFAULT_NEUTRAL_EFFICIENCY;
        }
        if (attack == shield) {
            return DEFAULT_SAME_EFFICIENCY;
        }
        if (allowCounter && ElementManager.isCounterElement(shield, attack)) {
            return DEFAULT_COUNTER_EFFICIENCY;
        }
        if (allowCounter && ElementManager.isCounterElement(attack, shield)) {
            return DEFAULT_REVERSE_EFFICIENCY;
        }
        return DEFAULT_NEUTRAL_EFFICIENCY;
    }

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
            SHIELD_WEIGHTS.putIfAbsent(type, type == ElementType.PRISM || type == ElementType.TIDE || type == ElementType.MOON ? 0 : 1);
        }
    }

    public static void registerShieldProvider(IElementShieldProvider provider) {
        if (provider != null && !SHIELD_PROVIDERS.contains(provider)) {
            SHIELD_PROVIDERS.add(provider);
        }
    }

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

    public static ElementType rollShieldElement(net.minecraft.util.RandomSource random) {
        return ElementWeightHelper.weightedRoll(random, SHIELD_WEIGHTS);
    }

    public static ElementType rollShieldElement(net.minecraft.util.RandomSource random, ElementType boosted) {
        if (boosted == null) {
            return rollShieldElement(random);
        }
        Map<ElementType, Integer> weights = new EnumMap<>(SHIELD_WEIGHTS);
        Integer weight = weights.get(boosted);
        if (weight != null && weight > 0) {
            weights.put(boosted, weight * 2);
        }
        return ElementWeightHelper.weightedRoll(random, weights);
    }
}
