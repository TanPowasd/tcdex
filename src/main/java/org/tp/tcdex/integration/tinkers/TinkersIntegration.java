package org.tp.tcdex.integration.tinkers;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import org.tp.tcdex.api.ITcdexIntegration;
import org.tp.tcdex.integration.tinkers.TinkersBridge;
import org.tp.tcdex.integration.tinkers.TinkersBridgeHolder;
import org.tp.tcdex.integration.tinkers.event.TcdexHookEvents;
import org.tp.tcdex.integration.tinkers.event.WarBannerEvents;
import org.tp.tcdex.integration.tinkers.modifier.ModifierExclusivity;
import org.tp.tcdex.integration.tinkers.modifier.elemental.ElementalModifier;
import org.tp.tcdex.integration.tinkers.modifier.elemental.FiveForcesModifier;
import org.tp.tcdex.integration.tinkers.modifier.elemental.PrismResonanceModifier;
import org.tp.tcdex.integration.tinkers.modifier.melee.ArcAmplifierModifier;
import org.tp.tcdex.integration.tinkers.modifier.melee.BurningFistsModifier;
import org.tp.tcdex.integration.tinkers.modifier.melee.BurstBarrierModifier;
import org.tp.tcdex.integration.tinkers.modifier.melee.CombatEchoModifier;
import org.tp.tcdex.integration.tinkers.modifier.melee.EagerEdgeModifier;
import org.tp.tcdex.integration.tinkers.modifier.melee.KineticSiphonModifier;
import org.tp.tcdex.integration.tinkers.modifier.melee.KineticTremorsModifier;
import org.tp.tcdex.integration.tinkers.modifier.melee.SynthoModifier;
import org.tp.tcdex.integration.tinkers.modifier.special.AllPermittedModifier;
import org.tp.tcdex.integration.tinkers.modifier.special.ElementalMasteryModifier;
import org.tp.tcdex.integration.tinkers.modifier.special.WarBannerModifier;

/**
 * Tinkers Construct 联动 add 包。
 *
 * <p>当检测到 tconstruct 加载时由 {@link org.tp.tcdex.integration.IntegrationManager} 调用，
 * 负责注册全部匠魂词条、互斥关系与事件。</p>
 */
public class TinkersIntegration implements ITcdexIntegration {

    @Override
    public String getModId() {
        return "tconstruct";
    }

    @Override
    public void init(IEventBus modEventBus) {
        // 注册桥接实现
        TinkersBridgeHolder.set(new TinkersBridge());

        // 词条互斥关系（元素充能 ↔ 棱镜共鸣等）
        ModifierExclusivity.registerAll();

        // 注册全部匠魂词条
        modEventBus.addListener(EagerEdgeModifier::registerModifier);
        modEventBus.addListener(AllPermittedModifier::registerModifier);
        modEventBus.addListener(CombatEchoModifier::registerModifier);
        modEventBus.addListener(ElementalModifier::registerModifier);
        modEventBus.addListener(PrismResonanceModifier::registerModifier);
        modEventBus.addListener(FiveForcesModifier::registerModifier);
        modEventBus.addListener(SynthoModifier::registerModifier);
        modEventBus.addListener(BurningFistsModifier::registerModifier);
        modEventBus.addListener(ArcAmplifierModifier::registerModifier);
        modEventBus.addListener(BurstBarrierModifier::registerModifier);
        modEventBus.addListener(KineticTremorsModifier::registerModifier);
        modEventBus.addListener(KineticSiphonModifier::registerModifier);
        modEventBus.addListener(WarBannerModifier::registerModifier);
        modEventBus.addListener(ElementalMasteryModifier::registerModifier);

        // 注册 Tinkers 相关 Forge 事件
        MinecraftForge.EVENT_BUS.register(TcdexHookEvents.class);
        MinecraftForge.EVENT_BUS.register(WarBannerEvents.class);
    }
}
