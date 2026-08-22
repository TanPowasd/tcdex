package org.tp.tcdex.chain;

import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.reaction.ReactionType;

import java.util.List;

/**
 * 多元素组合（三元及以上）连携专属效果。
 */
public record MultiComboEffect(List<ElementType> elements,
                               String nameCn, String nameEn,
                               ReactionType type,
                               float damage, int duration, float radius, float intensity) {

    public boolean matches(List<ElementType> available) {
        return available.containsAll(elements);
    }
}
