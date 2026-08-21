package org.tp.tcdex.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * 多 mod 统一 Add 包注册中心。
 *
 * <p>所有 TCDEX 联动 Add 包（内置或附属 mod 注册）都集中在这里，
 * 由 {@code IntegrationManager} 统一检测加载与生命周期派发。</p>
 */
public final class TcdexIntegrationRegistry {

    private static final List<ITcdexIntegration> REGISTERED = new ArrayList<>();
    private static final List<ITcdexIntegration> ACTIVE = new ArrayList<>();
    private static Consumer<ITcdexIntegration> lateRegistrationHandler;

    private TcdexIntegrationRegistry() {
    }

    /** 注册一个 Add 包（重复 modId 会忽略） */
    public static synchronized void register(ITcdexIntegration integration) {
        if (integration == null) {
            return;
        }
        for (ITcdexIntegration existing : REGISTERED) {
            if (existing.getModId().equals(integration.getModId())) {
                return;
            }
        }
        REGISTERED.add(integration);
        if (lateRegistrationHandler != null) {
            lateRegistrationHandler.accept(integration);
        }
    }

    /** 设置“注册发生在 IntegrationManager.init 之后”时的回调（由 IntegrationManager 调用） */
    public static synchronized void setLateRegistrationHandler(Consumer<ITcdexIntegration> handler) {
        lateRegistrationHandler = handler;
    }

    /** 获取全部已注册 Add 包（只读） */
    public static synchronized List<ITcdexIntegration> getRegistered() {
        return Collections.unmodifiableList(new ArrayList<>(REGISTERED));
    }

    /** 获取当前已加载（目标 mod 存在且已初始化）的 Add 包（只读） */
    public static synchronized List<ITcdexIntegration> getActive() {
        return Collections.unmodifiableList(new ArrayList<>(ACTIVE));
    }

    /** 查询某个 modId 是否已有对应的活跃 Add 包 */
    public static synchronized boolean isModIntegrated(String modId) {
        for (ITcdexIntegration integration : ACTIVE) {
            if (integration.getModId().equals(modId)) {
                return true;
            }
        }
        return false;
    }

    /** 由 IntegrationManager 在初始化成功后标记为活跃 */
    public static synchronized void markActive(ITcdexIntegration integration) {
        if (!ACTIVE.contains(integration)) {
            ACTIVE.add(integration);
        }
    }

    /** 清理活跃列表（一般用于测试或重载场景） */
    public static synchronized void clearActive() {
        ACTIVE.clear();
    }
}
