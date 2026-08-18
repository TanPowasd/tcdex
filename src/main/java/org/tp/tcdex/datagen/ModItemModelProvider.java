package org.tp.tcdex.datagen;

import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.tp.tcdex.Tcdex;

/**
 * 生成 assets/tcdex/models/item/*.json（均继承 item/generated）。
 */
public class ModItemModelProvider extends ItemModelProvider {

    public ModItemModelProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, Tcdex.MODID, helper);
    }

    @Override
    protected void registerModels() {
        singleTexture("my_love", mcLoc("item/generated"), "layer0", modLoc("item/my_love"));
        // 光之精华复用 my_love 纹理
        singleTexture("light_essence", mcLoc("item/generated"), "layer0", modLoc("item/my_love"));
    }
}
