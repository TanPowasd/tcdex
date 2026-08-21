package org.tp.tcdex.integration;

import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.tp.tcdex.api.ITcdexIntegration;
import org.tp.tcdex.api.TcdexIntegrationRegistry;
import org.tp.tcdex.integration.iceandfire.IceAndFireIntegration;
import org.tp.tcdex.integration.irons_spellbooks.IronSpellsIntegration;
import org.tp.tcdex.integration.tinkers.TinkersIntegration;

import java.util.List;

/**
 * TCDEX 联动管理器：统一检测外部 mod 是否加载，并派发 Add 包生命周期。
 *
 * <p>内置 Add 包在静态块注册；附属 mod 也可以通过
 * {@link org.tp.tcdex.api.TcdexIntegrationBuilder} / {@link TcdexIntegrationRegistry} 注册自己的联动。</p>
 */
public final class IntegrationManager {

    private static boolean initialized = false;
    private static IEventBus modEventBus;

    private IntegrationManager() {
    }

    static {
        TcdexIntegrationRegistry.register(new TinkersIntegration());
        TcdexIntegrationRegistry.register(new IceAndFireIntegration());
        TcdexIntegrationRegistry.register(new IronSpellsIntegration());
    }

    /** 在 mod 构造早期调用：初始化所有已加载的 Add 包 */
    public static void init(IEventBus modEventBus) {
        IntegrationManager.modEventBus = modEventBus;
        TcdexIntegrationRegistry.setLateRegistrationHandler(IntegrationManager::initLate);
        initialized = true;
        for (ITcdexIntegration integration : TcdexIntegrationRegistry.getRegistered()) {
            if (integration.shouldLoad()) {
                integration.init(modEventBus);
                TcdexIntegrationRegistry.markActive(integration);
            }
        }
    }

    /** 供 TcdexIntegrationRegistry 在 init 之后新注册 Add 包时调用 */
    private static void initLate(ITcdexIntegration integration) {
        if (!initialized || modEventBus == null) {
            return;
        }
        if (integration.shouldLoad()) {
            integration.init(modEventBus);
            TcdexIntegrationRegistry.markActive(integration);
        }
    }

    /** 是否已经完成 Add 包初始化 */
    public static boolean isInitialized() {
        return initialized;
    }

    /** CommonSetup 阶段派发 */
    public static void fireCommonSetup(FMLCommonSetupEvent event) {
        for (ITcdexIntegration integration : TcdexIntegrationRegistry.getActive()) {
            integration.onCommonSetup(event);
        }
    }

    /** 服务端启动阶段派发 */
    public static void fireServerStarting(ServerStartingEvent event) {
        for (ITcdexIntegration integration : TcdexIntegrationRegistry.getActive()) {
            integration.onServerStarting(event);
        }
    }

    /** 服务端停止阶段派发 */
    public static void fireServerStopping(ServerStoppingEvent event) {
        for (ITcdexIntegration integration : TcdexIntegrationRegistry.getActive()) {
            integration.onServerStopping(event);
        }
    }

    /** 获取当前活跃 Add 包（只读） */
    public static List<ITcdexIntegration> getActiveIntegrations() {
        return TcdexIntegrationRegistry.getActive();
    }
}
