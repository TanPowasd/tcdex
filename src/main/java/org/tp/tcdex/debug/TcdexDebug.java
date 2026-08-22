package org.tp.tcdex.debug;

/**
 * TCDEX 调试开关集中管理。
 *
 * <p>elementalDebug 控制元素伤害/护盾/关键词系统的调试输出（聊天 + 服务端日志）。</p>
 */
public final class TcdexDebug {

    /** 元素/护盾系统调试开关 */
    private static boolean elementalEnabled = false;

    /** 元素反应触发调试开关 */
    private static boolean reactionDebugEnabled = false;

    private TcdexDebug() {
    }

    public static boolean isElementalEnabled() {
        return elementalEnabled;
    }

    public static void setElementalEnabled(boolean enabled) {
        elementalEnabled = enabled;
    }

    /** 切换元素调试开关，返回切换后的状态 */
    public static boolean toggleElemental() {
        elementalEnabled = !elementalEnabled;
        return elementalEnabled;
    }

    public static boolean isReactionDebugEnabled() {
        return reactionDebugEnabled;
    }

    public static void setReactionDebugEnabled(boolean enabled) {
        reactionDebugEnabled = enabled;
    }

    /** 切换元素反应调试开关，返回切换后的状态 */
    public static boolean toggleReaction() {
        reactionDebugEnabled = !reactionDebugEnabled;
        return reactionDebugEnabled;
    }
}
