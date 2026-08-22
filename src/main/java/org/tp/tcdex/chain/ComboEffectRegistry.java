package org.tp.tcdex.chain;

import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.reaction.ReactionType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 45 个二元连携组合专属效果注册表。
 */
public final class ComboEffectRegistry {

    private static final Map<String, ComboEffect> EFFECTS = new HashMap<>();

    static {
        // ===== 光能 × 光能/暗影 =====
        add(ElementType.SOLAR, ElementType.ARC, "熔爆", "Meltburst", ReactionType.DAMAGE, 10f, 0, 2f, 1f);
        add(ElementType.SOLAR, ElementType.VOID, "湮灭", "Annihilation", ReactionType.DAMAGE, 14f, 0, 0f, 1f);
        add(ElementType.ARC, ElementType.VOID, "雷蚀", "Void Lightning", ReactionType.DAMAGE, 10f, 0, 2f, 1f);
        add(ElementType.SOLAR, ElementType.STASIS, "融化", "Melt", ReactionType.DAMAGE, 12f, 0, 1.5f, 1f);
        add(ElementType.SOLAR, ElementType.STRAND, "燃缚", "Burnbind", ReactionType.DAMAGE, 10f, 0, 1.5f, 1f);
        add(ElementType.SOLAR, ElementType.MOON, "日蚀", "Solar Eclipse", ReactionType.DAMAGE, 16f, 0, 2f, 1f);
        add(ElementType.ARC, ElementType.STASIS, "聚导体", "Conductive Link", ReactionType.AMPLIFY, 0f, 200, 0f, 2f);
        add(ElementType.ARC, ElementType.STRAND, "雷缠", "Arc Bind", ReactionType.CONTROL, 0f, 60, 2f, 1f);
        add(ElementType.ARC, ElementType.MOON, "月电", "Moon Volt", ReactionType.DAMAGE, 12f, 0, 2f, 1f);
        add(ElementType.VOID, ElementType.STASIS, "虚空霜", "Void Frost", ReactionType.CONTROL, 0f, 70, 2f, 1f);
        add(ElementType.VOID, ElementType.STRAND, "虚空缚", "Void Bind", ReactionType.DAMAGE, 10f, 0, 1.5f, 1f);
        add(ElementType.VOID, ElementType.MOON, "暗月", "Dark Moon", ReactionType.DAMAGE, 14f, 0, 2f, 1f);

        // ===== 光能/暗影 × 中性 =====
        add(ElementType.SOLAR, ElementType.MISTFLOW, "烈日旋风", "Solar Swirl", ReactionType.DIFFUSION, 0f, 0, 3.5f, 1f);
        add(ElementType.SOLAR, ElementType.TIDE, "蒸发", "Vaporize", ReactionType.DAMAGE, 14f, 0, 1.5f, 1f);
        add(ElementType.SOLAR, ElementType.SINKSTAR, "星火护壁", "Starfire Ward", ReactionType.SHIELD, 0f, 120, 0f, 1f);
        add(ElementType.ARC, ElementType.MISTFLOW, "风暴锁链", "Storm Chain", ReactionType.CONTROL, 0f, 60, 2f, 1f);
        add(ElementType.ARC, ElementType.TIDE, "感电", "Electro-Charged", ReactionType.DAMAGE, 8f, 0, 2f, 1f);
        add(ElementType.ARC, ElementType.SINKSTAR, "雷晶护壁", "Storm Crystal", ReactionType.SHIELD, 0f, 140, 0f, 2f);
        add(ElementType.VOID, ElementType.MISTFLOW, "虚空扩散", "Void Diffusion", ReactionType.DIFFUSION, 0f, 0, 3.5f, 1f);
        add(ElementType.VOID, ElementType.TIDE, "暗流涌动", "Dark Current", ReactionType.DAMAGE, 10f, 0, 2f, 1f);
        add(ElementType.VOID, ElementType.SINKSTAR, "虚空结晶", "Void Crystal", ReactionType.SHIELD, 0f, 160, 0f, 2f);
        add(ElementType.STASIS, ElementType.MISTFLOW, "冰岚", "Frost Gale", ReactionType.CONTROL, 0f, 70, 2f, 1f);
        add(ElementType.STASIS, ElementType.TIDE, "冻结", "Freeze", ReactionType.CONTROL, 0f, 80, 1.5f, 1f);
        add(ElementType.STASIS, ElementType.SINKSTAR, "沉霜镇压", "Frostfall", ReactionType.CONTROL, 0f, 70, 0f, 1f);

        // ===== 暗影 × 暗影/中性 =====
        add(ElementType.STASIS, ElementType.STRAND, "霜缚", "Frostweave", ReactionType.CONTROL, 0f, 80, 1.5f, 1f);
        add(ElementType.STASIS, ElementType.MOON, "极致冰流", "Absolute Zero", ReactionType.CONTROL, 0f, 100, 2f, 1f);
        add(ElementType.STRAND, ElementType.MOON, "蜕散", "Moonmolt", ReactionType.DAMAGE, 12f, 0, 1.5f, 1f);
        add(ElementType.STRAND, ElementType.MISTFLOW, "风缚", "Windbind", ReactionType.CONTROL, 0f, 60, 2f, 1f);
        add(ElementType.STRAND, ElementType.TIDE, "潮缚", "Tidebind", ReactionType.CONTROL, 0f, 60, 2f, 1f);
        add(ElementType.STRAND, ElementType.SINKSTAR, "星缚", "Star Bind", ReactionType.CONTROL, 0f, 60, 2f, 1f);
        add(ElementType.MOON, ElementType.MISTFLOW, "月岚", "Moon Gale", ReactionType.DIFFUSION, 0f, 0, 3.5f, 1f);
        add(ElementType.MOON, ElementType.TIDE, "月潮", "Moon Tide", ReactionType.DAMAGE, 10f, 0, 2f, 1f);
        add(ElementType.MOON, ElementType.SINKSTAR, "月星", "Moonstar", ReactionType.SHIELD, 0f, 180, 0f, 2f);

        // ===== 中性 × 中性 =====
        add(ElementType.MISTFLOW, ElementType.TIDE, "水岚", "Hydro Swirl", ReactionType.DIFFUSION, 0f, 0, 3.5f, 1f);
        add(ElementType.MISTFLOW, ElementType.SINKSTAR, "风星", "Star Gale", ReactionType.DIFFUSION, 0f, 0, 3.5f, 1f);
        add(ElementType.TIDE, ElementType.SINKSTAR, "星潮", "Star Tide", ReactionType.CONTROL, 0f, 80, 2f, 1f);

        // ===== 棱镜 × 各元素 =====
        add(ElementType.PRISM, ElementType.SOLAR, "棱镜烈阳", "Prismatic Sun", ReactionType.DAMAGE, 16f, 0, 3f, 1f);
        add(ElementType.PRISM, ElementType.ARC, "棱镜感电", "Prismatic Volt", ReactionType.DAMAGE, 14f, 0, 2.5f, 1f);
        add(ElementType.PRISM, ElementType.VOID, "棱镜虚空", "Prismatic Void", ReactionType.DAMAGE, 14f, 0, 2.5f, 1f);
        add(ElementType.PRISM, ElementType.STASIS, "棱镜霜封", "Prismatic Frost", ReactionType.CONTROL, 0f, 100, 2f, 1f);
        add(ElementType.PRISM, ElementType.STRAND, "棱镜缠绕", "Prismatic Bind", ReactionType.CONTROL, 0f, 100, 2f, 1f);
        add(ElementType.PRISM, ElementType.MISTFLOW, "棱镜风暴", "Prismatic Storm", ReactionType.DIFFUSION, 0f, 0, 3.5f, 1f);
        add(ElementType.PRISM, ElementType.TIDE, "棱镜潮汐", "Prismatic Tide", ReactionType.DAMAGE, 12f, 0, 2f, 1f);
        add(ElementType.PRISM, ElementType.SINKSTAR, "棱镜结晶", "Prismatic Crystal", ReactionType.SHIELD, 0f, 200, 0f, 3f);
        add(ElementType.PRISM, ElementType.MOON, "月之暗面", "Dark Side of the Moon", ReactionType.DAMAGE, 18f, 0, 3f, 1f);
    }

    private ComboEffectRegistry() {
    }

    private static void add(ElementType a, ElementType b, String cn, String en,
                            ReactionType type, float damage, int duration, float radius, float intensity) {
        ComboEffect effect = new ComboEffect(a, b, cn, en, type, damage, duration, radius, intensity);
        String key = key(a, b);
        EFFECTS.put(key, effect);
    }

    private static String key(ElementType a, ElementType b) {
        return a.getId().compareTo(b.getId()) <= 0
                ? a.getId() + "_" + b.getId()
                : b.getId() + "_" + a.getId();
    }

    public static ComboEffect find(ElementType a, ElementType b) {
        return EFFECTS.get(key(a, b));
    }

    /** 获取链中所有已覆盖的二元组合效果 */
    public static List<ComboEffect> findFor(Collection<ElementType> elements) {
        List<ElementType> list = new ArrayList<>(elements);
        List<ComboEffect> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            for (int j = i + 1; j < list.size(); j++) {
                ComboEffect effect = find(list.get(i), list.get(j));
                if (effect != null) {
                    result.add(effect);
                }
            }
        }
        return result;
    }

    public static Collection<ComboEffect> getAll() {
        return EFFECTS.values();
    }
}
