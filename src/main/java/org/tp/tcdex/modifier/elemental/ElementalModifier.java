package org.tp.tcdex.modifier.elemental;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.tp.tcdex.Tcdex;
import org.tp.tcdex.effect.TcdexEffects;
import org.tp.tcdex.element.ElementManager;
import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.modifier.base.TcdexBaseModifier;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 元素充能 (elemental)：武器打上本词条后，随机获得一种元素能力（烈日/电弧/虚空/冰影/缚丝），
 * **一旦确定无法改变**（固化在工具持久 NBT）。
 *
 * <p>元素决定攻击伤害类型（由 {@link org.tp.tcdex.event.ElementalDamageEvents} 把动能伤害
 * 转化为对应元素伤害，吃元素抗性/弱点），命中时施加状态效果、粒子与元素状态
 * （Ignite/冻结/Volatile/Jolt/Sever 联动）。</p>
 *
 * <p>固化时机：服务端首次生效（首次攻击/转化）时随机并写入工具 NBT；
 * 客户端显示名只读已固化元素，避免两端随机不一致。</p>
 */
public class ElementalModifier extends TcdexBaseModifier {

    /** 工具持久数据中固化的元素 key（写入后不可改变；命令/API 可据此指定） */
    public static final ResourceLocation ELEMENT_KEY = ResourceLocation.fromNamespaceAndPath(Tcdex.MODID, "elemental_element");

    /** 吞噬 buff 刷新时长（tick，10 秒） */
    private static final int DEVOUR_DURATION = 200;

    public ElementalModifier() {
    }

    /** 通过 Tinkers 注册事件注册此 Modifier（单个随机元素词条） */
    public static void registerModifier(ModifierManager.ModifierRegistrationEvent event) {
        event.registerStatic(new ModifierId(Tcdex.MODID, "elemental"), new ElementalModifier());
    }

    /**
     * 获取工具固化的元素：已固化直接返回；未固化则按权重随机分配并写入工具 NBT（不可改变）。
     * <b>仅服务端调用</b>（命中/伤害转化路径），写入后需调用方 updateStack 保存。
     */
    public ElementType getElement(IToolStackView tool) {
        ElementType element = parseElement(tool.getPersistentData().getString(ELEMENT_KEY));
        if (element == null) {
            element = ElementManager.rollElement(RandomSource.create());
            tool.getPersistentData().putString(ELEMENT_KEY, element.getId());
        }
        return element;
    }

    /**
     * 从弹射物携带的工具数据读取固化元素（只读，不写入——远程路径由伤害转化事件固化）。
     * 未固化时按权重随机返回（不影响攻击类型判定）。
     */
    public ElementType getElement(ModDataNBT persistentData) {
        ElementType element = parseElement(persistentData.getString(ELEMENT_KEY));
        return element != null ? element : ElementManager.rollElement(RandomSource.create());
    }

    /** 显示名：已固化则附加元素名（只读，避免客户端固化与服务端不一致） */
    @Override
    protected Component modifierGetDisplayName(IToolStackView tool, ModifierEntry modifier, Component name, RegistryAccess registryAccess) {
        ElementType element = parseElement(tool.getPersistentData().getString(ELEMENT_KEY));
        if (element != null) {
            return name.copy().append(" (").append(Component.translatable("modifier.tcdex.elemental.element." + element.getId())).append(")");
        }
        return name;
    }

