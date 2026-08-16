package org.tp.tcdex;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.tp.tcdex.light.LightLevelManager;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = Tcdex.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER.comment("Whether to log the dirt block on common setup").define("logDirtBlock", true);

    private static final ForgeConfigSpec.IntValue MAGIC_NUMBER = BUILDER.comment("A magic number").defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER.comment("What you want the introduction message to be for the magic number").define("magicNumberIntroduction", "The magic number is... ");

    // a list of strings that are treated as resource locations for items
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER.comment("A list of items to log on common setup.").defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), Config::validateItemName);

    // 怪物光等表
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> MONSTER_BASE_LIGHTS = BUILDER
            .comment(
                    "Monster light level table.",
                    "Format: entity_registry_name=light_level",
                    "Example: minecraft:zombie=20",
                    "You can add mobs from other mods, e.g. modid:mob=50"
            )
            .defineListAllowEmpty("monsterBaseLights", List.of(
                    "minecraft:zombie=20",
                    "minecraft:husk=25",
                    "minecraft:drowned=25",
                    "minecraft:skeleton=20",
                    "minecraft:stray=25",
                    "minecraft:creeper=25",
                    "minecraft:spider=20",
                    "minecraft:cave_spider=25",
                    "minecraft:slime=15",
                    "minecraft:magma_cube=30",
                    "minecraft:zombified_piglin=30",
                    "minecraft:piglin=30",
                    "minecraft:piglin_brute=45",
                    "minecraft:hoglin=35",
                    "minecraft:zoglin=40",
                    "minecraft:enderman=45",
                    "minecraft:endermite=20",
                    "minecraft:silverfish=15",
                    "minecraft:witch=40",
                    "minecraft:phantom=35",
                    "minecraft:shulker=45",
                    "minecraft:vex=35",
                    "minecraft:pillager=35",
                    "minecraft:vindicator=40",
                    "minecraft:evoker=50",
                    "minecraft:ravager=55",
                    "minecraft:blaze=45",
                    "minecraft:ghast=50",
                    "minecraft:wither_skeleton=45",
                    "minecraft:wither=100",
                    "minecraft:ender_dragon=120",
                    "minecraft:warden=150"
            ), obj -> obj instanceof String);

    // 未配置的怪物默认光等
    private static final ForgeConfigSpec.IntValue DEFAULT_MONSTER_LIGHT = BUILDER
            .comment("Default light level for monsters not listed in monsterBaseLights.")
            .defineInRange("defaultMonsterLight", 20, 1, 10000);

    // 怪物生成光等随机浮动范围
    private static final ForgeConfigSpec.IntValue MONSTER_SPAWN_RANDOM_RANGE = BUILDER
            .comment("Monster spawn light random range. Value 30 means spawn light = average armor light +/- 30.")
            .defineInRange("monsterSpawnRandomRange", 30, 0, 1000);

    // 玩家攻击怪物时的伤害修正系数
    private static final ForgeConfigSpec.DoubleValue DEALT_OVERLEVEL_STEP = BUILDER
            .comment("Damage dealt bonus per light level when player is higher than monster. Default: 0.01 (1%).")
            .defineInRange("dealtOverlevelStep", 0.01, 0.0, 1.0);
    private static final ForgeConfigSpec.DoubleValue DEALT_OVERLEVEL_CAP = BUILDER
            .comment("Max light level advantage counted for damage dealt. Default: 20 (matches Destiny 2's +20 power advantage cap).")
            .defineInRange("dealtOverlevelCap", 20.0, 0.0, 1000.0);
    private static final ForgeConfigSpec.DoubleValue DEALT_UNDERLEVEL_STEP = BUILDER
            .comment("Damage dealt penalty per light level when player is lower than monster. Default: 0.02 (2%).")
            .defineInRange("dealtUnderlevelStep", 0.02, 0.0, 1.0);
    private static final ForgeConfigSpec.DoubleValue DEALT_UNDERLEVEL_MIN = BUILDER
            .comment("Minimum damage dealt multiplier when player is much lower than monster. Default: 0.02 (2%).")
            .defineInRange("dealtUnderlevelMin", 0.02, 0.0, 1.0);

    // 怪物攻击玩家时的伤害修正系数
    private static final ForgeConfigSpec.DoubleValue TAKEN_UNDERLEVEL_STEP = BUILDER
            .comment("Damage taken bonus per light level when player is lower than monster. Default: 0.04 (4%).")
            .defineInRange("takenUnderlevelStep", 0.04, 0.0, 1.0);
    private static final ForgeConfigSpec.DoubleValue TAKEN_UNDERLEVEL_CAP = BUILDER
            .comment("Max damage taken multiplier when player is much lower than monster. Default: 2.0 (200%).")
            .defineInRange("takenUnderlevelCap", 2.0, 0.0, 100.0);
    private static final ForgeConfigSpec.DoubleValue TAKEN_OVERLEVEL_STEP = BUILDER
            .comment("Damage taken reduction per light level when player is higher than monster. Default: 0.01 (1%).")
            .defineInRange("takenOverlevelStep", 0.01, 0.0, 1.0);
    private static final ForgeConfigSpec.DoubleValue TAKEN_OVERLEVEL_MIN = BUILDER
            .comment("Minimum damage taken multiplier when player is much higher than monster. Default: 0.8 (80%, corresponding to the +20 power advantage cap).")
            .defineInRange("takenOverlevelMin", 0.8, 0.0, 1.0);

    // 光等伤害调试开关
    private static final ForgeConfigSpec.BooleanValue DEBUG_LIGHT_DAMAGE = BUILDER
            .comment("Enable debug output in chat for light level damage calculation.")
            .define("debugLightDamage", false);

    // 光等 HUD 开关
    private static final ForgeConfigSpec.BooleanValue SHOW_LIGHT_HUD = BUILDER
            .comment("Show light level HUD on the screen.")
            .define("showLightHud", true);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean logDirtBlock;
    public static int magicNumber;
    public static String magicNumberIntroduction;
    public static Set<Item> items;

    private static boolean validateItemName(final Object obj) {
        return obj instanceof final String itemName && ForgeRegistries.ITEMS.containsKey(new ResourceLocation(itemName));
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        logDirtBlock = LOG_DIRT_BLOCK.get();
        magicNumber = MAGIC_NUMBER.get();
        magicNumberIntroduction = MAGIC_NUMBER_INTRODUCTION.get();

        // convert the list of strings into a set of items
        items = ITEM_STRINGS.get().stream().map(itemName -> ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemName))).collect(Collectors.toSet());

        LightLevelManager.reloadFromConfig(MONSTER_BASE_LIGHTS.get(), DEFAULT_MONSTER_LIGHT.get());
        LightLevelManager.setMonsterSpawnRandomRange(MONSTER_SPAWN_RANDOM_RANGE.get());
        LightLevelManager.reloadDamageConfig(
                DEALT_OVERLEVEL_STEP.get(), DEALT_OVERLEVEL_CAP.get(),
                DEALT_UNDERLEVEL_STEP.get(), DEALT_UNDERLEVEL_MIN.get(),
                TAKEN_UNDERLEVEL_STEP.get(), TAKEN_UNDERLEVEL_CAP.get(),
                TAKEN_OVERLEVEL_STEP.get(), TAKEN_OVERLEVEL_MIN.get()
        );
        LightLevelManager.setDebugEnabled(DEBUG_LIGHT_DAMAGE.get());
        LightLevelManager.setHudEnabled(SHOW_LIGHT_HUD.get());
    }
}
