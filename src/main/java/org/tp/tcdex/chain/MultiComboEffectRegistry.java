package org.tp.tcdex.chain;

import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.reaction.ReactionType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 多元素链组合（三元及以上）专属效果注册表。
 *
 * <p>用于丰富元素链的多元素组合类型：不仅只有二元专属效果，
 * 三元、四元等组合也可以拥有独立名称与强化效果。</p>
 */
public final class MultiComboEffectRegistry {

    private static final Map<String, MultiComboEffect> EFFECTS = new HashMap<>();

    static {
        // ===== 三元组合 =====
        add(List.of(ElementType.SOLAR, ElementType.TIDE, ElementType.STASIS),
                "霜炎爆", "Frostflare", ReactionType.DAMAGE, 16f, 0, 3f, 1f);
        add(List.of(ElementType.ARC, ElementType.TIDE, ElementType.STASIS),
                "雷暴冻结", "Stormfreeze", ReactionType.CONTROL, 0f, 100, 3f, 1f);
        add(List.of(ElementType.VOID, ElementType.MISTFLOW, ElementType.SINKSTAR),
                "虚空星爆", "Void Starburst", ReactionType.DAMAGE, 18f, 0, 3.5f, 1f);
        add(List.of(ElementType.MOON, ElementType.VOID, ElementType.SINKSTAR),
                "月结晶", "Moon Crystal", ReactionType.SHIELD, 0f, 240, 0f, 3f);
        add(List.of(ElementType.MOON, ElementType.ARC, ElementType.TIDE),
                "月感电", "Moonvolt", ReactionType.DAMAGE, 16f, 0, 2.5f, 1f);
        add(List.of(ElementType.PRISM, ElementType.SOLAR, ElementType.VOID),
                "棱镜湮灭", "Prismatic Annihilation", ReactionType.DAMAGE, 20f, 0, 3f, 1f);
        add(List.of(ElementType.PRISM, ElementType.ARC, ElementType.STASIS),
                "棱镜霜雷", "Prismatic Frostvolt", ReactionType.CONTROL, 0f, 120, 3f, 1f);
        add(List.of(ElementType.SOLAR, ElementType.STASIS, ElementType.STRAND),
                "灰烬束缚", "Ashbind", ReactionType.DAMAGE, 14f, 0, 2.5f, 1f);
        add(List.of(ElementType.ARC, ElementType.VOID, ElementType.MOON),
                "三相暗雷", "Trinity Darkvolt", ReactionType.DAMAGE, 18f, 0, 3f, 1f);
        add(List.of(ElementType.SOLAR, ElementType.ARC, ElementType.MISTFLOW),
                "日雷风爆", "Solstorm", ReactionType.DIFFUSION, 0f, 0, 4f, 1f);

        // ===== 四元组合 =====
        add(List.of(ElementType.SOLAR, ElementType.ARC, ElementType.VOID, ElementType.TIDE),
                "元素大融合", "Elemental Fusion", ReactionType.DAMAGE, 26f, 0, 3.5f, 1f);
        add(List.of(ElementType.MOON, ElementType.VOID, ElementType.STRAND, ElementType.SINKSTAR),
                "月蚀星缚", "Eclipsestar Bind", ReactionType.SHIELD, 0f, 280, 0f, 3f);
        add(List.of(ElementType.PRISM, ElementType.SOLAR, ElementType.ARC, ElementType.STASIS),
                "棱镜烈冰风暴", "Prismatic Froststorm", ReactionType.CONTROL, 0f, 140, 4f, 1f);
        add(List.of(ElementType.SOLAR, ElementType.ARC, ElementType.VOID, ElementType.MOON),
                "四相原力", "Quad Primal", ReactionType.DAMAGE, 28f, 0, 4f, 1f);
    }

    private MultiComboEffectRegistry() {
    }

    private static void add(List<ElementType> elements, String cn, String en,
                            ReactionType type, float damage, int duration, float radius, float intensity) {
        List<ElementType> copy = new ArrayList<>(elements);
        copy.sort(Comparator.comparing(ElementType::getId));
        MultiComboEffect effect = new MultiComboEffect(List.copyOf(copy), cn, en, type, damage, duration, radius, intensity);
        EFFECTS.put(key(copy), effect);
    }

    private static String key(List<ElementType> elements) {
        StringBuilder sb = new StringBuilder();
        for (ElementType element : elements) {
            if (sb.length() > 0) {
                sb.append('_');
            }
            sb.append(element.getId());
        }
        return sb.toString();
    }

    /** 查找链中所有已覆盖的多元素组合效果 */
    public static List<MultiComboEffect> findFor(Collection<ElementType> available) {
        List<MultiComboEffect> result = new ArrayList<>();
        for (MultiComboEffect effect : EFFECTS.values()) {
            if (available.containsAll(effect.elements())) {
                result.add(effect);
            }
        }
        return result;
    }

    public static Collection<MultiComboEffect> getAll() {
        return EFFECTS.values();
    }
}
