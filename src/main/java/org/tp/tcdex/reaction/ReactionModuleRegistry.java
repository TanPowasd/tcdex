package org.tp.tcdex.reaction;

import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.reaction.module.AmplifyReactionModule;
import org.tp.tcdex.reaction.module.ControlReactionModule;
import org.tp.tcdex.reaction.module.DamageReactionModule;
import org.tp.tcdex.reaction.module.DiffusionReactionModule;
import org.tp.tcdex.reaction.module.ShieldReactionModule;

import java.util.EnumMap;
import java.util.Map;

/**
 * 元素反应模块注册中心。
 *
 * <p>每个反应对应一个 {@link ElementReactionModule} 实现，内置模块在类加载时
 * 根据 {@link ElementReactionRegistry} 中的反应定义自动生成。</p>
 */
public final class ReactionModuleRegistry {

    private static final Map<ElementType, Map<ElementType, ElementReactionModule>> MODULES = new EnumMap<>(ElementType.class);

    static {
        for (ElementReaction reaction : ElementReactionRegistry.getAllReactions()) {
            register(createModule(reaction));
        }
    }

    private ReactionModuleRegistry() {
    }

    public static void register(ElementReactionModule module) {
        if (module == null || module.getReaction() == null) {
            return;
        }
        ElementType aura = module.getReaction().getAuraElement();
        ElementType trigger = module.getReaction().getTriggerElement();
        MODULES.computeIfAbsent(aura, k -> new EnumMap<>(ElementType.class))
                .put(trigger, module);
    }

    public static ElementReactionModule find(ElementType aura, ElementType trigger) {
        if (aura == null || trigger == null) {
            return null;
        }
        Map<ElementType, ElementReactionModule> map = MODULES.get(aura);
        return map == null ? null : map.get(trigger);
    }

    /** 如果指定反应还没有模块，按反应类型注册一个默认模块 */
    public static void ensure(ElementReaction reaction) {
        if (reaction == null) {
            return;
        }
        if (find(reaction.getAuraElement(), reaction.getTriggerElement()) == null) {
            register(createModule(reaction));
        }
    }

    /** 获取已有模块；没有时按反应类型创建一个临时模块（如岚流通用扩散） */
    public static ElementReactionModule getOrCreate(ElementReaction reaction) {
        if (reaction == null) {
            return null;
        }
        ElementReactionModule existing = find(reaction.getAuraElement(), reaction.getTriggerElement());
        return existing != null ? existing : createModule(reaction);
    }

    /** 取消注册指定方向的反应模块 */
    public static void unregister(ElementType aura, ElementType trigger) {
        if (aura == null || trigger == null) {
            return;
        }
        Map<ElementType, ElementReactionModule> map = MODULES.get(aura);
        if (map != null) {
            map.remove(trigger);
            if (map.isEmpty()) {
                MODULES.remove(aura);
            }
        }
    }

    private static ElementReactionModule createModule(ElementReaction reaction) {
        return switch (reaction.getType()) {
            case DAMAGE -> new DamageReactionModule(reaction);
            case CONTROL -> new ControlReactionModule(reaction);
            case AMPLIFY -> new AmplifyReactionModule(reaction);
            case SHIELD -> new ShieldReactionModule(reaction);
            case DIFFUSION -> new DiffusionReactionModule(reaction);
        };
    }
}
