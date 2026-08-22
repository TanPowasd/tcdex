package org.tp.tcdex.element;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.tp.tcdex.api.IElementShieldProvider;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 元素系统统一门面（Facade）。
 *
 * <p>重构后内部按职责拆分为：</p>
 * <ul>
 *   <li>{@link ElementResistanceManager}：元素抗性 / 弱点</li>
 *   <li>{@link ElementShieldManager}：元素护盾、破盾效率、护盾分配</li>
 *   <li>{@link ElementMonsterAttackManager}：怪物元素攻击</li>
 *   <li>{@link ElementRegistry}：元素定义注册表</li>
 * </ul>
 *
 * <p>保留原有静态方法入口，避免大范围改动调用方；后续可逐步迁移到直接使用各 Manager。</p>
 */
public final class ElementManager {

    /** 元素环形克制表：每个元素克制下一个元素 */
    private static final ElementType[] ELEMENT_CYCLE = {
            ElementType.SOLAR,
            ElementType.ARC,
            ElementType.VOID,
            ElementType.STASIS,
            ElementType.STRAND,
            ElementType.SINKSTAR,
            ElementType.MISTFLOW
    };

    /** 元素充能随机权重（默认各 1，0 = 不会随机到该元素；棱镜/潮汐/月不可通过元素充能获得） */
    private static final Map<ElementType, Integer> ELEMENT_WEIGHTS = new EnumMap<>(ElementType.class);

    static {
        for (ElementType type : ElementType.values()) {
            ELEMENT_WEIGHTS.put(type, type == ElementType.PRISM || type == ElementType.TIDE || type == ElementType.MOON ? 0 : 1);
        }
    }

    /** 元素附着量自然衰减速度（每 tick 减少量；默认 0.01） */
    private static float auraDecayPerTick = 0.01f;

    private ElementManager() {
    }

    // ===== 元素克制 =====

    public static ElementType getCounterElement(ElementType element) {
        for (int i = 0; i < ELEMENT_CYCLE.length; i++) {
            if (ELEMENT_CYCLE[i] == element) {
                return ELEMENT_CYCLE[(i + 1) % ELEMENT_CYCLE.length];
            }
        }
        return null;
    }

    public static boolean isCounterElement(ElementType shield, ElementType attack) {
        ElementType counter = getCounterElement(shield);
        return counter != null && counter == attack;
    }

    // ===== 附着衰减 =====

    public static float getAuraDecayPerTick() {
        return auraDecayPerTick;
    }

    public static void setAuraDecayPerTick(float decay) {
        auraDecayPerTick = Math.max(0.0f, decay);
    }

    // ===== 元素 ID 解析 =====

    public static ElementType parseElement(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        for (ElementType type : ElementType.values()) {
            if (type.getId().equals(id)) {
                return type;
            }
        }
        return null;
    }

    // ===== 元素抗性 / 弱点 =====

    public static void reloadResistances(List<? extends String> entries) {
        ElementResistanceManager.reload(entries);
    }

    public static void registerResistance(String entityId, ElementType element, float multiplier) {
        ElementResistanceManager.registerResistance(entityId, element, multiplier);
    }

    public static float getResistance(LivingEntity entity, ElementType element) {
        return ElementResistanceManager.getResistance(entity, element);
    }

    // ===== 元素护盾 =====

    public static void reloadShieldEfficiencyTable(List<? extends String> entries) {
        ElementShieldManager.reloadShieldEfficiencyTable(entries);
    }

    public static float getShieldEfficiency(ElementType shield, ElementType attack) {
        return ElementShieldManager.getShieldEfficiency(shield, attack);
    }

    public static void reloadShieldConfig(List<? extends String> blacklist, Map<ElementType, Integer> weights) {
        ElementShieldManager.reloadShieldConfig(blacklist, weights);
    }

    public static void registerShieldProvider(IElementShieldProvider provider) {
        ElementShieldManager.registerShieldProvider(provider);
    }

    public static ElementType getProviderShieldElement(LivingEntity entity) {
        return ElementShieldManager.getProviderShieldElement(entity);
    }

    public static void addShieldBlacklist(String entityId) {
        ElementShieldManager.addShieldBlacklist(entityId);
    }

    public static void removeShieldBlacklist(String entityId) {
        ElementShieldManager.removeShieldBlacklist(entityId);
    }

    public static boolean isShieldBlacklisted(String entityId) {
        return ElementShieldManager.isShieldBlacklisted(entityId);
    }

    public static boolean isShieldBlacklisted(LivingEntity entity) {
        return ElementShieldManager.isShieldBlacklisted(entity);
    }

    public static void setShieldWeight(ElementType element, int weight) {
        ElementShieldManager.setShieldWeight(element, weight);
    }

    public static Map<ElementType, Integer> getShieldWeights() {
        return ElementShieldManager.getShieldWeights();
    }

    public static ElementType rollShieldElement(net.minecraft.util.RandomSource random) {
        return ElementShieldManager.rollShieldElement(random);
    }

    public static ElementType rollShieldElement(net.minecraft.util.RandomSource random, ElementType boosted) {
        return ElementShieldManager.rollShieldElement(random, boosted);
    }

    // ===== 怪物元素攻击 =====

    public static boolean isAttackEnabled() {
        return ElementMonsterAttackManager.isAttackEnabled();
    }

    public static float getAttackChance() {
        return ElementMonsterAttackManager.getAttackChance();
    }

    public static void setAttackChance(float chance) {
        ElementMonsterAttackManager.setAttackChance(chance);
    }

    public static void reloadAttackConfig(boolean enabled, double chance) {
        ElementMonsterAttackManager.reloadAttackConfig(enabled, chance);
    }

    // ===== 元素充能随机权重 =====

    public static void reloadElementWeights(Map<ElementType, Integer> weights) {
        ELEMENT_WEIGHTS.clear();
        if (weights != null && !weights.isEmpty()) {
            ELEMENT_WEIGHTS.putAll(weights);
        }
        for (ElementType type : ElementType.values()) {
            ELEMENT_WEIGHTS.putIfAbsent(type, type == ElementType.PRISM || type == ElementType.TIDE || type == ElementType.MOON ? 0 : 1);
        }
    }

    public static void setElementWeight(ElementType element, int weight) {
        ELEMENT_WEIGHTS.put(element, Math.max(0, weight));
    }

    public static Map<ElementType, Integer> getElementWeights() {
        return new EnumMap<>(ELEMENT_WEIGHTS);
    }

    public static ElementType rollElement(net.minecraft.util.RandomSource random) {
        return ElementWeightHelper.weightedRoll(random, ELEMENT_WEIGHTS);
    }

    // ===== 通用 =====

    public static boolean isMonster(LivingEntity entity) {
        return entity instanceof net.minecraft.world.entity.monster.Enemy
                || entity.getType().getCategory() == net.minecraft.world.entity.MobCategory.MONSTER;
    }
}
