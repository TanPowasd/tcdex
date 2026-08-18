package org.tp.tcdex.modifier.elemental;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import org.tp.tcdex.Tcdex;
import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.modifier.ModifierExclusivity;
import org.tp.tcdex.modifier.base.TcdexBaseModifier;
import org.tp.tcdex.modifier.hook.TcdexHooks;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

import javax.annotation.Nullable;

/**
 * 棱镜共鸣 (prism_resonance)：棱镜伤害的专属词条（命运2 Prismatic）。
 *
 * <p>攻击整体转化为<b>棱镜伤害</b>（死亡消息/伤害类型区分）：
 * <ul>
 *   <li>破盾特化：棱镜伤害对任意元素护盾按匹配效率（×2）磨损，可<b>永久打破棱镜盾</b>
 *       （棱镜伤害打穿 → 护盾不再回复）</li>
 *   <li>命中施加棱镜标记 → 受击 Refract 折射（本击 25% 伤害溅射周围）</li>
 * </ul>
 * 与元素充能互不冲突：工具同时持有两者时棱镜共鸣优先（固定棱镜）。
 */
public class PrismResonanceModifier extends TcdexBaseModifier {

    public PrismResonanceModifier() {
    }

    /** 通过 Tinkers 注册事件注册此 Modifier */
    public static void registerModifier(ModifierManager.ModifierRegistrationEvent event) {
        event.registerStatic(new ModifierId(Tcdex.MODID, "prism_resonance"), new PrismResonanceModifier());
    }

    /** 无等级词条：显示名不附带等级 */
    @Override
    public Component getDisplayName(int level) {
        return super.getDisplayName();
    }

    /** 互斥校验（注册于 ModifierExclusivity）：与元素充能互斥，冲突时返回提示 */
    @Override
    protected Component modifierValidate(IToolStackView tool, ModifierEntry modifier) {
        return ModifierExclusivity.validate(tool, modifier);
    }

    /** 命中施加棱镜标记（折射联动见 ElementalStateEvents）；近战路径派发 ELEMENTAL_STATE_APPLY hook，远程不派发 */
    private static void applyPrism(IToolStackView tool, LivingEntity target) {
        if (target.level().isClientSide) {
            return;
        }
        if (target.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ElementType.PRISM.getParticle(),
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    15, 0.3, 0.3, 0.3, 0.1);
        }
        float stacks = ElementType.PRISM.getStacksPerHit();
        int duration = ElementType.PRISM.getStateDuration();
        if (tool != null) {
            for (ModifierEntry entry : tool.getModifierList()) {
                stacks = entry.getHook(TcdexHooks.ELEMENTAL_STATE_APPLY)
                        .modifyStateStacks(tool, entry, ElementType.PRISM, stacks);
                duration = entry.getHook(TcdexHooks.ELEMENTAL_STATE_APPLY)
                        .modifyStateDuration(tool, entry, ElementType.PRISM, duration);
            }
        }
        if (stacks > 0 && duration > 0) {
            IElementalEntity.of(target).addElementState(ElementType.PRISM, stacks, duration);
        }
    }

    @Override
    protected void modifierAfterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        LivingEntity attacker = context.getAttacker();
        LivingEntity target = context.getLivingTarget();
        if (attacker == null || target == null) {
            return;
        }
        applyPrism(tool, target);
    }

    @Override
    protected boolean modifierOnProjectileHitEntity(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier,
                                                    Projectile projectile, EntityHitResult hit,
                                                    @Nullable LivingEntity attacker, @Nullable LivingEntity target) {
        if (attacker == null || target == null) {
            return false;
        }
        applyPrism(null, target);
        return false;
    }
}
