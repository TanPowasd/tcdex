package org.tp.tcdex.reaction;

/**
 * TCDEX 元素反应类型。
 *
 * <p>第一版以控制类为主，后续可扩展伤害/增幅/护盾等类型。</p>
 */
public enum ReactionType {
    /** 控制类：冻结、减速、麻痹、压制、恐惧等 */
    CONTROL,
    /** 伤害类：额外伤害 / AOE */
    DAMAGE,
    /** 增幅类：提高本次或后续伤害 */
    AMPLIFY,
    /** 护盾类：生成元素护盾 */
    SHIELD,
    /** 扩散类：把目标身上的元素附着扩散给周围敌人 */
    DIFFUSION
}
