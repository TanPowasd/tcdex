package org.tp.tcdex.element;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.tp.tcdex.modifier.elemental.IElementalEntity;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 元素抗性 / 弱点管理。
 *
 * <p>负责实体类型级元素抗性表、Add 包动态抗性，以及元素适应对最终抗性的影响。</p>
 */
public final class ElementResistanceManager {

    /** 配置加载的元素抗性表（优先级最高） */
    private static final Map<String, Map<ElementType, Float>> RESISTANCES = new HashMap<>();

    /** Add 包注册的额外抗性（优先级低于配置） */
    private static final Map<String, Map<ElementType, Float>> RESISTANCE_OVERRIDES = new HashMap<>();

    private ElementResistanceManager() {
    }

    public static void reload(List<? extends String> entries) {
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
            ElementType element = ElementManager.parseElement(elementId);
            if (element == null) {
                continue;
            }
            try {
                float multiplier = Float.parseFloat(value);
                RESISTANCES.computeIfAbsent(entityId, k -> new EnumMap<>(ElementType.class))
                        .put(element, Math.max(0.0f, multiplier));
            } catch (NumberFormatException ignored) {
                // 忽略无法解析的行
            }
        }
    }

    public static void registerResistance(String entityId, ElementType element, float multiplier) {
        if (entityId == null || element == null) {
            return;
        }
        RESISTANCE_OVERRIDES.computeIfAbsent(entityId, k -> new EnumMap<>(ElementType.class))
                .put(element, Math.max(0.05f, multiplier));
    }

    public static float getResistance(LivingEntity entity, ElementType element) {
        float resistance = 1.0f;
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (key != null) {
            Map<ElementType, Float> configMap = RESISTANCES.get(key.toString());
            if (configMap != null && configMap.containsKey(element)) {
                resistance = configMap.get(element);
            } else {
                Map<ElementType, Float> overrideMap = RESISTANCE_OVERRIDES.get(key.toString());
                if (overrideMap != null) {
                    resistance = overrideMap.getOrDefault(element, 1.0f);
                }
            }
        }
        if (entity instanceof IElementalEntity elemental) {
            resistance *= (1.0f - elemental.getElementAdaptation(element));
        }
        return Math.max(0.05f, resistance);
    }
}
