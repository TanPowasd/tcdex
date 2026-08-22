package org.tp.tcdex.damage;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import org.tp.tcdex.Tcdex;
import org.tp.tcdex.element.ElementType;

/**
 * TCDEX 自定义伤害类型。
 *
 * <p>数据定义位于 data/tcdex/damage_type/*.json。</p>
 */
public final class ModDamageSources {

    /** 纯粹伤害：无视护甲/无敌帧/魔法保护/效果（毕业词条真伤用） */
    public static final ResourceKey<DamageType> PURE_DAMAGE_TYPE = ResourceKey.create(
            Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Tcdex.MODID, "pure"));

    /** 动能伤害：无元素词条时，匠魂武器的攻击伤害标记为动能类型（命运2 动能武器） */
    public static final ResourceKey<DamageType> KINETIC_DAMAGE_TYPE = ResourceKey.create(
            Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Tcdex.MODID, "kinetic"));

    /** 元素伤害：动能武器打上元素词条后，攻击伤害整体转化为对应元素伤害 */
    public static final ResourceKey<DamageType> SOLAR_DAMAGE_TYPE = elementKey("solar");
    public static final ResourceKey<DamageType> ARC_DAMAGE_TYPE = elementKey("arc");
    public static final ResourceKey<DamageType> VOID_DAMAGE_TYPE = elementKey("void");
    public static final ResourceKey<DamageType> STASIS_DAMAGE_TYPE = elementKey("stasis");
    public static final ResourceKey<DamageType> STRAND_DAMAGE_TYPE = elementKey("strand");
    public static final ResourceKey<DamageType> MOON_DAMAGE_TYPE = elementKey("moon");
    public static final ResourceKey<DamageType> SINKSTAR_DAMAGE_TYPE = elementKey("sinkstar");
    public static final ResourceKey<DamageType> MISTFLOW_DAMAGE_TYPE = elementKey("mistflow");
    public static final ResourceKey<DamageType> TIDE_DAMAGE_TYPE = elementKey("tide");
    public static final ResourceKey<DamageType> PRISM_DAMAGE_TYPE = elementKey("prism");

    /**
     * 灼烧 DoT 伤害：带 bypasses_invulnerability tag（无视无敌帧）。
     * 保证每 tick 稳定结算，且不与玩家攻击互相吞无敌帧。
     */
    public static final ResourceKey<DamageType> SCORCH_DAMAGE_TYPE = ResourceKey.create(
            Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Tcdex.MODID, "scorch"));

    private ModDamageSources() {
    }

    private static ResourceKey<DamageType> elementKey(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Tcdex.MODID, name));
    }

    /** 构造"纯粹伤害"伤害源（以攻击者为来源） */
    public static DamageSource pure(LivingEntity attacker) {
        return new DamageSource(
                attacker.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(PURE_DAMAGE_TYPE),
                attacker);
    }

    /** 构造"动能伤害"伤害源（以攻击者为来源） */
    public static DamageSource kinetic(LivingEntity attacker) {
        return new DamageSource(
                attacker.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(KINETIC_DAMAGE_TYPE),
                attacker);
    }

    /** 判断伤害源是否为 TCDEX 元素伤害 */
    public static boolean isElementDamage(net.minecraft.world.damagesource.DamageSource source) {
        return source.is(SOLAR_DAMAGE_TYPE) || source.is(ARC_DAMAGE_TYPE) || source.is(VOID_DAMAGE_TYPE)
                || source.is(STASIS_DAMAGE_TYPE) || source.is(STRAND_DAMAGE_TYPE)
                || source.is(MOON_DAMAGE_TYPE)
                || source.is(SINKSTAR_DAMAGE_TYPE) || source.is(MISTFLOW_DAMAGE_TYPE)
                || source.is(TIDE_DAMAGE_TYPE) || source.is(PRISM_DAMAGE_TYPE);
    }

    /** 判断伤害源是否为 TCDEX 自定义伤害类型（元素/动能/纯粹/灼烧 DoT） */
    public static boolean isTcdexDamage(net.minecraft.world.damagesource.DamageSource source) {
        return isElementDamage(source)
                || source.is(KINETIC_DAMAGE_TYPE)
                || source.is(PURE_DAMAGE_TYPE)
                || source.is(SCORCH_DAMAGE_TYPE);
    }

    /** 构造对应元素的伤害源（以攻击者为来源，死亡消息区分元素） */
    public static DamageSource element(LivingEntity attacker, ElementType element) {
        ResourceKey<DamageType> key = switch (element) {
            case SOLAR -> SOLAR_DAMAGE_TYPE;
            case ARC -> ARC_DAMAGE_TYPE;
            case VOID -> VOID_DAMAGE_TYPE;
            case STASIS -> STASIS_DAMAGE_TYPE;
            case STRAND -> STRAND_DAMAGE_TYPE;
            case MOON -> MOON_DAMAGE_TYPE;
            case SINKSTAR -> SINKSTAR_DAMAGE_TYPE;
            case MISTFLOW -> MISTFLOW_DAMAGE_TYPE;
            case TIDE -> TIDE_DAMAGE_TYPE;
            case PRISM -> PRISM_DAMAGE_TYPE;
        };
        return new DamageSource(
                attacker.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key),
                attacker);
    }

    /** 从伤害源解析对应的 TCDEX 元素；非 TCDEX 元素伤害返回 null */
    @javax.annotation.Nullable
    public static ElementType getElement(DamageSource source) {
        if (source.is(SOLAR_DAMAGE_TYPE)) return ElementType.SOLAR;
        if (source.is(ARC_DAMAGE_TYPE)) return ElementType.ARC;
        if (source.is(VOID_DAMAGE_TYPE)) return ElementType.VOID;
        if (source.is(STASIS_DAMAGE_TYPE)) return ElementType.STASIS;
        if (source.is(STRAND_DAMAGE_TYPE)) return ElementType.STRAND;
        if (source.is(MOON_DAMAGE_TYPE)) return ElementType.MOON;
        if (source.is(SINKSTAR_DAMAGE_TYPE)) return ElementType.SINKSTAR;
        if (source.is(MISTFLOW_DAMAGE_TYPE)) return ElementType.MISTFLOW;
        if (source.is(TIDE_DAMAGE_TYPE)) return ElementType.TIDE;
        if (source.is(PRISM_DAMAGE_TYPE)) return ElementType.PRISM;
        return null;
    }

    /** 构造灼烧 DoT 伤害源（无视无敌帧；来源为实体自身） */
    public static DamageSource scorch(LivingEntity entity) {
        return new DamageSource(
                entity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SCORCH_DAMAGE_TYPE),
                entity);
    }
}
