package org.tp.tcdex.integration.tinkers.modifier.melee;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.tp.tcdex.Tcdex;
import org.tp.tcdex.damage.ModDamageSources;
import org.tp.tcdex.integration.tinkers.modifier.ModifierExclusivity;
import org.tp.tcdex.integration.tinkers.modifier.base.TcdexBaseModifier;
import org.tp.tcdex.integration.tinkers.modifier.hook.KineticAttackModifierHook;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.tools.stats.ToolType;

/**
 * 动能震颤 (kinetic_tremors)：命运2 Kinetic Tremors 武器特性（近战专属）。
 *
 * <p><b>叠层</b>：动能武器（无元素词条）近战命中同一目标 +1 层（上限 5），
 * 命中其他目标重置为 1；4 秒（80 tick）未继续命中当前目标则清层（BurningFists 衰减模式）。
 * <b>触发</b>：满 5 层 → 目标脚下地震波：3 格内敌人（不含玩家）受到 5 点动能伤害并击退，
 * 中心目标同样受伤。触发后清层，需重新积累。</p>
 *
 * <p>动能专属：与元素充能 / 棱镜共鸣互斥（见 {@link ModifierExclusivity}），
 * 运行时另有 {@link KineticAttackModifierHook#isKineticTool} 兜底判定。</p>
 */
public class KineticTremorsModifier extends TcdexBaseModifier {

    /** 仅近战工具生效 */
    public static final ToolType[] CAN_BE_USE_ON_TYPES = {ToolType.MELEE};

    /** 当前叠层目标（工具持久 NBT，UUID 字符串） */
    private static final ResourceLocation TARGET_KEY = ResourceLocation.fromNamespaceAndPath(Tcdex.MODID, "tremor_target");
    /** 当前层数（工具持久 NBT） */
    private static final ResourceLocation STACKS_KEY = ResourceLocation.fromNamespaceAndPath(Tcdex.MODID, "tremor_stacks");
    /** 衰减计时器（工具持久 NBT） */
    private static final ResourceLocation DECAY_KEY = ResourceLocation.fromNamespaceAndPath(Tcdex.MODID, "tremor_decay");

    /** 触发所需层数（命运2：连续命中积累） */
    private static final int MAX_STACKS = 5;
    /** 衰减间隔（tick，80 = 4 秒） */
    private static final int DECAY_TICKS = 80;
    /** 地震波伤害（中心目标与周围一致） */
    private static final float BURST_DAMAGE = 5.0f;
    /** 地震波半径（格） */
    private static final float BURST_RADIUS = 3.0f;
    /** 击退力度 */
    private static final double KNOCKBACK = 0.8;

    /** 通过 Tinkers 注册事件注册此 Modifier */
    public static void registerModifier(ModifierManager.ModifierRegistrationEvent event) {
        event.registerStatic(new ModifierId(Tcdex.MODID, "kinetic_tremors"), new KineticTremorsModifier());
    }

    /** 无等级词条：显示名不附带等级 */
    @Override
    public Component getDisplayName(int level) {
        return super.getDisplayName();
    }

    /** 互斥校验：与元素充能/棱镜共鸣互斥（动能专属词条） */
    @Override
    protected Component modifierValidate(IToolStackView tool, ModifierEntry modifier) {
        return ModifierExclusivity.validate(tool, modifier);
    }

    /** 近战命中叠层：同一目标 +1，满 5 层触发地震波 */
    @Override
    protected void modifierAfterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        if (!canModified(tool) || !KineticAttackModifierHook.isKineticTool(tool)) {
            return;
        }
        LivingEntity attacker = context.getAttacker();
        LivingEntity target = context.getLivingTarget();
        if (attacker == null || target == null || attacker.level().isClientSide) {
            return;
        }
        if (target.isDeadOrDying() || damageDealt <= 0.0F) {
            return;
        }

        ModDataNBT data = tool.getPersistentData();
        String targetId = target.getStringUUID();
        int stacks = data.getInt(STACKS_KEY);
        if (!targetId.equals(data.getString(TARGET_KEY))) {
            stacks = 0; // 换目标：从 0 重新叠
            data.putString(TARGET_KEY, targetId);
        }
        stacks++;
        data.putInt(STACKS_KEY, stacks);
        data.putInt(DECAY_KEY, 0); // 命中刷新衰减计时

        if (stacks >= MAX_STACKS) {
            data.remove(TARGET_KEY);
            data.remove(STACKS_KEY);
            triggerTremor(target, attacker);
        }

        // 写回手持物品（与 BurningFists 击杀写回模式一致）
        if (tool instanceof ToolStack toolStack) {
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

    /** 衰减：4 秒未继续命中当前目标 → 清层（服务端） */
    @Override
    protected void modifierOnInventoryTick(IToolStackView tool, ModifierEntry modifier, Level world, LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
        if (world.isClientSide || !canModified(tool) || !KineticAttackModifierHook.isKineticTool(tool)) {
            return;
        }
        ModDataNBT data = tool.getPersistentData();
        if (data.getInt(STACKS_KEY) > 0) {
            int decay = data.getInt(DECAY_KEY) + 1;
            if (decay >= DECAY_TICKS) {
                data.remove(TARGET_KEY);
                data.remove(STACKS_KEY);
                data.putInt(DECAY_KEY, 0);
            } else {
                data.putInt(DECAY_KEY, decay);
            }
            if (tool instanceof ToolStack toolStack) {
                toolStack.updateStack(stack);
            }
        }
    }

    /** 地震波：中心目标 + 3 格内敌人（不含玩家）受到 5 点动能伤害并击退 */
    private static void triggerTremor(LivingEntity center, LivingEntity attacker) {
        Level level = center.level();
        DamageSource source = ModDamageSources.kinetic(attacker);
        // 中心目标先结算
        center.hurt(source, BURST_DAMAGE);
        // 周围敌人（不含玩家，命运2 语义）
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                center.getBoundingBox().inflate(BURST_RADIUS),
                e -> e != center && e.isAlive() && !(e instanceof Player))) {
            entity.hurt(source, BURST_DAMAGE);
            entity.knockback(KNOCKBACK, center.getX() - entity.getX(), center.getZ() - entity.getZ());
        }
        level.playSound(null, center.getX(), center.getY(), center.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.8F, 1.2F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, center.getX(), center.getY() + 0.3, center.getZ(), 12, 0.5, 0.1, 0.5, 0.05);
            serverLevel.sendParticles(ParticleTypes.CRIT, center.getX(), center.getY() + 0.5, center.getZ(), 20, 0.4, 0.2, 0.4, 0.3);
        }
    }

    /** 是否为可用工具（近战） */
    private static boolean canModified(IToolStackView tool) {
        return ToolType.from(tool.getItem(), CAN_BE_USE_ON_TYPES) != null;
    }
}
