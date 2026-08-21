package org.tp.tcdex.integration.tinkers;

import org.tp.tcdex.api.ITinkersBridge;

/**
 * 保存当前生效的 Tinkers 桥接实现。
 */
public final class TinkersBridgeHolder {

    private static ITinkersBridge bridge;

    private TinkersBridgeHolder() {
    }

    public static void set(ITinkersBridge bridge) {
        TinkersBridgeHolder.bridge = bridge;
    }

    public static ITinkersBridge get() {
        return bridge;
    }

    public static boolean isAvailable() {
        return bridge != null;
    }
}
