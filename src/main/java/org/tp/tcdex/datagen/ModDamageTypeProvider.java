package org.tp.tcdex.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import org.tp.tcdex.Tcdex;
import org.tp.tcdex.damage.ModDamageSources;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * 生成 TCDEX 自定义伤害类型（data/tcdex/damage_type/*.json）。
 *
 * <p>全部为 {@code scaling = when_caused_by_living_non_player, effects = hurt}；
 * 纯粹/灼烧 DoT 无疲劳（exhaustion 0），其余 0.1。</p>
 */
public class ModDamageTypeProvider extends DatapackBuiltinEntriesProvider {

    public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(Registries.DAMAGE_TYPE, bootstrap -> {
                // 纯粹伤害：无视护甲/无敌帧/魔法保护（毕业词条真伤用）
                bootstrap.register(ModDamageSources.PURE_DAMAGE_TYPE,
                        new DamageType("tcdex.pure", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.0f));
                // 动能伤害：无元素词条时匠魂武器的攻击类型
                bootstrap.register(ModDamageSources.KINETIC_DAMAGE_TYPE,
                        new DamageType("tcdex.kinetic", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f));
                // 灼烧 DoT：带 bypasses_invulnerability tag（见 ModDamageTypeTagsProvider），无视无敌帧稳定结算
                bootstrap.register(ModDamageSources.SCORCH_DAMAGE_TYPE,
                        new DamageType("tcdex.scorch", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.0f));
                // 元素伤害（死亡消息区分元素）
                bootstrap.register(ModDamageSources.SOLAR_DAMAGE_TYPE,
                        new DamageType("tcdex.solar", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f));
                bootstrap.register(ModDamageSources.ARC_DAMAGE_TYPE,
                        new DamageType("tcdex.arc", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f));
                bootstrap.register(ModDamageSources.VOID_DAMAGE_TYPE,
                        new DamageType("tcdex.void", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f));
                bootstrap.register(ModDamageSources.STASIS_DAMAGE_TYPE,
                        new DamageType("tcdex.stasis", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f));
                bootstrap.register(ModDamageSources.STRAND_DAMAGE_TYPE,
                        new DamageType("tcdex.strand", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f));
                bootstrap.register(ModDamageSources.SINKSTAR_DAMAGE_TYPE,
                        new DamageType("tcdex.sinkstar", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f));
                bootstrap.register(ModDamageSources.MISTFLOW_DAMAGE_TYPE,
                        new DamageType("tcdex.mistflow", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f));
                bootstrap.register(ModDamageSources.TIDE_DAMAGE_TYPE,
                        new DamageType("tcdex.tide", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f));
                bootstrap.register(ModDamageSources.PRISM_DAMAGE_TYPE,
                        new DamageType("tcdex.prism", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f));
            });

    public ModDamageTypeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, BUILDER, Set.of(Tcdex.MODID));
    }
}
