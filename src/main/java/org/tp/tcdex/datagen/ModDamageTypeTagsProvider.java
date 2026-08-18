package org.tp.tcdex.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.DamageTypeTagsProvider;
import net.minecraft.tags.DamageTypeTags;
import org.tp.tcdex.damage.ModDamageSources;

import java.util.concurrent.CompletableFuture;

/**
 * 生成 data/minecraft/tags/damage_type/bypasses_invulnerability.json：
 * 灼烧 DoT 无视无敌帧稳定结算（且不吞玩家攻击的无敌帧）。
 */
public class ModDamageTypeTagsProvider extends DamageTypeTagsProvider {

    public ModDamageTypeTagsProvider(PackOutput output,
                                     CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(DamageTypeTags.BYPASSES_INVULNERABILITY)
                .add(ModDamageSources.SCORCH_DAMAGE_TYPE);
    }
}
