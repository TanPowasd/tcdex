package org.tp.tcdex.modifier.melee;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.tp.tcdex.Tcdex;
import org.tp.tcdex.modifier.base.TcdexBaseModifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 爆裂屏障 (burst_barrier)：玩家护盾破碎联动词条（命运2 风格）。
 *
 * <p>通过 TCDEX 自定义 {@link org.tp.tcdex.modifier.hook.TcdexHooks#PLAYER_SHIELD_BREAK}：
 * <ul>
 *   <li><b>破碎格挡</b>：护盾被打穿时，溢出伤害 ×0.5（格挡一半，剩余结算到生命）</li>
 *   <li><b>碎裂冲击</b>：破碎瞬间以玩家为中心释放冲击波（3 格内敌人 4 点伤害，不含玩家）</li>
 * </ul>
 * 破碎后护盾照常脱战回复（回复机制不受影响），本词条只作用于破碎时刻。</p>
 */
public class BurstBarrierModifier extends TcdexBaseModifier {

    /** 溢出伤害减免比例（×0.5 = 格挡一半） */
    private static final float OVERFLOW_REDUCTION = 0.5f;
    /** 冲击波伤害 */
    private static final float BURST_DAMAGE = 4.0f;
    /** 冲击波半径（格） */
    private static final float BURST_RADIUS = 3.0f;

    /** 通过 Tinkers 注册事件注册此 Modifier */
    public static void registerModifier(ModifierManager.ModifierRegistrationEvent event) {
        event.registerStatic(new ModifierId(Tcdex.MODID, "burst_barrier"), new BurstBarrierModifier());
    }

    /** 无等级词条：显示名不附带等级 */
    @Override
    public Component getDisplayName(int level) {
        return super.getDisplayName();
    }

    /** 破碎格挡：溢出伤害 ×0.5 */
    @Override
    protected float modifierModifyBreakOverflow(IToolStackView tool, ModifierEntry modifier, Player player, DamageSource source, float overflow) {
        return overflow * OVERFLOW_REDUCTION;
    }

    /** 碎裂冲击：破碎瞬间 3 格内敌人受到 4 点伤害（不含玩家，命运2 语义） */
    @Override
    protected void modifierOnShieldBreak(IToolStackView tool, ModifierEntry modifier, Player player, DamageSource source, float overflow) {
        Level level = player.level();
        if (level.isClientSide) {
            return;
        }
        DamageSource burst = player.damageSources().indirectMagic(player, null);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(BURST_RADIUS),
                e -> e != player && e.isAlive() && !(e instanceof Player))) {
            entity.hurt(burst, BURST_DAMAGE);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.9F, 1.3F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 0.5, player.getZ(), 10, 0.5, 0.3, 0.5, 0.02);
        }
    }
}
