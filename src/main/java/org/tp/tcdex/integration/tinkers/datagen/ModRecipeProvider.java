package org.tp.tcdex.integration.tinkers.datagen;

import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import org.tp.tcdex.Tcdex;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 生成匠魂词条配方（data/tcdex/recipes/tools/modifiers/**）。
 *
 * <p>匠魂词条配方为自定义类型（tconstruct:modifier / tconstruct:modifier_salvage），
 * 不走 vanilla RecipeProvider，这里直接输出 JSON，保证与手写产物一致且无失效引用。</p>
 */
public class ModRecipeProvider implements DataProvider {

    private final PackOutput output;

    public ModRecipeProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        Map<ResourceLocation, JsonObject> recipes = new LinkedHashMap<>();

        // 急切刀锋：升级（末影珍珠）/ 拆解返还
        recipes.put(ResourceLocation.fromNamespaceAndPath(Tcdex.MODID, "tools/modifiers/upgrade/eager_edge"),
                modifierUpgrade("tcdex:eager_edge", "minecraft:ender_pearl"));
        recipes.put(ResourceLocation.fromNamespaceAndPath(Tcdex.MODID, "tools/modifiers/salvage/upgrade/eager_edge"),
                modifierSalvage("tcdex:eager_edge"));

        // 元素精通：升级（附魔之瓶）/ 拆解返还
        recipes.put(ResourceLocation.fromNamespaceAndPath(Tcdex.MODID, "tools/modifiers/upgrade/elemental_mastery"),
                modifierUpgrade("tcdex:elemental_mastery", "minecraft:experience_bottle"));
        recipes.put(ResourceLocation.fromNamespaceAndPath(Tcdex.MODID, "tools/modifiers/salvage/upgrade/elemental_mastery"),
                modifierSalvage("tcdex:elemental_mastery"));

        PackOutput.PathProvider pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "recipes");
        CompletableFuture<?>[] futures = recipes.entrySet().stream().map(entry ->
                DataProvider.saveStable(cache, entry.getValue(), pathProvider.json(entry.getKey()))
        ).toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures);
    }

    @Override
    public String getName() {
        return "TCDEX Modifier Recipes";
    }

    /** 近战工具（排除一次性工具）基础 JSON 片段 */
    private static JsonObject meleeTools() {
        JsonObject subtracted = new JsonObject();
        subtracted.addProperty("tag", "tconstruct:modifiable/single_use");
        JsonObject base = new JsonObject();
        base.addProperty("tag", "tconstruct:modifiable/melee");
        JsonObject difference = new JsonObject();
        difference.addProperty("type", "forge:difference");
        difference.add("subtracted", subtracted);
        difference.add("base", base);
        return difference;
    }

    /** tconstruct:modifier 升级配方：近战工具 + 材料 → 词条（消耗 1 升级槽） */
    private static JsonObject modifierUpgrade(String modifierId, String ingredientItem) {
        JsonObject tools = meleeTools();
        JsonObject input = new JsonObject();
        input.addProperty("item", ingredientItem);
        JsonObject slots = new JsonObject();
        slots.addProperty("upgrades", 1);
        JsonObject json = new JsonObject();
        json.addProperty("type", "tconstruct:modifier");
        json.addProperty("allow_crystal", true);
        json.add("inputs", jsonArray(input));
        json.addProperty("level", 1);
        json.addProperty("result", modifierId);
        json.add("slots", slots);
        json.add("tools", tools);
        addTinkersCondition(json);
        return json;
    }

    /** tconstruct:modifier_salvage 拆解配方：返还升级槽 */
    private static JsonObject modifierSalvage(String modifierId) {
        JsonObject tools = meleeTools();
        JsonObject slots = new JsonObject();
        slots.addProperty("upgrades", 1);
        JsonObject json = new JsonObject();
        json.addProperty("type", "tconstruct:modifier_salvage");
        json.addProperty("modifier", modifierId);
        json.add("slots", slots);
        json.add("tools", tools);
        addTinkersCondition(json);
        return json;
    }

    /** 添加 Forge mod_loaded 条件：未安装 Tinkers 时跳过该配方 */
    private static void addTinkersCondition(JsonObject json) {
        com.google.gson.JsonArray conditions = new com.google.gson.JsonArray();
        JsonObject condition = new JsonObject();
        condition.addProperty("type", "forge:mod_loaded");
        condition.addProperty("modid", "tconstruct");
        conditions.add(condition);
        json.add("conditions", conditions);
    }

    private static com.google.gson.JsonArray jsonArray(JsonObject... elements) {
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        for (JsonObject element : elements) {
            arr.add(element);
        }
        return arr;
    }
}
