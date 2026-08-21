package org.tp.tcdex.api;

import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.reaction.ElementReaction;
import org.tp.tcdex.reaction.ElementReactionEvents;
import org.tp.tcdex.reaction.ElementReactionRegistry;

/**
 * TCDEX 元素反应对外 API。
 *
 * <p>附属 mod 可以注册自定义元素反应、查询反应、控制反应总开关。</p>
 */
public final class TcdexReactionAPI {

    private TcdexReactionAPI() {
    }

    /** 注册一条自定义元素反应 */
    public static void registerReaction(ElementReaction reaction) {
        ElementReactionRegistry.register(reaction);
    }

    /** 查询已有元素 aura 被 trigger 触发时是否存在反应 */
    public static ElementReaction findReaction(ElementType aura, ElementType trigger) {
        return ElementReactionRegistry.find(aura, trigger);
    }

    /** 元素反应总开关 */
    public static boolean isEnabled() {
        return ElementReactionEvents.isEnabled();
    }

    /** 设置元素反应总开关 */
    public static void setEnabled(boolean enabled) {
        ElementReactionEvents.setEnabled(enabled);
    }
}
