package org.tp.tcdex.modifier.melee;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import org.tp.tcdex.Tcdex;
import org.tp.tcdex.element.ElementManager;
import org.tp.tcdex.modifier.base.TcdexBaseModifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.tools.stats.ToolType;

/**
 * 合成感受器 (synthetic_hands)：命运2 泰坦异域臂铠 Synthoceps 效果。
 *
 * <p>围攻加成（命运2 数值）：被 <b>3 个或更多</b>敌对生物包围时（含其他 mod 生物），
 * 近战伤害 <b>+200%</b>（×3）；近战击杀回复 2 点生命。仅对近战工具生效。</p>
 */
public class SynthoModifier extends TcdexBaseModifier {

    /** 仅近战工具生效 */
    public static final ToolType[] CAN_BE_USE_ON_TYPES = {ToolType.MELEE};

    /** 敌人计数半径（格） */
    private static final float RADIUS = 4.0f;
    /** 围攻触发所需的最小附近敌人数量（命运2：3 个或更多） */
    private static final int TRIGGER_ENEMIES = 3;
    /** 围攻加成倍率（命运2 Synthoceps 削弱后数值）：+200% = ×3 */
    private static final float SURROUNDED_BONUS = 3.0f;
    /** 击杀回复生命值 */
    private static final float HEAL_AMOUNT = 2.0f;

    /** 通过 Tinkers 注册事件注册此 Modifier */
    public static void registerModifier(ModifierManager.ModifierRegistrationEvent event) {
        event.registerStatic(new ModifierId(Tcdex.MODID, "synthetic_hands"), new SynthoModifier());
    }

    /** 无等级词条：显示名不附带等级 */
    @Override
    public Component getDisplayName(int level) {
        return super.getDisplayName();
    }

    /** 围攻加成（命运2）：被 3 个或更多敌对生物包围时，近战伤害 ×3（+200%） */
    @Override
    protected float modifierMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        if (!canModified(tool)) {
            return damage;
        }
        LivingEntity attacker = context.getAttacker();
        if (attacker == null || attacker.level().isClientSide) {
            return damage;
        }
        int enemies = countNearbyEnemies(attacker);
        if (enemies >= TRIGGER_ENEMIES) {
            return damage * SURROUNDED_BONUS;
        }
        return damage;
    }

    /** 击杀回血：本工具造成的击杀回复 2 点生命 */
    @Override
    protected void modifierOnKillLivingTarget(IToolStackView tool, LivingDeathEvent event, LivingEntity attacker, LivingEntity target, int level) {
        if (!canModified(tool)) {
            return;
        }
        if (event.getSource().getEntity() != attacker) {
            return; // 只处理本工具造成的击杀
        }
        if (attacker.level().isClientSide) {
            return;
        }
        attacker.heal(HEAL_AMOUNT);
    }

    /** 统计 4 格内敌对生物数量（Enemy 接口或 MONSTER 分类，兼容其他 mod 生物） */
    private static int countNearbyEnemies(LivingEntity self) {
        int count = 0;
        for (LivingEntity entity : self.level().getEntitiesOfClass(LivingEntity.class, self.getBoundingBox().inflate(RADIUS),
                e -> e != self && e.isAlive() && !(e instanceof Player) && ElementManager.isMonster(e))) {
            count++;
        }
        return count;
    }

    /** 是否为可用工具（近战） */
    private static boolean canModified(IToolStackView tool) {
        return ToolType.from(tool.getItem(), CAN_BE_USE_ON_TYPES) != null;
    }
}
