package org.tp.tcdex.element;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 元素加权随机工具。
 */
public final class ElementWeightHelper {

    private ElementWeightHelper() {
    }

    /** 按权重随机；权重全 0 时退化为等概率（排除 Prism / Tide / Moon 等默认不参与随机的元素） */
    public static ElementType weightedRoll(RandomSource random, Map<ElementType, Integer> weights) {
        int total = 0;
        for (Integer weight : weights.values()) {
            total += weight;
        }
        if (total <= 0) {
            List<ElementType> candidates = new ArrayList<>();
            for (ElementType type : ElementType.values()) {
                if (type != ElementType.PRISM && type != ElementType.TIDE && type != ElementType.MOON) {
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
}
