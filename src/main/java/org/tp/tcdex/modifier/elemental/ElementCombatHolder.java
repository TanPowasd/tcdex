package org.tp.tcdex.modifier.elemental;

import org.tp.tcdex.element.ElementType;

/**
 * 实体失衡 / 破绽 / 元素适应能力接口。
 */
public interface ElementCombatHolder {

    float getImbalance();

    void addImbalance(float amount);

    void resetImbalance();

    int getBreakTicks();

    void setBreakTicks(int ticks);

    float getElementAdaptation(ElementType type);

    void addElementAdaptation(ElementType type, float amount);
}
