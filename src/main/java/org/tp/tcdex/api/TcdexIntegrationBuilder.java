package org.tp.tcdex.api;

import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * 多 mod 统一 Add 包 Builder。
 *
 * <p>附属 mod 可以用流式 API 快速注册一个轻量联动 Add 包，而不需要新建类：</p>
 * <pre>{@code
 * TcdexIntegrationBuilder.builder("mymod")
 *         .displayName("My Mod")
 *         .onInit(bus -> { ... })
 *         .onCommonSetup(event -> { ... })
 *         .onServerStarting(event -> { ... })
 *         .register();
 * }</pre>
 */
public final class TcdexIntegrationBuilder implements ITcdexIntegration {

    private final String modId;
    private String displayName;
    private BooleanSupplier shouldLoad;
    private Consumer<IEventBus> init;
    private Consumer<FMLCommonSetupEvent> commonSetup;
    private Consumer<ServerStartingEvent> serverStarting;
    private Consumer<ServerStoppingEvent> serverStopping;

    private TcdexIntegrationBuilder(String modId) {
        this.modId = modId;
        this.displayName = modId;
        this.shouldLoad = () -> ITcdexIntegration.super.shouldLoad();
        this.init = bus -> {
        };
        this.commonSetup = event -> {
        };
        this.serverStarting = event -> {
        };
        this.serverStopping = event -> {
        };
    }

    public static TcdexIntegrationBuilder builder(String modId) {
        if (modId == null || modId.isEmpty()) {
            throw new IllegalArgumentException("modId must not be empty");
        }
        return new TcdexIntegrationBuilder(modId);
    }

    public TcdexIntegrationBuilder displayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public TcdexIntegrationBuilder shouldLoad(BooleanSupplier shouldLoad) {
        this.shouldLoad = shouldLoad;
        return this;
    }

    public TcdexIntegrationBuilder onInit(Consumer<IEventBus> init) {
        this.init = init;
        return this;
    }

    public TcdexIntegrationBuilder onCommonSetup(Consumer<FMLCommonSetupEvent> commonSetup) {
        this.commonSetup = commonSetup;
        return this;
    }

    public TcdexIntegrationBuilder onServerStarting(Consumer<ServerStartingEvent> serverStarting) {
        this.serverStarting = serverStarting;
        return this;
    }

    public TcdexIntegrationBuilder onServerStopping(Consumer<ServerStoppingEvent> serverStopping) {
        this.serverStopping = serverStopping;
        return this;
    }

    @Override
    public String getModId() {
        return modId;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean shouldLoad() {
        return shouldLoad.getAsBoolean();
    }

    @Override
    public void init(IEventBus modEventBus) {
        init.accept(modEventBus);
    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event) {
        commonSetup.accept(event);
    }

    @Override
    public void onServerStarting(ServerStartingEvent event) {
        serverStarting.accept(event);
    }

    @Override
    public void onServerStopping(ServerStoppingEvent event) {
        serverStopping.accept(event);
    }

    /** 注册到 TCDEX 统一联动管理器 */
    public void register() {
        TcdexIntegrationRegistry.register(this);
    }
}
