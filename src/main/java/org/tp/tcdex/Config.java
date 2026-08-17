package org.tp.tcdex;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.tp.tcdex.element.ElementManager;
import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.light.LightLevelManager;
import org.tp.tcdex.shield.PlayerShieldManager;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
            .comment("Monster spawn light random roll range (+/-). Small value keeps same-type monsters consistent. Default: 5.")
            .defineInRange("monsterSpawnRandomRange", 5, 0, 1000);

    // 元素护盾黑名单（绝对不带元素盾的生物，兼容其他 mod，entity id 匹配）
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> SHIELD_BLACKLIST = BUILDER
            .comment(
                    "Entities that never get an elemental shield (also: no elemental attacks, since attack element = shield element).",
                    "Format: modid:entity, e.g. minecraft:slime, othermod:boss",
                    "Applies to monsters from any mod (matched by registry name)."
            )
            .defineListAllowEmpty("shieldBlacklist", List.of(), obj -> obj instanceof String);

    // 元素护盾生成权重（相对权重，0 = 不生成该元素盾）
    private static final ForgeConfigSpec.IntValue SHIELD_WEIGHT_SOLAR = BUILDER
            .comment("Relative weight for Solar shields on random assignment.")
            .defineInRange("shieldWeightSolar", 1, 0, 100);
    private static final ForgeConfigSpec.IntValue SHIELD_WEIGHT_ARC = BUILDER
            .comment("Relative weight for Arc shields on random assignment.")
            .defineInRange("shieldWeightArc", 1, 0, 100);
    private static final ForgeConfigSpec.IntValue SHIELD_WEIGHT_VOID = BUILDER
            .comment("Relative weight for Void shields on random assignment.")
            .defineInRange("shieldWeightVoid", 1, 0, 100);
    private static final ForgeConfigSpec.IntValue SHIELD_WEIGHT_STASIS = BUILDER
            .comment("Relative weight for Stasis shields on random assignment.")
            .defineInRange("shieldWeightStasis", 1, 0, 100);
    private static final ForgeConfigSpec.IntValue SHIELD_WEIGHT_STRAND = BUILDER
            .comment("Relative weight for Strand shields on random assignment.")
            .defineInRange("shieldWeightStrand", 1, 0, 100);
    private static final ForgeConfigSpec.IntValue SHIELD_WEIGHT_PRISM = BUILDER
            .comment("Relative weight for Prism shields on random assignment. Default: 0 (Prism temporarily excluded from random shields).")
            .defineInRange("shieldWeightPrism", 0, 0, 100);

    // 元素充能随机元素权重（相对权重，0 = 不会随机到该元素）
    private static final ForgeConfigSpec.IntValue ELEMENT_WEIGHT_SOLAR = BUILDER
            .comment("Relative weight for Solar when Elemental Charge rolls an element.")
            .defineInRange("elementWeightSolar", 1, 0, 100);
    private static final ForgeConfigSpec.IntValue ELEMENT_WEIGHT_ARC = BUILDER
            .comment("Relative weight for Arc when Elemental Charge rolls an element.")
            .defineInRange("elementWeightArc", 1, 0, 100);
    private static final ForgeConfigSpec.IntValue ELEMENT_WEIGHT_VOID = BUILDER
            .comment("Relative weight for Void when Elemental Charge rolls an element.")
            .defineInRange("elementWeightVoid", 1, 0, 100);
    private static final ForgeConfigSpec.IntValue ELEMENT_WEIGHT_STASIS = BUILDER
            .comment("Relative weight for Stasis when Elemental Charge rolls an element.")
            .defineInRange("elementWeightStasis", 1, 0, 100);
    private static final ForgeConfigSpec.IntValue ELEMENT_WEIGHT_STRAND = BUILDER
            .comment("Relative weight for Strand when Elemental Charge rolls an element.")
            .defineInRange("elementWeightStrand", 1, 0, 100);
    private static final ForgeConfigSpec.IntValue ELEMENT_WEIGHT_PRISM = BUILDER
            .comment("Relative weight for Prism when Elemental Charge rolls an element. Default: 0 (players cannot get Prism damage via the Elemental Charge modifier).")
            .defineInRange("elementWeightPrism", 0, 0, 100);

    // 世界光等场：出生点附近基线
    private static final ForgeConfigSpec.IntValue WORLD_BASE_LIGHT = BUILDER
            .comment("World base light level: monster light near world spawn in the overworld. Monster light is fixed by world position, not player average.")
            .defineInRange("worldBaseLight", 20, 1, 10000);

    // 维度光等偏移
    private static final ForgeConfigSpec.IntValue NETHER_LIGHT_OFFSET = BUILDER
            .comment("Extra monster light offset in the Nether.")
            .defineInRange("netherLightOffset", 25, 0, 1000);
    private static final ForgeConfigSpec.IntValue END_LIGHT_OFFSET = BUILDER
            .comment("Extra monster light offset in the End.")
            .defineInRange("endLightOffset", 50, 0, 1000);

    // 维度光等偏移表（含其他 mod 维度）与其他维度默认偏移
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> DIMENSION_LIGHT_OFFSETS = BUILDER
            .comment(
                    "Dimension light offsets (overrides built-in Nether/End and the default for other dimensions).",
                    "Format: dimension_registry_name=offset",
                    "Example: twilightforest:twilight_forest=40",
                    "Works with dimensions from any mod."
            )
            .defineListAllowEmpty("dimensionLightOffsets", List.of(), obj -> obj instanceof String);
    private static final ForgeConfigSpec.IntValue OTHER_DIMENSION_OFFSET = BUILDER
            .comment("Default monster light offset for dimensions not listed above (dimensions added by other mods).")
            .defineInRange("dimensionOffsetOther", 30, 0, 1000);

    // 距离梯度：越远离出生点越难
    private static final ForgeConfigSpec.IntValue DISTANCE_GRADIENT_STEP = BUILDER
            .comment("Monster light gained per 1000 blocks away from world spawn.")
            .defineInRange("distanceGradientStep", 3, 0, 100);
    private static final ForgeConfigSpec.IntValue DISTANCE_GRADIENT_CAP = BUILDER
            .comment("Max monster light gained from distance to world spawn.")
            .defineInRange("distanceGradientCap", 45, 0, 1000);

    // 时间压力：黑暗从世界边缘向中心蔓延
    private static final ForgeConfigSpec.IntValue DAYS_PER_TIME_BONUS = BUILDER
            .comment("Active days (server runtime days) per +1 monster light from time pressure. Server time does not advance while nobody plays.")
            .defineInRange("daysPerTimeBonus", 5, 1, 1000);
    private static final ForgeConfigSpec.IntValue MAX_TIME_BONUS = BUILDER
            .comment("Max monster light gained from time pressure.")
            .defineInRange("maxTimeBonus", 30, 0, 1000);
    private static final ForgeConfigSpec.IntValue TIME_SPREAD_START = BUILDER
            .comment("Time pressure starts applying beyond this distance from spawn (blocks). Spawn area stays easy for new players.")
            .defineInRange("timeSpreadStart", 2000, 0, 1000000);
    private static final ForgeConfigSpec.IntValue TIME_SPREAD_END = BUILDER
            .comment("Time pressure reaches full strength at this distance from spawn (blocks).")
            .defineInRange("timeSpreadEnd", 10000, 1, 1000000);

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

    // 玩家脱战自动回复护盾（命运2 风格）
    private static final ForgeConfigSpec.BooleanValue PLAYER_SHIELD_ENABLED = BUILDER
            .comment("Enable player auto-regenerating shield (Destiny 2 style).")
            .define("playerShieldEnabled", true);
    private static final ForgeConfigSpec.DoubleValue PLAYER_SHIELD_RATIO = BUILDER
            .comment("Shield max = player max health × ratio. Default: 1.0 (shield equals health).")
            .defineInRange("playerShieldRatio", 1.0, 0.0, 10.0);
    private static final ForgeConfigSpec.IntValue PLAYER_SHIELD_REGEN_DELAY = BUILDER
            .comment("Seconds out of combat (no damage taken, no attacks) before shield starts regenerating. Default: 5.")
            .defineInRange("playerShieldRegenDelay", 5, 0, 60);
    private static final ForgeConfigSpec.DoubleValue PLAYER_SHIELD_REGEN_RATE = BUILDER
            .comment("Shield points regenerated per tick while out of combat. Default: 0.4 (8/s, full in ~2.5s).")
            .defineInRange("playerShieldRegenRate", 0.4, 0.0, 100.0);
    private static final ForgeConfigSpec.BooleanValue PLAYER_SHIELD_HUD = BUILDER
            .comment("Show player shield bar on HUD.")
            .define("playerShieldHud", true);
    private static final ForgeConfigSpec.BooleanValue PLAYER_BUFF_HUD = BUILDER
            .comment("Show Destiny 2 style buff list HUD (eager edge, all permitted, elemental states).")
            .define("playerBuffHud", true);
    private static final ForgeConfigSpec.BooleanValue MONSTER_SHIELD_HUD = BUILDER
            .comment("Show monster elemental shield HUD (name tag marker + crosshair shield bar).")
            .define("monsterShieldHud", true);

    // 元素怪物：怪物元素攻击（命中玩家时施加元素状态）
    private static final ForgeConfigSpec.BooleanValue MONSTER_ELEMENTAL_ATTACKS = BUILDER
            .comment("Monsters with elemental attacks (from the attack table) apply their element state to players on hit.")
            .define("monsterElementalAttacks", true);
    private static final ForgeConfigSpec.DoubleValue MONSTER_ELEMENTAL_ATTACK_CHANCE = BUILDER
            .comment("Chance per hit for an elemental monster attack to apply its element state (0-1). Default: 1.0 (always applies).")
            .defineInRange("monsterElementalAttackChance", 1.0, 0.0, 1.0);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean logDirtBlock;
    public static int magicNumber;
    public static String magicNumberIntroduction;
    public static Set<Item> items;
    public static boolean playerShieldHud;
    public static boolean playerBuffHud;
    public static boolean monsterShieldHud;

    private static boolean validateItemName(final Object obj) {
        if (!(obj instanceof final String itemName)) {
            return false;
        }
        ResourceLocation location = ResourceLocation.tryParse(itemName);
        return location != null && ForgeRegistries.ITEMS.containsKey(location);
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        logDirtBlock = LOG_DIRT_BLOCK.get();
        magicNumber = MAGIC_NUMBER.get();
        magicNumberIntroduction = MAGIC_NUMBER_INTRODUCTION.get();

        // convert the list of strings into a set of items
        items = ITEM_STRINGS.get().stream()
                .map(ResourceLocation::tryParse)
                .filter(location -> location != null)
                .map(ForgeRegistries.ITEMS::getValue)
                .collect(Collectors.toSet());

        LightLevelManager.reloadFromConfig(MONSTER_BASE_LIGHTS.get(), DEFAULT_MONSTER_LIGHT.get());
        LightLevelManager.setMonsterSpawnRandomRange(MONSTER_SPAWN_RANDOM_RANGE.get());
        LightLevelManager.reloadWorldLightConfig(
                WORLD_BASE_LIGHT.get(), NETHER_LIGHT_OFFSET.get(), END_LIGHT_OFFSET.get(),
                DISTANCE_GRADIENT_STEP.get(), DISTANCE_GRADIENT_CAP.get(),
                DAYS_PER_TIME_BONUS.get(), MAX_TIME_BONUS.get(),
                TIME_SPREAD_START.get(), TIME_SPREAD_END.get()
        );
        LightLevelManager.reloadDimensionConfig(DIMENSION_LIGHT_OFFSETS.get(), OTHER_DIMENSION_OFFSET.get());
        // 元素护盾黑名单与生成权重
        Map<ElementType, Integer> shieldWeights = new EnumMap<>(ElementType.class);
        shieldWeights.put(ElementType.SOLAR, SHIELD_WEIGHT_SOLAR.get());
        shieldWeights.put(ElementType.ARC, SHIELD_WEIGHT_ARC.get());
        shieldWeights.put(ElementType.VOID, SHIELD_WEIGHT_VOID.get());
        shieldWeights.put(ElementType.STASIS, SHIELD_WEIGHT_STASIS.get());
        shieldWeights.put(ElementType.STRAND, SHIELD_WEIGHT_STRAND.get());
        shieldWeights.put(ElementType.PRISM, SHIELD_WEIGHT_PRISM.get());
        ElementManager.reloadShieldConfig(SHIELD_BLACKLIST.get(), shieldWeights);
        // 元素充能随机元素权重
        Map<ElementType, Integer> elementWeights = new EnumMap<>(ElementType.class);
        elementWeights.put(ElementType.SOLAR, ELEMENT_WEIGHT_SOLAR.get());
        elementWeights.put(ElementType.ARC, ELEMENT_WEIGHT_ARC.get());
        elementWeights.put(ElementType.VOID, ELEMENT_WEIGHT_VOID.get());
        elementWeights.put(ElementType.STASIS, ELEMENT_WEIGHT_STASIS.get());
        elementWeights.put(ElementType.STRAND, ELEMENT_WEIGHT_STRAND.get());
        elementWeights.put(ElementType.PRISM, ELEMENT_WEIGHT_PRISM.get());
        ElementManager.reloadElementWeights(elementWeights);
        ElementManager.reloadAttackConfig(MONSTER_ELEMENTAL_ATTACKS.get(), MONSTER_ELEMENTAL_ATTACK_CHANCE.get());
        PlayerShieldManager.reloadConfig(
                PLAYER_SHIELD_ENABLED.get(), PLAYER_SHIELD_RATIO.get(),
                PLAYER_SHIELD_REGEN_DELAY.get(), PLAYER_SHIELD_REGEN_RATE.get());
        playerShieldHud = PLAYER_SHIELD_HUD.get();
        playerBuffHud = PLAYER_BUFF_HUD.get();
        monsterShieldHud = MONSTER_SHIELD_HUD.get();
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
