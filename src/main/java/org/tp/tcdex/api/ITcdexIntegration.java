package org.tp.tcdex.api;

import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * TCDEX 多 mod 统一联动接口。
 *
 * <p>所有外部 mod 联动 Add 包都实现此接口，由 {@code IntegrationManager} 统一管理生命周期：</p>
 * <ul>
 *   <li>{@link #shouldLoad()} 检测目标 mod 是否加载</li>
 *   <li>{@link #init(IEventBus)} 在 mod 构造阶段注册词条、事件、桥接等</li>
 *   <li>{@link #onCommonSetup(FMLCommonSetupEvent)} 在 CommonSetup 阶段做跨 mod 初始化</li>
 *   <li>{@link #onServerStarting(ServerStartingEvent)} / {@link #onServerStopping(ServerStoppingEvent)} 服务端生命周期</li>
 * </ul>
 */
public interface ITcdexIntegration {

    /** 目标外部 mod 的 modId，例如 "tconstruct"、"iceandfire" */
    String getModId();

    /** 是否应当加载此 Add 包；默认检测目标 mod 是否已加载 */
    default boolean shouldLoad() {
        return ModList.get().isLoaded(getModId());
    }

    /** 显示名称，默认使用 modId */
    default String getDisplayName() {
        return getModId();
    }

    /** mod 构造阶段：注册桥接、事件、词条等 */
    default void init(IEventBus modEventBus) {
    }

    /** Forge CommonSetup 阶段 */
    default void onCommonSetup(FMLCommonSetupEvent event) {
    }

    /** 服务端启动阶段 */
    default void onServerStarting(ServerStartingEvent event) {
    }

    /** 服务端停止阶段 */
    default void onServerStopping(ServerStoppingEvent event) {
    }
}
