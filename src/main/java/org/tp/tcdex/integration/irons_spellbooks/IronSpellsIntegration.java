package org.tp.tcdex.integration.irons_spellbooks;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import org.tp.tcdex.api.ITcdexIntegration;

/**
 * 铁魔法联动 add 包。
 */
public class IronSpellsIntegration implements ITcdexIntegration {

    @Override
    public String getModId() {
        return "irons_spellbooks";
    }

    @Override
    public void init(IEventBus modEventBus) {
        MinecraftForge.EVENT_BUS.register(IronSpellsEvents.class);
    }
}
