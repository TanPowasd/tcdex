package org.tp.tcdex.modifier;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.tp.tcdex.Tcdex;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 命运2风格词条：急切刀锋（Eager Edge）。
 *
 * <p>切换到武器后获得“急切”Buff，下一次近战命中时突进到目标面前；
 * 若没有目标则向面朝方向突进。突进后进入玩家全局冷却。</p>
 */
public class EagerEdgeModifier extends NoLevelsModifier implements InventoryTickModifierHook, MeleeHitModifierHook {

    /** 切换检测状态存放在工具持久 NBT */
    private static final ResourceLocation EAGER_WAS_HELD_KEY = ResourceLocation.fromNamespaceAndPath(Tcdex.MODID, "eager_was_held");

    private static final String BUFF_UNTIL_KEY = "tcdex_eager_buff_until";
    private static final String COOLDOWN_UNTIL_KEY = "tcdex_eager_cooldown_until";

    /** Buff 持续时间（tick） */
    private static final int BUFF_DURATION_TICKS = 60;
    /** 玩家全局冷却（tick） */
    private static final int COOLDOWN_TICKS = 100;
    /** 无目标时向前突进距离 */
    private static final double DASH_DISTANCE = 5.0;
    /** 有目标时停留在目标面前的距离 */
    private static final double TARGET_FRONT_DISTANCE = 0.5;

    public EagerEdgeModifier() {
    }

    /** 通过 Tinkers 注册事件注册此 Modifier */
    public static void registerModifier(ModifierManager.ModifierRegistrationEvent event) {
        event.registerStatic(new ModifierId(Tcdex.MODID, "eager_edge"), new EagerEdgeModifier());
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder builder) {
        super.registerHooks(builder);
        builder.addHook(this, new ModuleHook[]{
                ModifierHooks.INVENTORY_TICK,
                ModifierHooks.MELEE_HIT
        });
    }

    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level world, LivingEntity holder, int slot, boolean isSelected, boolean isHeld, ItemStack stack) {
        if (world.isClientSide || !(holder instanceof Player player)) {
            return;
        }

        long now = world.getGameTime();
        CompoundTag playerData = player.getPersistentData();

        long cooldownUntil = playerData.getLong(COOLDOWN_UNTIL_KEY);
        long buffUntil = playerData.getLong(BUFF_UNTIL_KEY);

        // 清理过期状态
        if (cooldownUntil <= now) {
            playerData.remove(COOLDOWN_UNTIL_KEY);
        }
        if (buffUntil <= now) {
            playerData.remove(BUFF_UNTIL_KEY);
        }

        // 检测武器是否刚从非手持切换为主手持有
        boolean wasHeld = tool.getPersistentData().getBoolean(EAGER_WAS_HELD_KEY);
        if (isHeld && !wasHeld && cooldownUntil <= now) {
            playerData.putLong(BUFF_UNTIL_KEY, now + BUFF_DURATION_TICKS);
        }
        tool.getPersistentData().putBoolean(EAGER_WAS_HELD_KEY, isHeld);
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        LivingEntity attacker = context.getAttacker();
        if (attacker == null || attacker.level().isClientSide || !(attacker instanceof Player player)) {
            return;
        }

        long now = attacker.level().getGameTime();
        CompoundTag playerData = player.getPersistentData();
        long buffUntil = playerData.getLong(BUFF_UNTIL_KEY);
        long cooldownUntil = playerData.getLong(COOLDOWN_UNTIL_KEY);

        // 没有 Buff 或还在冷却中则无法突进
        if (buffUntil <= now || cooldownUntil > now) {
            return;
        }

        LivingEntity target = context.getLivingTarget();
        if (target != null) {
            // 突进到目标面前
            Vec3 toTarget = target.position().subtract(player.position());
            if (toTarget.lengthSqr() > 0.0001) {
                toTarget = toTarget.normalize();
                double x = target.getX() - toTarget.x * TARGET_FRONT_DISTANCE;
                double z = target.getZ() - toTarget.z * TARGET_FRONT_DISTANCE;
                player.teleportTo(x, target.getY(), z);
            }
        } else {
            // 无目标时向前突进
            Vec3 look = player.getLookAngle();
            Vec3 pos = player.position().add(look.scale(DASH_DISTANCE));
            player.teleportTo(pos.x, pos.y, pos.z);
        }

        playerData.putLong(COOLDOWN_UNTIL_KEY, now + COOLDOWN_TICKS);
        playerData.remove(BUFF_UNTIL_KEY);
    }
}