    /** Tooltip：显示词条已充能的元素（未充能时提示） */
    @Override
    protected void modifierAddTooltip(IToolStackView tool, ModifierEntry modifier, Player player, List<Component> tooltip, TooltipKey tooltipKey, TooltipFlag tooltipFlag) {
        ElementType element = parseElement(tool.getPersistentData().getString(ELEMENT_KEY));
        if (element != null) {
            Component elementName = Component.translatable("modifier.tcdex.elemental.element." + element.getId())
                    .withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.fromRgb(elementColor(element))));
            tooltip.add(Component.translatable("modifier.tcdex.elemental.tooltip.element", elementName));
        } else {
            tooltip.add(Component.translatable("modifier.tcdex.elemental.tooltip.uncharged"));
        }
    }

    /**
     * 物品栏每 tick：服务端尽早固化随机元素（打上词条放入物品栏后立即确定，
     * 无需等待首次攻击），并写回物品 NBT 供客户端 tooltip/显示名读取。
     */
    @Override
    protected void modifierOnInventoryTick(IToolStackView tool, ModifierEntry modifier, Level world, LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
        if (world.isClientSide) {
            return;
        }
        // 首次调用时随机并写入工具 NBT；已固化则为只读（零开销）
        getElement(tool);
        if (tool instanceof ToolStack toolStack) {
            toolStack.updateStack(stack);
        }
    }

    /** 命运2 元素色（tooltip 显示用） */
    private static int elementColor(ElementType element) {
        return switch (element) {
            case SOLAR -> 0xFFFF9A3C;
            case ARC -> 0xFF5CC8FF;
            case VOID -> 0xFF9B59B6;
            case STASIS -> 0xFF7FD8E6;
            case STRAND -> 0xFF8FDB6A;
        };
    }

    /** 无等级词条：显示名不附带等级 */
    @Override
    public Component getDisplayName(int level) {
        return super.getDisplayName();
    }

    /** 按元素 id 解析元素类型（solar/arc/void/stasis/strand），无效返回 null */
    public static ElementType parseElement(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        for (ElementType type : ElementType.values()) {
            if (type.getId().equals(id)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 命中结算（全元素关键词化）：只播放粒子并给目标叠加元素状态。
     * 灼烧 DoT / 冻结减速由 Mixin tick 结算，Volatile/Jolt/Shatter/Sever 由受击联动结算，
     * 伤害数值由伤害转化事件负责（动能 → 元素），modifier 本身不再施加任何即时效果。
     *
     * @param element 本武器固化的元素
     * @param target  受击目标
     */
    private void applyElement(ElementType element, LivingEntity target) {
        if (target.level().isClientSide) {
            return;
        }

        // 粒子
        if (target.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(element.getParticle(),
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    15, 0.3, 0.3, 0.3, 0.1);
        }

        // 施加元素状态（Mixin 注入怪物身上）：层数叠加，满 100 触发关键词（Ignite/冻结），
        // 标记型元素（虚空/电弧/缚丝）由 ElementalStateEvents 在受击时联动结算
        IElementalEntity.of(target).addElementState(element, element.getStacksPerHit(), element.getStateDuration());
    }

    @Override
    protected void modifierAfterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        LivingEntity attacker = context.getAttacker();
        LivingEntity target = context.getLivingTarget();
        if (attacker == null || target == null) {
            return;
        }
        applyElement(getElement(tool), target);
    }

    @Override
    protected boolean modifierOnProjectileHitEntity(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier,
                                                    Projectile projectile, EntityHitResult hit,
                                                    @Nullable LivingEntity attacker, @Nullable LivingEntity target) {
        if (attacker == null || target == null) {
            return false;
        }
        applyElement(getElement(persistentData), target);
        return false;
    }

    /**
     * 击杀联动（命运2 关键词，需目标带本武器元素标记）：
     * - 电弧：Amplified 强化（移速 + 攻速 5 秒）
     * - 虚空：Devour 吞噬——玩家持有「吞噬」buff 时，击杀带虚空标记目标 → 回满生命 + 刷新 buff 时长
     * - 缚丝：Woven Mail 织甲（抗性 I 5 秒，≈20% 减伤）
     */
    @Override
    protected void modifierOnKillLivingTarget(IToolStackView tool, net.minecraftforge.event.entity.living.LivingDeathEvent event,
                                              LivingEntity attacker, LivingEntity target, int level) {
        if (event.getSource().getEntity() != attacker) {
            return; // 只处理本工具造成的击杀
        }
        if (attacker.level().isClientSide) {
            return;
        }
        ElementType element = parseElement(tool.getPersistentData().getString(ELEMENT_KEY));
        if (element == null) {
            return;
        }
        IElementalEntity targetData = IElementalEntity.of(target);
        switch (element) {
            case ARC -> {
                if (targetData.getElementStacks(ElementType.ARC) > 0) {
                    attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1, false, true));
                    attacker.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 100, 1, false, true));
                }
            }
            case VOID -> {
                // 吞噬：需玩家持有「吞噬」buff 才触发
                if (targetData.getElementStacks(ElementType.VOID) > 0 && attacker.hasEffect(TcdexEffects.DEVOUR.get())) {
                    // 回复满生命值
                    attacker.setHealth(attacker.getMaxHealth());
                    // 刷新吞噬 buff 时长（保留原 amplifier）
                    MobEffectInstance current = attacker.getEffect(TcdexEffects.DEVOUR.get());
                    int amplifier = current != null ? current.getAmplifier() : 0;
                    attacker.addEffect(new MobEffectInstance(TcdexEffects.DEVOUR.get(), DEVOUR_DURATION, amplifier, false, true));
                }
            }
            case STRAND -> {
                if (targetData.getElementStacks(ElementType.STRAND) > 0) {
                    attacker.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0, false, true));
                }
            }
            default -> {
            }
        }
    }
}
