package org.tp.tcdex.element;

import org.tp.tcdex.element.processor.MoonProcessor;

import java.util.EnumMap;
import java.util.Map;

/**
 * 元素效果处理器注册中心。
 *
 * <p>每个元素可注册一个 {@link ElementEffectProcessor}；未注册时使用默认空实现。</p>
 */
public final class ElementEffectRegistry {

    private static final Map<ElementType, ElementEffectProcessor> PROCESSORS = new EnumMap<>(ElementType.class);

    private static final ElementEffectProcessor DEFAULT = new ElementEffectProcessor() {
    };

    static {
        registerDefaultProcessors();
    }

    private ElementEffectRegistry() {
    }

    public static void register(ElementType type, ElementEffectProcessor processor) {
        if (type != null && processor != null) {
            PROCESSORS.put(type, processor);
        }
    }

    public static ElementEffectProcessor get(ElementType type) {
        return PROCESSORS.getOrDefault(type, DEFAULT);
    }

    private static void registerDefaultProcessors() {
        // 预留：后续将 Sever / Shatter / Volatile / Jolt / Refract 从 ElementalStateEvents 迁移到这里。
        // 当前先注册 Moon，用于月蚀净化。
        register(ElementType.MOON, new MoonProcessor());
    }
}
