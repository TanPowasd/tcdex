package org.tp.tcdex.modifier.elemental;

import org.tp.tcdex.element.ElementType;

/**
 * 实体元素护盾 / 棱镜盾 / 元素攻击能力接口。
 */
public interface ElementShieldHolder {

    ElementType getShieldElement();

    float getShieldAmount();

    int getShieldLayers();

    void setShieldLayers(int layers);

    float consumeShield(float damage);

    float consumeShield(float damage, boolean permanent);

    void destroyShield();

    void setShield(ElementType element, float amount);

    void markShieldHit(long gameTime);

    long getShieldLastHurtTime();

    ElementType getAttackElement();
}
