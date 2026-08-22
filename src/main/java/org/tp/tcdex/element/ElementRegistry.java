package org.tp.tcdex.element;

import net.minecraft.core.particles.ParticleTypes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * TCDEX 元素注册中心（重构骨架）。
 *
 * <p>所有元素定义统一在这里注册、查询。后续可扩展为数据驱动 / Add 包动态注册。</p>
 */
public final class ElementRegistry {

    private static final Map<ElementType, ElementDefinition> DEFINITIONS = new EnumMap<>(ElementType.class);

    static {
        registerDefaultElements();
    }

    private ElementRegistry() {
    }

    public static void register(ElementDefinition definition) {
        if (definition == null || definition.getType() == null) {
            return;
        }
        DEFINITIONS.put(definition.getType(), definition);
    }

    public static ElementDefinition get(ElementType type) {
        return type == null ? null : DEFINITIONS.get(type);
    }

    public static ElementDefinition getById(String id) {
        if (id == null) {
            return null;
        }
        for (ElementDefinition definition : DEFINITIONS.values()) {
            if (definition.getId().equals(id)) {
                return definition;
            }
        }
        return null;
    }

    public static Collection<ElementDefinition> all() {
        return Collections.unmodifiableCollection(new ArrayList<>(DEFINITIONS.values()));
    }

    public static Collection<ElementDefinition> byCategory(ElementCategory category) {
        List<ElementDefinition> result = new ArrayList<>();
        for (ElementDefinition definition : DEFINITIONS.values()) {
            if (definition.getCategory() == category) {
                result.add(definition);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public static boolean isCategory(ElementType type, ElementCategory category) {
        ElementDefinition definition = get(type);
        return definition != null && definition.getCategory() == category;
    }

    private static void registerDefaultElements() {
        // 光能元素
        register(ElementDefinition.builder(ElementType.SOLAR, "solar", ElementCategory.LIGHT)
                .displayName("烈日")
                .color(0xFFFF9A3C)
                .particle(ParticleTypes.FLAME)
                .stacksPerHit(25f)
                .stateDuration(100)
                .doTPerStack(0.01f)
                .auraPerHit(1.0f)
                .addKeyword("ignite")
                .build());
        register(ElementDefinition.builder(ElementType.VOID, "void", ElementCategory.LIGHT)
                .displayName("虚空")
                .color(0xFF9B59B6)
                .particle(ParticleTypes.SCULK_SOUL)
                .stacksPerHit(1f)
                .stateDuration(100)
                .doTPerStack(0f)
                .auraPerHit(1.0f)
                .addKeyword("volatile")
                .addKeyword("weaken")
                .addKeyword("devour")
                .build());
        register(ElementDefinition.builder(ElementType.ARC, "arc", ElementCategory.LIGHT)
                .displayName("电能")
                .color(0xFF5CC8FF)
                .particle(ParticleTypes.ELECTRIC_SPARK)
                .stacksPerHit(1f)
                .stateDuration(100)
                .doTPerStack(0f)
                .auraPerHit(1.0f)
                .addKeyword("jolt")
                .addKeyword("blind")
                .addKeyword("amplified")
                .build());

        // 暗影元素
        register(ElementDefinition.builder(ElementType.STASIS, "stasis", ElementCategory.DARK)
                .displayName("冰影")
                .color(0xFF7FD8E6)
                .particle(ParticleTypes.SNOWFLAKE)
                .stacksPerHit(50f)
                .stateDuration(100)
                .doTPerStack(0f)
                .auraPerHit(1.0f)
                .addKeyword("shatter")
                .build());
        register(ElementDefinition.builder(ElementType.STRAND, "strand", ElementCategory.DARK)
                .displayName("缚丝")
                .color(0xFF8FDB6A)
                .particle(ParticleTypes.ENCHANT)
                .stacksPerHit(25f)
                .stateDuration(100)
                .doTPerStack(0f)
                .auraPerHit(1.0f)
                .addKeyword("sever")
                .addKeyword("suspend")
                .addKeyword("woven_mail")
                .build());
        register(ElementDefinition.builder(ElementType.MOON, "moon", ElementCategory.DARK)
                .displayName("月")
                .color(0xFFC0C0FF)
                .particle(ParticleTypes.SCULK_SOUL)
                .stacksPerHit(10f)
                .stateDuration(100)
                .doTPerStack(0.01f)
                .auraPerHit(1.0f)
                .addKeyword("moon_mark")
                .addKeyword("purify")
                .build());

        // 中性元素
        register(ElementDefinition.builder(ElementType.MISTFLOW, "mistflow", ElementCategory.NEUTRAL)
                .displayName("罡流")
                .color(0xFFA8E6CF)
                .particle(ParticleTypes.CLOUD)
                .stacksPerHit(1f)
                .stateDuration(100)
                .doTPerStack(0f)
                .auraPerHit(1.0f)
                .addKeyword("diffusion")
                .build());
        register(ElementDefinition.builder(ElementType.TIDE, "tide", ElementCategory.NEUTRAL)
                .displayName("水")
                .color(0xFF3B9EFF)
                .particle(ParticleTypes.SPLASH)
                .stacksPerHit(1f)
                .stateDuration(100)
                .doTPerStack(0f)
                .auraPerHit(1.0f)
                .addKeyword("wet")
                .build());
        register(ElementDefinition.builder(ElementType.SINKSTAR, "sinkstar", ElementCategory.NEUTRAL)
                .displayName("落星")
                .color(0xFF5B7DB1)
                .particle(ParticleTypes.END_ROD)
                .stacksPerHit(1f)
                .stateDuration(100)
                .doTPerStack(0f)
                .auraPerHit(1.0f)
                .addKeyword("gravity")
                .addKeyword("crystal")
                .build());

        // 特殊元素
        register(ElementDefinition.builder(ElementType.PRISM, "prism", ElementCategory.SPECIAL)
                .displayName("棱镜")
                .color(0xFFA78BFA)
                .particle(ParticleTypes.FIREWORK)
                .stacksPerHit(1f)
                .stateDuration(100)
                .doTPerStack(0f)
                .auraPerHit(1.0f)
                .addKeyword("refract")
                .reactionParticipant(true)
                .build());
    }
}
