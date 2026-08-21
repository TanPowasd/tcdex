package org.tp.tcdex.integration.tinkers.modifier.melee;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import org.tp.tcdex.Tcdex;
import org.tp.tcdex.integration.tinkers.modifier.ModifierExclusivity;
import org.tp.tcdex.integration.tinkers.modifier.base.TcdexBaseModifier;
import org.tp.tcdex.integration.tinkers.modifier.hook.KineticAttackModifierHook;
import org.tp.tcdex.shield.PlayerShieldManager;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 动能虹吸 (kinetic_siphon)：命运2 Kinetic Siphon 模组效果（击杀产出光球 → 资源循环）。
 *
 * <p>动能武器（无元素词条）击杀任意生物 → 回复 2 点玩家护盾（命运2：动能快速击杀产出光球，
 * 这里映射为护盾资源，与玩家护盾体系联动）。近战/远程击杀均生效（KILLING_HOOK 派发）。</p>
 *
 * <p>动能专属：与元素充能 / 棱镜共鸣互斥（见 {@link ModifierExclusivity}），
 * 运行时另有 {@link KineticAttackModifierHook#isKineticTool} 兜底判定。</p>
 */
public class KineticSiphonModifier extends TcdexBaseModifier {

    /** 每次动能击杀回复的护盾值 */
    private static final float SIPHON_AMOUNT = 2.0f;

    /** 通过 Tinkers 注册事件注册此 Modifier */
    public static void registerModifier(ModifierManager.ModifierRegistrationEvent event) {
        event.registerStatic(new ModifierId(Tcdex.MODID, "kinetic_siphon"), new KineticSiphonModifier());
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

    /** 动能击杀 → 回复玩家护盾（本工具造成的击杀，近战/远程均可） */
    @Override
    protected void modifierOnKillLivingTarget(IToolStackView tool, LivingDeathEvent event, LivingEntity attacker, LivingEntity target, int level) {
        if (!KineticAttackModifierHook.isKineticTool(tool)) {
            return;
        }
        if (event.getSource().getEntity() != attacker) {
            return; // 只处理本工具造成的击杀
        }
        if (attacker.level().isClientSide || !(attacker instanceof Player player)) {
            return;
        }
        if (!PlayerShieldManager.isEnabled()) {
            return;
        }

        // 回复护盾（钳制到上限）
        PlayerShieldManager.setShield(player, PlayerShieldManager.getShield(player) + SIPHON_AMOUNT);

        // 演出：光球回收
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7F, 1.4F);
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.GLOW, player.getX(), player.getY() + 1.2, player.getZ(), 10, 0.4, 0.4, 0.4, 0.02);
        }
    }
}
