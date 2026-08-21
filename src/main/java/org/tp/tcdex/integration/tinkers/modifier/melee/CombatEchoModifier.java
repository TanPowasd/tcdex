package org.tp.tcdex.integration.tinkers.modifier.melee;

import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.tp.tcdex.Tcdex;
import org.tp.tcdex.integration.tinkers.modifier.base.TcdexBaseModifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 战斗回响 (combat_echo)：近战命中后，按本击实际伤害的 50% 追加一次额外伤害。
 *
 * <p>追加伤害归属玩家（计入击杀/经验），忽略目标无敌帧但正常吃护甲等规则。
 * （移植自 Tprt-re Modifiers/re/combat_echo.java）</p>
 */
public class CombatEchoModifier extends TcdexBaseModifier {

    /** 回响伤害占本击实际伤害的比例 */
    private static final float ECHO_RATIO = 0.5F;

    /** 通过 Tinkers 注册事件注册此 Modifier */
    public static void registerModifier(ModifierManager.ModifierRegistrationEvent event) {
        event.registerStatic(new ModifierId(Tcdex.MODID, "combat_echo"), new CombatEchoModifier());
    }

    /** 无等级词条：显示名不附带等级 */
    @Override
    public Component getDisplayName(int level) {
        return super.getDisplayName();
    }

    @Override
    protected void modifierAfterMeleeHit(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier,
                                         @NotNull ToolAttackContext context, float damageDealt) {
        // 近战命中必为玩家攻击（怪物使用工具走 MONSTER_MELEE_HIT，不触发本词条）
        Player attacker = context.getPlayerAttacker();
        LivingEntity target = context.getLivingTarget();
        // 只有命中实体才有回响；目标已死或本击无伤害则跳过
        if (attacker == null || target == null || target.isDeadOrDying() || damageDealt <= 0.0F) return;
        Level level = target.level();
        if (level.isClientSide) return;

        // 伤害来源为玩家：归属击杀/经验给攻击者
        DamageSource source = attacker.damageSources().playerAttack(attacker);
        // 忽略无敌帧：清零后立即结算；不携带 bypasses 标记，其余规则（护甲等）正常生效
        target.invulnerableTime = 0;
        target.hurt(source, damageDealt * ECHO_RATIO);
    }
}
