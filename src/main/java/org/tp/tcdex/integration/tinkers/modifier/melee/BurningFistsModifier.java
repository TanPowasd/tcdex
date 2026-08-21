package org.tp.tcdex.integration.tinkers.modifier.melee;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import org.tp.tcdex.Tcdex;
import org.tp.tcdex.integration.tinkers.modifier.base.TcdexBaseModifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

/**
 * 燃烧之拳 (burning_fists)：命运2 泰坦异域臂铠虫神爱抚（Wormgod Caress）效果。
 *
 * <p><b>有等级词条</b>（1-3 级）：
 * <ul>
 *   <li><b>击杀叠层</b>：近战攻击/终结技（MC 以近战击杀计）击败目标 → 燃烧层数 +1（上限 5 层）</li>
 *   <li><b>近战伤害</b>：每层 +40%（命运2 数值），加成随时间衰减（每 2 秒 -1 层）</li>
 *   <li><b>武器伤害（等级效果）</b>：等级 2 起提供额外工具攻击力（+2 / 等级 3 +4）——
 *       "更高等级的燃烧之拳提供更高的武器伤害"</li>
 * </ul></p>
 */
public class BurningFistsModifier extends TcdexBaseModifier {

    /** 燃烧层数（工具持久 NBT） */
    private static final ResourceLocation STACKS_KEY = ResourceLocation.fromNamespaceAndPath(Tcdex.MODID, "burning_fists_stacks");
    /** 衰减计时器（工具持久 NBT） */
    private static final ResourceLocation DECAY_KEY = ResourceLocation.fromNamespaceAndPath(Tcdex.MODID, "burning_fists_decay");

    /** 层数上限（命运2：5 层） */
    private static final int MAX_STACKS = 5;
    /** 每层近战伤害加成（命运2 数值：+40%） */
    private static final float MELEE_BONUS_PER_STACK = 0.4f;
    /** 衰减间隔（tick，40 = 2 秒衰减 1 层） */
    private static final int DECAY_INTERVAL = 40;
    /** 等级 2 武器攻击力加成 */
    private static final float WEAPON_DAMAGE_LV2 = 2.0f;
    /** 等级 3 武器攻击力加成 */
    private static final float WEAPON_DAMAGE_LV3 = 4.0f;

    /** 通过 Tinkers 注册事件注册此 Modifier */
    public static void registerModifier(ModifierManager.ModifierRegistrationEvent event) {
        event.registerStatic(new ModifierId(Tcdex.MODID, "burning_fists"), new BurningFistsModifier());
    }

    // 有等级词条：显示名默认附带等级（Burning Fists II / III），无需覆写 getDisplayName(int)

    /** 近战伤害：× (1 + 层数 × 40%) */
    @Override
    protected float modifierMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        int stacks = tool.getPersistentData().getInt(STACKS_KEY);
        if (stacks <= 0) {
            return damage;
        }
        return damage * (1.0f + stacks * MELEE_BONUS_PER_STACK);
    }

    /**
     * 近战击杀叠层：只计算<b>近战</b>击杀（直接伤害来源 = 攻击者本人，命运2 近战/终结技语义；
     * 弹射物击杀不叠层），层数 +1（上限 5），并写回手持物品 NBT。
     */
    @Override
    protected void modifierOnKillLivingTarget(IToolStackView tool, LivingDeathEvent event, LivingEntity attacker, LivingEntity target, int level) {
        if (event.getSource().getEntity() != attacker) {
            return; // 只处理本工具造成的击杀
        }
        if (event.getSource().getDirectEntity() != attacker) {
            return; // 仅近战击杀叠层（终结技无对应，以近战计）
        }
        if (attacker.level().isClientSide) {
            return;
        }
        ModDataNBT data = tool.getPersistentData();
        data.putInt(STACKS_KEY, Math.min(MAX_STACKS, data.getInt(STACKS_KEY) + 1));
        data.putInt(DECAY_KEY, 0); // 击杀刷新衰减计时
        if (tool instanceof ToolStack toolStack) {
            // 写回手持物品（击杀 hook 无 stack 参数，从攻击者主手/副手定位）
            ItemStack main = attacker.getMainHandItem();
            if (!main.isEmpty() && main.getItem() == tool.getItem()) {
                toolStack.updateStack(main);
            } else {
                ItemStack off = attacker.getOffhandItem();
                if (!off.isEmpty() && off.getItem() == tool.getItem()) {
                    toolStack.updateStack(off);
                }
            }
        }
    }

    /** 燃烧衰减：每 DECAY_INTERVAL tick 层数 -1（服务端），并写回物品 NBT */
    @Override
    protected void modifierOnInventoryTick(IToolStackView tool, ModifierEntry modifier, Level world, LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
        if (world.isClientSide) {
            return;
        }
        ModDataNBT data = tool.getPersistentData();
        int stacks = data.getInt(STACKS_KEY);
        if (stacks > 0) {
            int timer = data.getInt(DECAY_KEY) + 1;
            if (timer >= DECAY_INTERVAL) {
                data.putInt(DECAY_KEY, 0);
                data.putInt(STACKS_KEY, stacks - 1);
            } else {
                data.putInt(DECAY_KEY, timer);
            }
            if (tool instanceof ToolStack toolStack) {
                toolStack.updateStack(stack);
            }
        } else {
            data.putInt(DECAY_KEY, 0);
        }
    }

    /** 武器伤害（等级效果）：等级 2 起增加工具攻击力（+2 / 等级 3 +4） */
    @Override
    protected void modifierAddToolStats(IToolContext context, ModifierEntry modifier, ModifierStatsBuilder builder) {
        if (modifier.getLevel() >= 2) {
            ToolStats.ATTACK_DAMAGE.add(builder, modifier.getLevel() >= 3 ? WEAPON_DAMAGE_LV3 : WEAPON_DAMAGE_LV2);
        }
    }
}
