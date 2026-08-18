package org.tp.tcdex.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tp.tcdex.Tcdex;

import java.util.concurrent.CompletableFuture;

/**
 * TCDEX 数据生成入口（./gradlew runData）。
 *
 * <p>生成内容（输出到 src/generated/resources，随构建打包）：
 * <ul>
 *   <li>伤害类型 data/tcdex/damage_type/*.json（RegistrySetBuilder）</li>
 *   <li>伤害类型 tag data/minecraft/tags/damage_type/bypasses_invulnerability.json</li>
 *   <li>语言文件 assets/tcdex/lang/en_us.json / zh_cn.json</li>
 *   <li>物品模型 assets/tcdex/models/item/*.json</li>
 *   <li>匠魂词条配方 data/tcdex/recipes/tools/modifiers/**</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = Tcdex.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModDataGen {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        PackOutput output = gen.getPackOutput();
        ExistingFileHelper helper = event.getExistingFileHelper();

        // 把 damage type 注册内容构建进 lookup，供 tag provider 引用 tcdex:scorch 等
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider()
                .thenApply(provider -> ModDamageTypeProvider.BUILDER.buildPatch(
                        RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY), provider));

        // ===== 服务端 =====
        gen.addProvider(event.includeServer(), new ModDamageTypeProvider(output, lookup));
        gen.addProvider(event.includeServer(), new ModDamageTypeTagsProvider(output, lookup));
        gen.addProvider(event.includeServer(), new ModRecipeProvider(output));

        // ===== 客户端 =====
        gen.addProvider(event.includeClient(), new ModLanguageProvider(output, "en_us"));
        gen.addProvider(event.includeClient(), new ModLanguageProvider(output, "zh_cn"));
        gen.addProvider(event.includeClient(), new ModItemModelProvider(output, helper));
    }
}
