package org.tp.tcdex.hud;

import org.tp.tcdex.chain.ChainEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 连携 HUD 客户端缓存。
 *
 * <p>由 {@link org.tp.tcdex.network.ChainStateSyncPacket} 从服务端同步，
 * 实际渲染接入 {@link TcdexBuffHud}。</p>
 */
public final class ChainHud {

    private static List<ChainEntry> mainChain = Collections.emptyList();
    private static List<ChainEntry> focusChain = Collections.emptyList();
    private static float groupOverflow = 0.0f;
    private static int detonateCooldown = 0;
    private static int chainBuffTicks = 0;

    private ChainHud() {
    }

    public static void sync(List<ChainEntry> main, List<ChainEntry> focus,
                            float group, int cooldown, int buff) {
        mainChain = List.copyOf(main);
        focusChain = List.copyOf(focus);
        groupOverflow = group;
        detonateCooldown = cooldown;
        chainBuffTicks = buff;
    }

    public static List<ChainEntry> getMainChain() {
        return new ArrayList<>(mainChain);
    }

    public static List<ChainEntry> getFocusChain() {
        return new ArrayList<>(focusChain);
    }

    public static float getGroupOverflow() {
        return groupOverflow;
    }

    public static int getDetonateCooldown() {
        return detonateCooldown;
    }

    public static int getChainBuffTicks() {
        return chainBuffTicks;
    }
}
