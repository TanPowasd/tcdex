package org.tp.tcdex.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.tp.tcdex.Tcdex;

/**
 * TCDEX 自定义药水效果注册。
 */
public final class TcdexEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Tcdex.MODID);

    /** 吞噬（Devour）：击杀带虚空标记目标回满生命并刷新时长 */
    public static final RegistryObject<DevourEffect> DEVOUR =
            EFFECTS.register("devour", DevourEffect::new);

    private TcdexEffects() {
    }

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }
}
