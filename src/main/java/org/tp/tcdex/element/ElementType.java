package org.tp.tcdex.element;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

/**
 * 命运2元素伤害类型。
 *
 * <p>全元素关键词化：命中只叠加元素状态（stacksPerHit / stateDuration），
 * 效果全部由状态系统结算——烈日灼烧 DoT（doTPerStack）+ Ignite 引爆、
 * 冰影渐进减速/冻结/Shatter、虚空 Volatile 爆炸、电弧 Jolt 连锁、缚丝 Sever 减伤。</p>
 */
public enum ElementType {

    /** 烈日：命中 +25 层，持续灼烧（每 tick 层数×0.01×抗性），满 100 Ignite 引爆 */
    SOLAR("solar", ParticleTypes.FLAME, 25f, 100, 0.01f),
    /** 电弧：标记型，受击时 Jolt 连锁闪电（连锁目标致盲），击杀带电弧标记目标获得强化（Amplified） */
    ARC("arc", ParticleTypes.ELECTRIC_SPARK, 1f, 100, 0f),
    /** 虚空：标记型，受击时 Volatile 爆炸（10% 最大生命 AOE）+ 期间受击增伤（Weaken），击杀回血（Devour） */
    VOID("void", ParticleTypes.SCULK_SOUL, 1f, 100, 0f),
    /** 冰影：命中 +50 层，渐进减速（≥50 缓慢 I / ≥75 缓慢 II），满 100 冻结，冻结中受击 Shatter +50% */
    STASIS("stasis", ParticleTypes.SNOWFLAKE, 50f, 100, 0f),
    /** 缚丝：叠层（+25/击），期间攻击者伤害 -40%（Sever），满 100 悬挂（Suspend），击杀带标记目标获得织甲（Woven Mail） */
    STRAND("strand", ParticleTypes.ENCHANT, 25f, 100, 0f);

    private final String id;
    private final ParticleOptions particle;
    /** 每击叠加的元素状态层数 */
    private final float stacksPerHit;
    /** 元素状态有效时长（tick） */
    private final int stateDuration;
    /** 灼烧 DoT：每 tick 伤害 = 当前层数 × 系数 × 元素抗性（0 = 无 DoT） */
    private final float doTPerStack;

    ElementType(String id, ParticleOptions particle, float stacksPerHit, int stateDuration, float doTPerStack) {
        this.id = id;
        this.particle = particle;
        this.stacksPerHit = stacksPerHit;
        this.stateDuration = stateDuration;
        this.doTPerStack = doTPerStack;
    }

    public String getId() {
        return id;
    }

    public ParticleOptions getParticle() {
        return particle;
    }

    public float getStacksPerHit() {
        return stacksPerHit;
    }

    public int getStateDuration() {
        return stateDuration;
    }

    public float getDoTPerStack() {
        return doTPerStack;
    }
}
