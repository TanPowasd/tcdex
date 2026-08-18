package org.tp.tcdex.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.tp.tcdex.api.TcdexElementAPI;
import org.tp.tcdex.element.ElementType;

/**
 * 其他 mod 软联动（不作为前置依赖；未安装对应 mod 时自动跳过，零类引用零风险）。
 *
 * <p><b>冰与火之舞（iceandfire）</b>：
 * <ul>
 *   <li>火龙 fire_dragon → 烈日盾/攻击（弱冰影，护盾同源自动生效）</li>
 *   <li>冰龙 ice_dragon → 冰影盾/攻击</li>
 *   <li>闪电龙 lightning_dragon → 电弧盾/攻击</li>
 * </ul>
 * 龙类体型天然高光等（世界光等场 × 生物系数），可在配置 monsterBaseLights 中微调。</p>
 *
 * <p><b>铁魔法（irons_spellbooks）</b>：法术命中元素化见 {@link CompatEvents}。</p>
 */
public final class TcdexCompat {

    private TcdexCompat() {
    }

    /** mod 构造时调用（注册时机须在实体生成前，静态注册表） */
    public static void init() {
        if (ModList.get().isLoaded("iceandfire")) {
            registerIceAndFire();
        }
        // 铁魔法法术元素化在 CompatEvents（事件层按实体 id 匹配，无需注册）
    }

    /** 冰与火之舞：龙类元素护盾（分配链"提供器"层，攻击元素同源自动生效） */
    private static void registerIceAndFire() {
        TcdexElementAPI.registerShieldProvider(entity -> {
            ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
            if (key == null || !"iceandfire".equals(key.getNamespace())) {
                return null;
            }
            return switch (key.getPath()) {
                case "fire_dragon" -> ElementType.SOLAR;          // 火龙：烈日
                case "ice_dragon" -> ElementType.STASIS;          // 冰龙：冰影
                case "lightning_dragon" -> ElementType.ARC;       // 闪电龙：电弧
                default -> null;                                  // 其余不接管（走加权随机）
            };
        });
    }

    /** 供事件层复用：实体注册名是否属于指定 mod */
    public static boolean isFromMod(LivingEntity entity, String modId) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return key != null && modId.equals(key.getNamespace());
    }
}
