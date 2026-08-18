package org.tp.tcdex.modifier.hook;

import net.minecraft.resources.ResourceLocation;
import org.tp.tcdex.Tcdex;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.module.ModuleHook;

/**
 * TCDEX 自定义 ModuleHook 注册中心。
 *
 * <p>通过 {@code ModifierHooks.register} 注册自定义 hook 类型，
 * 词条在 registerHooks 里 {@code addHook(this, TcdexHooks.KILLING_HOOK)} 声明，
 * 触发点由 {@link org.tp.tcdex.event.TcdexHookEvents} 在 Forge 事件中手动派发。</p>
 */
public final class TcdexHooks {

    /** 击杀 hook：工具击杀生物后触发 */
    public static final ModuleHook<KillingHook> KILLING_HOOK;

    /** 元素攻击 hook：词条调整元素伤害/护盾破盾效率 */
    public static final ModuleHook<ElementalAttackModifierHook> ELEMENTAL_ATTACK;

    /** 破盾 hook：护盾打穿时词条调整破盾爆炸伤害/触发联动 */
    public static final ModuleHook<ShieldBreakHook> SHIELD_BREAK;

    /** 玩家护盾 hook：词条调整玩家护盾吸收量/脱战回复速率 */
    public static final ModuleHook<PlayerShieldHook> PLAYER_SHIELD;

    /** 元素状态施加 hook：词条调整自己施加的元素状态层数/时长 */
    public static final ModuleHook<ElementalStateApplyHook> ELEMENTAL_STATE_APPLY;

    static {
        KILLING_HOOK = ModifierHooks.register(
                ResourceLocation.fromNamespaceAndPath(Tcdex.MODID, "killing_hook"),
                KillingHook.class,
                KillingHook.AllMerger::new,
                new KillingHook() {
                }
        );
        ELEMENTAL_ATTACK = ModifierHooks.register(
                ResourceLocation.fromNamespaceAndPath(Tcdex.MODID, "elemental_attack"),
                ElementalAttackModifierHook.class,
                ElementalAttackModifierHook.AllMerger::new,
                new ElementalAttackModifierHook() {
                }
        );
        SHIELD_BREAK = ModifierHooks.register(
                ResourceLocation.fromNamespaceAndPath(Tcdex.MODID, "shield_break"),
                ShieldBreakHook.class,
                ShieldBreakHook.AllMerger::new,
                new ShieldBreakHook() {
                }
        );
        PLAYER_SHIELD = ModifierHooks.register(
                ResourceLocation.fromNamespaceAndPath(Tcdex.MODID, "player_shield"),
                PlayerShieldHook.class,
                PlayerShieldHook.AllMerger::new,
                new PlayerShieldHook() {
                }
        );
        ELEMENTAL_STATE_APPLY = ModifierHooks.register(
                ResourceLocation.fromNamespaceAndPath(Tcdex.MODID, "elemental_state_apply"),
                ElementalStateApplyHook.class,
                ElementalStateApplyHook.AllMerger::new,
                new ElementalStateApplyHook() {
                }
        );
    }

    private TcdexHooks() {
    }
}
