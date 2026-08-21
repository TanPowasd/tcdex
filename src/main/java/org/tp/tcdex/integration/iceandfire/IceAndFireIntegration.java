package org.tp.tcdex.integration.iceandfire;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.ForgeRegistries;
import org.tp.tcdex.api.ITcdexIntegration;
import org.tp.tcdex.api.TcdexElementAPI;
import org.tp.tcdex.element.ElementType;

/**
 * 冰与火之舞联动 add 包。
 *
 * <p>深化内容：</p>
 * <ul>
 *   <li>更多冰火生物获得元素护盾 / 元素攻击（护盾提供器）</li>
 *   <li>龙类等关键生物拥有元素抗性与弱点</li>
 *   <li>龙息 / 龙爪等冰火伤害参与元素反应、元素护盾破盾、元素能量</li>
 * </ul>
 */
public class IceAndFireIntegration implements ITcdexIntegration {

    @Override
    public String getModId() {
        return "iceandfire";
    }

    @Override
    public void init(IEventBus modEventBus) {
        MinecraftForge.EVENT_BUS.register(IceAndFireEvents.class);

        TcdexElementAPI.registerShieldProvider(entity -> {
            ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
            if (key == null || !"iceandfire".equals(key.getNamespace())) {
                return null;
            }
            return switch (key.getPath()) {
                case "fire_dragon", "ifrit", "salamander" -> ElementType.SOLAR;
                case "ice_dragon" -> ElementType.STASIS;
                case "lightning_dragon" -> ElementType.ARC;
                case "sea_serpent", "amphithere", "siren" -> ElementType.TIDE;
                case "hippogryph", "pixie", "stymphalian_bird" -> ElementType.MISTFLOW;
                case "myrmex_worker", "myrmex_soldier", "myrmex_queen", "hydra" -> ElementType.STRAND;
                case "deathworm", "ghost", "lich", "troll", "cyclops", "cockatrice", "gorgon" -> ElementType.VOID;
                default -> null;
            };
        });

        registerResistances();
    }

    private static void registerResistances() {
        // 龙类：本系抗性，互克系弱点
        TcdexElementAPI.registerResistance("iceandfire:fire_dragon", ElementType.SOLAR, 0.3f);
        TcdexElementAPI.registerResistance("iceandfire:fire_dragon", ElementType.STASIS, 1.8f);
        TcdexElementAPI.registerResistance("iceandfire:fire_dragon", ElementType.ARC, 1.2f);

        TcdexElementAPI.registerResistance("iceandfire:ice_dragon", ElementType.STASIS, 0.3f);
        TcdexElementAPI.registerResistance("iceandfire:ice_dragon", ElementType.SOLAR, 1.8f);
        TcdexElementAPI.registerResistance("iceandfire:ice_dragon", ElementType.ARC, 1.2f);

        TcdexElementAPI.registerResistance("iceandfire:lightning_dragon", ElementType.ARC, 0.3f);
        TcdexElementAPI.registerResistance("iceandfire:lightning_dragon", ElementType.VOID, 1.5f);
        TcdexElementAPI.registerResistance("iceandfire:lightning_dragon", ElementType.SOLAR, 1.2f);

        // 火系生物
        TcdexElementAPI.registerResistance("iceandfire:ifrit", ElementType.SOLAR, 0.5f);
        TcdexElementAPI.registerResistance("iceandfire:ifrit", ElementType.STASIS, 1.5f);
        TcdexElementAPI.registerResistance("iceandfire:salamander", ElementType.SOLAR, 0.5f);
        TcdexElementAPI.registerResistance("iceandfire:salamander", ElementType.STASIS, 1.5f);

        // 海洋 / 飞行
        TcdexElementAPI.registerResistance("iceandfire:sea_serpent", ElementType.TIDE, 0.5f);
        TcdexElementAPI.registerResistance("iceandfire:sea_serpent", ElementType.ARC, 1.5f);
        TcdexElementAPI.registerResistance("iceandfire:amphithere", ElementType.TIDE, 0.6f);
        TcdexElementAPI.registerResistance("iceandfire:amphithere", ElementType.ARC, 1.4f);

        // 虚空 / 亡灵
        TcdexElementAPI.registerResistance("iceandfire:ghost", ElementType.VOID, 0.5f);
        TcdexElementAPI.registerResistance("iceandfire:ghost", ElementType.SOLAR, 1.5f);
        TcdexElementAPI.registerResistance("iceandfire:lich", ElementType.VOID, 0.5f);
        TcdexElementAPI.registerResistance("iceandfire:lich", ElementType.SOLAR, 1.5f);
        TcdexElementAPI.registerResistance("iceandfire:deathworm", ElementType.VOID, 0.6f);
        TcdexElementAPI.registerResistance("iceandfire:deathworm", ElementType.SOLAR, 1.4f);

        // 自然 / 缚丝
        TcdexElementAPI.registerResistance("iceandfire:hydra", ElementType.STRAND, 0.6f);
        TcdexElementAPI.registerResistance("iceandfire:hydra", ElementType.SOLAR, 1.5f);
        TcdexElementAPI.registerResistance("iceandfire:myrmex_worker", ElementType.STRAND, 0.7f);
        TcdexElementAPI.registerResistance("iceandfire:myrmex_soldier", ElementType.STRAND, 0.6f);
        TcdexElementAPI.registerResistance("iceandfire:myrmex_queen", ElementType.STRAND, 0.5f);
    }
}
