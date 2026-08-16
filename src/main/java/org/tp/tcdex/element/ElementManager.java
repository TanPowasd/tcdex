package org.tp.tcdex.element;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

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

    private ElementManager() {
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
}
