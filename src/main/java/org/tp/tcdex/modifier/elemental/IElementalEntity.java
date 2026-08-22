package org.tp.tcdex.modifier.elemental;

import net.minecraft.world.entity.LivingEntity;
import org.tp.tcdex.element.ElementType;

import java.util.Map;

/**
 * 元素状态注入接口：任何 LivingEntity 均可强制转换获得该接口（由 {@link org.tp.tcdex.mixin.LivingEntityElementalMixin} 注入）。
 *
 * <p>武器词条命中时通过 {@link #addElementState} 给目标叠加元素状态；
 * 状态由 Mixin 的 tick 结算（衰减/满层触发），受击联动见 {@link org.tp.tcdex.event.ElementalStateEvents}。</p>
 */
public interface IElementalEntity extends ElementStateHolder, ElementShieldHolder, ElementCombatHolder {

    /** 获取某元素当前层数 */
    float getElementStacks(ElementType type);

    /** 获取某元素剩余有效 tick */
    int getElementDuration(ElementType type);

    /**
     * 叠加元素状态：层数累加（封顶 100），时长取较大值刷新。
     * 同时会按 {@link ElementType#getAuraPerHit()} 增加该元素的附着量。
     *
     * @param type     元素类型
     * @param stacks   本次叠加的层数（烈日/冰影用；标记型元素传 1）
     * @param duration 状态有效时长（tick）
     */
    void addElementState(ElementType type, float stacks, int duration);

    /** 清除某元素状态（同时清除附着量与反应冷却记录） */
    void clearElementState(ElementType type);

    /** 获取全部元素状态（只读遍历用） */
    Map<ElementType, ElementStatus> getAllElementStates();

    // ===== 元素附着量（用于 TCDEX 元素反应） =====

    /** 获取某元素当前附着量 */
    float getAura(ElementType type);

    /** 直接增加某元素附着量（不改变命运2关键词层数/时长） */
    void addAuraAmount(ElementType type, float amount);

    /** 直接增加某元素附着量并设置/刷新状态时长 */
    void addAura(ElementType type, float amount, int duration);

    /**
     * 消耗某元素附着量，返回实际消耗值。
     *
     * @param type   元素类型
     * @param amount 希望消耗的附着量
     * @return 实际消耗的附着量（不会超过当前附着量）
     */
    float consumeAura(ElementType type, float amount);

    /** 获取某元素最近一次参与反应的世界时间（gameTime，0 = 从未参与） */
    long getLastReactionTime(ElementType type);

    /** 记录某元素最近一次参与反应的世界时间 */
    void markReaction(ElementType type, long gameTime);

    // ===== 破绽/失衡（原创战斗机制） =====

    /** 获取当前失衡值（0~100） */
    float getImbalance();

    /** 增加失衡值（超过 100 自动进入破绽状态） */
    void addImbalance(float amount);

    /** 重置失衡值 */
    void resetImbalance();

    /** 获取剩余破绽 tick（0 = 未破绽） */
    int getBreakTicks();

    /** 设置破绽剩余 tick */
    void setBreakTicks(int ticks);

    /** 是否处于破绽状态 */
    default boolean isBroken() {
        return getBreakTicks() > 0;
    }

    // ===== 元素适应（怪物逐渐抵抗常用元素） =====

    /** 获取某元素当前适应值（0~0.5，越高抗性越强） */
    float getElementAdaptation(ElementType type);

    /** 增加某元素适应值 */
    void addElementAdaptation(ElementType type, float amount);

    // ===== 元素护盾（命运2 匹配元素破盾） =====

    /** 获取护盾元素（无护盾返回 null；首次访问懒加载初始化，查 ElementManager 护盾表） */
    ElementType getShieldElement();

    /** 获取剩余护盾值（0 = 无护盾；首次访问懒加载） */
    float getShieldAmount();

    /** 获取剩余护盾层数（元素使徒多层护盾） */
    int getShieldLayers();

    /** 设置剩余护盾层数 */
    void setShieldLayers(int layers);

    /**
     * 护盾承受伤害：扣减护盾值（耗尽时清除护盾元素 = 永久破盾，等价于 {@code consumeShield(damage, true)}）。
     *
     * <p>护盾完全耗尽（归零）时清除护盾元素：棱镜盾（Boss）被棱镜伤害打穿后不再回复；
     * 元素攻击保留（攻击元素在分配时固化，与护盾状态无关）。</p>
     *
     * @param damage 打入护盾的伤害（已按匹配效率换算）
     * @return 溢出伤害（护盾打穿后剩余部分），未打穿返回 0
     */
    float consumeShield(float damage);

    /**
     * 护盾承受伤害：扣减护盾值，可指定打穿后是否永久失效。
     *
     * @param damage    打入护盾的伤害（已按匹配效率换算）
     * @param permanent 打穿后是否永久失效：true = 清除护盾元素（不再回复）；
     *                  false = 保留护盾元素（棱镜盾可脱战回复重新长满）
     * @return 溢出伤害（护盾打穿后剩余部分），未打穿返回 0
     */
    float consumeShield(float damage, boolean permanent);

    /** 破盾：清除护盾并重置初始化标记 */
    void destroyShield();

    /**
     * 直接设置护盾（生成时分配用）：设置护盾元素与护盾值，跳过护盾表懒加载。
     *
     * @param element 护盾元素（null = 无护盾）
     * @param amount  护盾值（&lt;=0 视为无护盾）
     */
    void setShield(ElementType element, float amount);

    // ===== 棱镜盾：脱战回复计时 =====

    /** 记录护盾受击时间（gameTime；棱镜盾受击重置脱战计时） */
    void markShieldHit(long gameTime);

    /** 获取护盾最近一次受击时间（gameTime，未受击过为 0） */
    long getShieldLastHurtTime();

    // ===== 元素攻击（与护盾同源分配，独立于护盾状态） =====

    /**
     * 获取该实体的元素攻击类型。
     *
     * <p>攻击元素在护盾分配时固化（与护盾元素同源），护盾被打破（耗尽/销毁）后
     * 仍然保留——元素攻击不随护盾消失。</p>
     *
     * @return 攻击元素（无护盾/黑名单生物为 null = 无元素攻击）
     */
    @javax.annotation.Nullable
    ElementType getAttackElement();

    /** 从任意生物实体获取元素状态接口（Mixin 注入，安全转换） */
    static IElementalEntity of(LivingEntity entity) {
        return (IElementalEntity) entity;
    }
}
