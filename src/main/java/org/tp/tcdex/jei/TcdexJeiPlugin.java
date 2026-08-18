package org.tp.tcdex.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.tp.tcdex.ModItems;
import org.tp.tcdex.Tcdex;

import java.util.List;

/**
 * JEI 集成：隐藏 my_love（仅可通过指令 /give 获得）。
 *
 * <p>JEI 为软依赖（mods.toml mandatory=false）：本类仅由 JEI 在启动扫描 @JeiPlugin 时加载，
 * JEI 未安装时不会被引用，安全。</p>
 */
@JeiPlugin
public class TcdexJeiPlugin implements IModPlugin {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Tcdex.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        jeiRuntime.getIngredientManager().removeIngredientsAtRuntime(
                VanillaTypes.ITEM_STACK,
                List.of(new ItemStack(ModItems.MY_LOVE.get())));
    }
}
