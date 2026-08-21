package org.tp.tcdex.api;

/**
 * Tinkers 联动入口：Core 检测到 Tinkers 后调用。
 */
public interface ITinkersIntegration {

    void registerModifiers();

    void registerHooks();

    void registerRecipes();
}
