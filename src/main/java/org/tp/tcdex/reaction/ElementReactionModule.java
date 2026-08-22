package org.tp.tcdex.reaction;

/**
 * 元素反应模块。
 *
 * <p>类似匠魂词条 Hook 的思路：每种元素反应实现为一个模块，
 * 模块可以决定是否触发、触发后执行什么效果，并允许调整伤害/持续时间/范围。</p>
 *
 * <p>模块是内置的，不需要玩家给工具打词条；只要满足反应条件就会自动触发。</p>
 */
public interface ElementReactionModule {

    /** 该模块对应的反应定义 */
    ElementReaction getReaction();

    /** 是否允许触发（默认 true；子类可检查额外条件） */
    default boolean canTrigger(ReactionContext context) {
        return true;
    }

    /** 反应触发后的效果实现 */
    default void onTrigger(ReactionContext context) {
    }

    /** 调整反应伤害 */
    default float modifyDamage(ReactionContext context, float damage) {
        return damage;
    }

    /** 调整反应持续时间 */
    default int modifyDuration(ReactionContext context, int duration) {
        return duration;
    }

    /** 调整反应范围 */
    default float modifyRadius(ReactionContext context, float radius) {
        return radius;
    }

    /** 调整反应强度（聚怪力度/增幅等级等） */
    default float modifyIntensity(ReactionContext context, float intensity) {
        return intensity;
    }

    /** 调整反应冷却 */
    default int modifyCooldown(ReactionContext context, int cooldown) {
        return cooldown;
    }

    /** 调整反应附着量消耗 */
    default float modifyAuraCost(ReactionContext context, float auraCost) {
        return auraCost;
    }
}
