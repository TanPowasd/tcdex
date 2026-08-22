package org.tp.tcdex.modifier.elemental;

import org.tp.tcdex.element.ElementType;

import java.util.Map;

/**
 * 实体元素状态 / 附着量 / 反应冷却能力接口。
 */
public interface ElementStateHolder {

    float getElementStacks(ElementType type);

    int getElementDuration(ElementType type);

    void addElementState(ElementType type, float stacks, int duration);

    void clearElementState(ElementType type);

    Map<ElementType, ElementStatus> getAllElementStates();

    float getAura(ElementType type);

    void addAuraAmount(ElementType type, float amount);

    void addAura(ElementType type, float amount, int duration);

    float consumeAura(ElementType type, float amount);

    long getLastReactionTime(ElementType type);

    void markReaction(ElementType type, long gameTime);
}
