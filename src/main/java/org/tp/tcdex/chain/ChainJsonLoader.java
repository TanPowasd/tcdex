package org.tp.tcdex.chain;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.fml.loading.FMLPaths;
import org.tp.tcdex.api.ChainTriggerTime;
import org.tp.tcdex.api.GenericChainContext;
import org.tp.tcdex.api.GenericChainEffect;
import org.tp.tcdex.api.GenericChainReaction;
import org.tp.tcdex.api.TcdexChainRegistry;
import org.tp.tcdex.element.ElementManager;
import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.reaction.ReactionType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * JSON 简易元素链加载器。
 *
 * <p>从 {@code config/tcdex-chain/*.json} 加载简易元素链反应，
 * 自动注册到 {@link TcdexChainRegistry}。</p>
 */
public final class ChainJsonLoader {

    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("tcdex-chain");

    private ChainJsonLoader() {
    }

    /** 加载 config/tcdex-chain 下所有 JSON 文件 */
    public static void loadAll() {
        try {
            Files.createDirectories(CONFIG_DIR);
            try (Stream<Path> paths = Files.list(CONFIG_DIR)) {
                paths.filter(path -> path.toString().toLowerCase().endsWith(".json"))
                        .sorted()
                        .forEach(ChainJsonLoader::loadFile);
            }
        } catch (IOException e) {
            org.slf4j.LoggerFactory.getLogger(ChainJsonLoader.class).warn("Failed to load chain json config", e);
        }
    }

    private static void loadFile(Path path) {
        try {
            String content = Files.readString(path);
            JsonElement root = JsonParser.parseString(content);
            if (root.isJsonArray()) {
                for (JsonElement element : root.getAsJsonArray()) {
                    if (element.isJsonObject()) {
                        parseReaction(element.getAsJsonObject(), path);
                    }
                }
            } else if (root.isJsonObject()) {
                JsonObject object = root.getAsJsonObject();
                if (object.has("reactions") && object.get("reactions").isJsonArray()) {
                    for (JsonElement element : object.getAsJsonArray("reactions")) {
                        if (element.isJsonObject()) {
                            parseReaction(element.getAsJsonObject(), path);
                        }
                    }
                } else {
                    parseReaction(object, path);
                }
            }
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(ChainJsonLoader.class).warn("Failed to parse chain json: {}", path, e);
        }
    }

    private static void parseReaction(JsonObject json, Path path) {
        String id = getString(json, "id", null);
        if (id == null || id.isBlank()) {
            id = path.getFileName().toString().replace(".json", "");
        }

        ChainTriggerTime[] triggerTimes = parseTriggerTimes(json);
        int minElements = getInt(json, "minElements", 0);
        int maxElements = getInt(json, "maxElements", 0);
        List<ElementType> contains = parseElements(json, "contains");
        List<ElementType> exclude = parseElements(json, "exclude");
        int priority = getInt(json, "priority", 0);

        Predicate<GenericChainContext> matcher = ctx -> {
            int size = ctx.getElements().size();
            if (size < minElements) {
                return false;
            }
            if (maxElements > 0 && size > maxElements) {
                return false;
            }
            for (ElementType element : contains) {
                if (!ctx.getElements().contains(element)) {
                    return false;
                }
            }
            for (ElementType element : exclude) {
                if (ctx.getElements().contains(element)) {
                    return false;
                }
            }
            return true;
        };

        GenericChainReaction.Builder builder = GenericChainReaction.builder(id)
                .matches(matcher)
                .priority(priority)
                .triggers(triggerTimes);

        if (json.has("effect") && json.get("effect").isJsonObject()) {
            builder.effect(parseEffect(json.getAsJsonObject("effect")));
        }

        TcdexChainRegistry.registerGenericReaction(builder.build());
    }

    private static GenericChainEffect parseEffect(JsonObject json) {
        ReactionType type = parseReactionType(getString(json, "type", "DAMAGE"));
        float damage = getFloat(json, "damage", 0.0f);
        float radius = getFloat(json, "radius", 0.0f);
        int duration = getInt(json, "duration", 0);
        float intensity = getFloat(json, "intensity", 1.0f);
        boolean selfBuff = getBoolean(json, "selfBuff", false);
        boolean triggerReactions = getBoolean(json, "triggerReactions", false);
        return new GenericChainEffect(type, damage, radius, duration, intensity, selfBuff, triggerReactions);
    }

    private static ChainTriggerTime[] parseTriggerTimes(JsonObject json) {
        if (!json.has("triggerTimes") || !json.get("triggerTimes").isJsonArray()) {
            return new ChainTriggerTime[] {
                    ChainTriggerTime.CHAIN_CHANGE,
                    ChainTriggerTime.DETONATE,
                    ChainTriggerTime.FINISHER
            };
        }
        List<ChainTriggerTime> result = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray("triggerTimes")) {
            try {
                result.add(ChainTriggerTime.valueOf(element.getAsString()));
            } catch (IllegalArgumentException ignored) {
                // 忽略未知触发时机
            }
        }
        return result.isEmpty()
                ? new ChainTriggerTime[] { ChainTriggerTime.CHAIN_CHANGE, ChainTriggerTime.DETONATE, ChainTriggerTime.FINISHER }
                : result.toArray(new ChainTriggerTime[0]);
    }

    private static ReactionType parseReactionType(String value) {
        try {
            return ReactionType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ReactionType.DAMAGE;
        }
    }

    private static List<ElementType> parseElements(JsonObject json, String key) {
        List<ElementType> result = new ArrayList<>();
        if (json.has(key) && json.get(key).isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray(key)) {
                ElementType type = ElementManager.parseElement(element.getAsString());
                if (type != null) {
                    result.add(type);
                }
            }
        }
        return result;
    }

    private static String getString(JsonObject json, String key, String defaultValue) {
        return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsString() : defaultValue;
    }

    private static int getInt(JsonObject json, String key, int defaultValue) {
        return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsInt() : defaultValue;
    }

    private static float getFloat(JsonObject json, String key, float defaultValue) {
        return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsFloat() : defaultValue;
    }

    private static boolean getBoolean(JsonObject json, String key, boolean defaultValue) {
        return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsBoolean() : defaultValue;
    }
}
