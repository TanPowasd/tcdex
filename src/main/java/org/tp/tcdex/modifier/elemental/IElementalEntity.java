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
public interface IElementalEntity {

    /** 获取某元素当前层数 */
    float getElementStacks(ElementType type);

    /** 获取某元素剩余有效 tick */
    int getElementDuration(ElementType type);

    /**
     * 叠加元素状态：层数累加（封顶 100），时长取较大值刷新。
     *
     * @param type     元素类型
     * @param stacks   本次叠加的层数（烈日/冰影用；标记型元素传 1）
     * @param duration 状态有效时长（tick）
     */
    void addElementState(ElementType type, float stacks, int duration);

    /** 清除某元素状态 */
    void clearElementState(ElementType type);

    /** 获取全部元素状态（只读遍历用） */
    Map<ElementType, ElementStatus> getAllElementStates();

    // ===== 元素护盾（命运2 匹配元素破盾） =====

    /** 获取护盾元素（无护盾返回 null；首次访问懒加载初始化，查 ElementManager 护盾表） */
    ElementType getShieldElement();

    /** 获取剩余护盾值（0 = 无护盾；首次访问懒加载） */
    float getShieldAmount();

    /**
     * 护盾承受伤害：扣减护盾值。
     *
     * <p>护盾完全耗尽（归零）时清除护盾元素：棱镜盾（Boss）第一次完全破坏后不再回复，
     * 元素攻击一并失效（攻击元素 = 护盾元素，同源）。</p>
     *
     * @param damage 打入护盾的伤害（已按匹配效率换算）
     * @return 溢出伤害（护盾打穿后剩余部分），未打穿返回 0
     */
    float consumeShield(float damage);

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
