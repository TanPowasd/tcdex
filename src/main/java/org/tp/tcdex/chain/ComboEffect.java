package org.tp.tcdex.chain;

import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.reaction.ReactionType;

/**
 * 连携二元组合专属效果定义。
 *
 * @param first      元素一
 * @param second     元素二
 * @param nameCn     中文名
 * @param nameEn     英文名
 * @param type       效果类型
 * @param damage     伤害类基础伤害
 * @param duration   持续类持续时间（tick）
 * @param radius     范围（格）
 * @param intensity  强度/等级
 */
public record ComboEffect(ElementType first, ElementType second,
                          String nameCn, String nameEn,
                          ReactionType type,
                          float damage, int duration, float radius, float intensity) {

    public boolean matches(ElementType a, ElementType b) {
        return (first == a && second == b) || (first == b && second == a);
    }
}
