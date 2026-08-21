package org.tp.tcdex.api;

import net.minecraft.world.entity.LivingEntity;
import org.tp.tcdex.element.ElementManager;
import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.modifier.elemental.IElementalEntity;
import java.util.Map;

/**
 * TCDEX 元素系统对外 API。
 *
 * <p>附属 mod 可通过此类：注册自定义护盾提供器、维护护盾黑名单、调整元素盾生成权重、
 * 查询元素抗性/实体元素数据、获取元素攻击 hook 用于词条联动。</p>
 */
public final class TcdexElementAPI {

    private TcdexElementAPI() {
    }

    // ===== 护盾提供器 =====

    /** 注册自定义护盾提供器（分配优先级高于静态表/随机，低于黑名单） */
    public static void registerShieldProvider(IElementShieldProvider provider) {
        ElementManager.registerShieldProvider(provider);
    }

    // ===== 护盾黑名单（绝对不带盾，兼容其他 mod 生物） =====

    /** 将某生物加入护盾黑名单（entity id，如 "minecraft:slime" 或 "othermod:boss"） */
    public static void addShieldBlacklist(String entityId) {
        ElementManager.addShieldBlacklist(entityId);
    }

    /** 将某生物移出护盾黑名单 */
    public static void removeShieldBlacklist(String entityId) {
        ElementManager.removeShieldBlacklist(entityId);
    }

    /** 查询生物是否在护盾黑名单中（entity id 匹配） */
    public static boolean isShieldBlacklisted(String entityId) {
        return ElementManager.isShieldBlacklisted(entityId);
    }

    /** 查询实体是否被判定为不带护盾（按实体注册名匹配黑名单） */
    public static boolean isShieldBlacklisted(LivingEntity entity) {
        return ElementManager.isShieldBlacklisted(entity);
    }

    // ===== 元素盾生成权重 =====

    /** 设置某元素的护盾生成权重（0 = 不生成该元素盾） */
    public static void setShieldWeight(ElementType element, int weight) {
        ElementManager.setShieldWeight(element, weight);
    }

    /** 获取当前护盾权重表 */
    public static Map<ElementType, Integer> getShieldWeights() {
        return ElementManager.getShieldWeights();
    }

    // ===== 元素充能随机权重 =====

    /** 设置某元素在元素充能随机分配中的权重（0 = 不会随机到该元素） */
    public static void setElementWeight(ElementType element, int weight) {
        ElementManager.setElementWeight(element, weight);
    }

    /** 获取元素充能随机权重表 */
    public static Map<ElementType, Integer> getElementWeights() {
        return ElementManager.getElementWeights();
    }

    // ===== 元素怪物：怪物元素攻击（与元素护盾同源分配，独立于护盾状态） =====

    /** 获取怪物的元素攻击类型（护盾分配时固化，护盾被打破后保留；无护盾/黑名单生物返回 null = 无元素攻击） */
    @javax.annotation.Nullable
    public static ElementType getMonsterAttackElement(LivingEntity entity) {
        return IElementalEntity.of(entity).getAttackElement();
    }

    /** 怪物元素攻击是否开启 */
    public static boolean isMonsterAttackEnabled() {
        return ElementManager.isAttackEnabled();
    }

    /** 设置怪物元素攻击命中概率（0-1，1 = 每次命中必施加） */
    public static void setMonsterAttackChance(float chance) {
        ElementManager.setAttackChance(chance);
    }

    // ===== 元素抗性 / 弱点（Add 包注册，优先级低于配置文件） =====

    /** 注册额外元素抗性/弱点：entityId 如 "iceandfire:fire_dragon" */
    public static void registerResistance(String entityId, ElementType element, float multiplier) {
        ElementManager.registerResistance(entityId, element, multiplier);
    }

    // ===== 查询 =====

    /** 获取实体对某元素的伤害倍率（元素抗性/弱点） */
    public static float getResistance(LivingEntity entity, ElementType element) {
        return ElementManager.getResistance(entity, element);
    }

    /** 获取实体的元素状态数据（护盾/元素状态），附属 mod 可直接读写 */
    public static IElementalEntity getEntityElementData(LivingEntity entity) {
        return IElementalEntity.of(entity);
    }
}
