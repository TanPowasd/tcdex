package org.tp.tcdex.modifier.elemental;

/**
 * 实体身上的元素状态（Mixin 注入，运行时状态不存档）。
 *
 * <p>stacks 表示层数/标记强度：烈日/冰影为叠层（满 100 触发 Ignite/冻结），
 * 虚空/电弧/缚丝为标记（&gt;0 即生效）。duration 为剩余有效 tick，归零自动清除。</p>
 */
public class ElementStatus {

    /** 层数/标记（0~100） */
    public float stacks;

    /** 剩余有效 tick */
    public int duration;

    /** 满层阈值（默认 100） */
    public static final int MAX_STACKS = 100;

    public ElementStatus(float stacks, int duration) {
        this.stacks = stacks;
        this.duration = duration;
    }
}
