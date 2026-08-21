package org.tp.tcdex.modifier.elemental;

/**
 * 实体身上的元素状态（Mixin 注入，运行时状态不存档）。
 *
 * <p>stacks 表示层数/标记强度：烈日/冰影为叠层（满 100 触发 Ignite/冻结），
 * 虚空/电弧/缚丝为标记（&gt;0 即生效）。duration 为剩余有效 tick，归零自动清除。</p>
 *
 * <p>aura 表示元素附着量，用于 TCDEX 元素反应系统（原神式附着/消耗模型）。
 * lastReactionTime 记录该元素最近一次参与反应的世界时间，用于反应冷却。</p>
 */
public class ElementStatus {

    /** 层数/标记（0~100） */
    public float stacks;

    /** 剩余有效 tick */
    public int duration;

    /** 元素附着量（用于元素反应；随时间衰减，触发反应时消耗） */
    public float aura;

    /** 该元素最近一次参与反应的世界时间（gameTime，0 = 从未参与） */
    public long lastReactionTime;

    /** 满层阈值（默认 100） */
    public static final int MAX_STACKS = 100;

    public ElementStatus(float stacks, int duration) {
        this.stacks = stacks;
        this.duration = duration;
        this.aura = 0.0f;
        this.lastReactionTime = 0L;
    }
}
