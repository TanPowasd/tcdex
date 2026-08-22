package org.tp.tcdex.chain;

/**
 * 连携系统支持的元素行为类型。
 *
 * <p>不同类型拥有不同连携贡献权重，用于体现“元素爆发/破盾”比普通攻击更容易积累连携。</p>
 */
public enum ElementActionType {

    /** 元素武器普通攻击 */
    MELEE(1.0f),
    /** 元素技能 / 法术 */
    SKILL(1.5f),
    /** 元素爆发 */
    BURST(3.0f),
    /** 触发元素反应 */
    REACTION(2.0f),
    /** 元素残响引爆 */
    ECHO(2.0f),
    /** 元素护盾破碎 */
    SHIELD_BREAK(3.0f);

    private final float weight;

    ElementActionType(float weight) {
        this.weight = weight;
    }

    public float getWeight() {
        return weight;
    }
}
