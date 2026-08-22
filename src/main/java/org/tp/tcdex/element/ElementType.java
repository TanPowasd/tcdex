package org.tp.tcdex.element;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

/**
 * TCDEX 元素伤害类型。
 *
 * <p>包含命运2 风格关键词元素，以及新增的 TCDEX 七元素反应体系元素：
 * 烈日/电能/虚空/冰影/缚丝/月/罡流/水/落星。Prism 棱镜为 Boss 专属特殊元素，
 * 不参与常规七元素反应。</p>
 *
 * <p>全元素关键词化：命中只叠加元素状态（stacksPerHit / stateDuration），
 * 效果全部由状态系统结算——烈日灼烧 DoT（doTPerStack）+ Ignite 引爆、
 * 冰影渐进减速/冻结/Shatter、虚空 Volatile 爆炸/Weaken/Devour、
 * 电弧 Jolt 连锁/Blind/Amplified、缚丝 Sever/Suspend/Woven Mail、
 * 棱镜 Refract 折射溅射（命运2 Prismatic 风格）。</p>
 *
 * <p>auraPerHit 表示每次施加该元素时附加到目标身上的“元素附着量”，
 * 用于 TCDEX 元素反应系统（原神式附着/消耗模型）。</p>
 */
public enum ElementType {

    /** 烈日：命中 +25 层，持续灼烧（每 tick 层数×0.01×抗性），满 100 Ignite 引爆 */
    SOLAR("solar", ParticleTypes.FLAME, 25f, 100, 0.01f, 1.0f, 0xFFFF9A3C),
    /** 电能：标记型，受击时 Jolt 连锁闪电（连锁目标致盲），击杀带电弧标记目标获得强化（Amplified） */
    ARC("arc", ParticleTypes.ELECTRIC_SPARK, 1f, 100, 0f, 1.0f, 0xFF5CC8FF),
    /** 虚空：标记型，受击时 Volatile 爆炸（10% 最大生命 AOE）+ 期间受击增伤（Weaken），击杀回血（Devour） */
    VOID("void", ParticleTypes.SCULK_SOUL, 1f, 100, 0f, 1.0f, 0xFF9B59B6),
    /** 冰影：命中 +50 层，渐进减速（≥50 缓慢 I / ≥75 缓慢 II），满 100 冻结，冻结中受击 Shatter +50% */
    STASIS("stasis", ParticleTypes.SNOWFLAKE, 50f, 100, 0f, 1.0f, 0xFF7FD8E6),
    /** 缚丝：叠层（+25/击），期间攻击者伤害 -40%（Sever），满 100 悬挂（Suspend），击杀带标记目标获得织甲（Woven Mail） */
    STRAND("strand", ParticleTypes.ENCHANT, 25f, 100, 0f, 1.0f, 0xFF8FDB6A),
    /** 月：暗影元素，月蚀标记 + 持续暗影伤害，可被光能净化触发爆发，与棱镜有特殊暗面反应 */
    MOON("moon", ParticleTypes.SCULK_SOUL, 10f, 100, 0.01f, 1.0f, 0xFFC0C0FF),
    /** 棱镜：标记型（+1），受击时折射（Refract）——本击 25% 伤害溅射周围；棱镜攻击对所有元素护盾按匹配效率破盾（折射所有光 = 匹配所有盾）。不参与常规七元素反应 */
    PRISM("prism", ParticleTypes.FIREWORK, 1f, 100, 0f, 1.0f, 0xFFA78BFA),
    /** 落星：重力与星核，定位压制/聚怪/护盾；参与元素反应 */
    SINKSTAR("sinkstar", ParticleTypes.END_ROD, 1f, 100, 0f, 1.0f, 0xFF5B7DB1),
    /** 罡流：风与气流，定位扩散/位移/控制；参与元素反应 */
    MISTFLOW("mistflow", ParticleTypes.CLOUD, 1f, 100, 0f, 1.0f, 0xFFA8E6CF),
    /** 水（伪元素）：仅用于环境附着/反应，不进入七元素、不参与元素充能/护盾随机 */
    TIDE("tide", ParticleTypes.SPLASH, 1f, 100, 0f, 1.0f, 0xFF3B9EFF);

    private final String id;
    private final ParticleOptions particle;
    /** 每击叠加的元素状态层数 */
    private final float stacksPerHit;
    /** 元素状态有效时长（tick） */
    private final int stateDuration;
    /** 灼烧 DoT：每 tick 伤害 = 当前层数 × 系数 × 元素抗性（0 = 无 DoT） */
    private final float doTPerStack;
    /** 每次施加时附加到目标身上的元素附着量（用于元素反应） */
    private final float auraPerHit;
    /** TCDEX 元素色（HUD/护盾/词条显示统一使用） */
    private final int color;

    ElementType(String id, ParticleOptions particle, float stacksPerHit, int stateDuration, float doTPerStack, float auraPerHit, int color) {
        this.id = id;
        this.particle = particle;
        this.stacksPerHit = stacksPerHit;
        this.stateDuration = stateDuration;
        this.doTPerStack = doTPerStack;
        this.auraPerHit = auraPerHit;
        this.color = color;
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

    /** 每次施加时附加到目标身上的元素附着量（用于元素反应） */
    public float getAuraPerHit() {
        return auraPerHit;
    }

    /** TCDEX 元素色（ARGB） */
    public int getColor() {
        return color;
    }
}
