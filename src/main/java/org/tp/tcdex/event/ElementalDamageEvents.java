package org.tp.tcdex.event;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tp.tcdex.Tcdex;
import org.tp.tcdex.damage.ModDamageSources;
import org.tp.tcdex.debug.TcdexDebug;
import org.tp.tcdex.element.ElementManager;
import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.modifier.elemental.ElementalModifier;
import org.tp.tcdex.modifier.elemental.IElementalEntity;
import org.tp.tcdex.modifier.hook.TcdexHooks;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.List;
import java.util.Locale;

/**
 * 伤害类型体系（命运2：动能武器 vs 元素武器 + 元素护盾破盾）。
 *
 * <p><b>动能/元素</b>：匠魂武器的攻击伤害有明确类型——
 * 无元素词条为<b>动能</b>伤害；打上元素词条后整体转化为对应<b>元素</b>伤害
 * （数值 = 原伤害 × 元素抗性/弱点系数，伤害源区分死亡消息）。近战与远程均生效。</p>
 *
 * <p><b>元素护盾</b>：带护盾的怪物（ElementManager 护盾表，护盾量 = 最大生命 × 50%）：
 * <ul>
 *   <li>匹配元素攻击 → 破盾效率 ×2（命运2 匹配元素破盾）</li>
 *   <li>不匹配元素 / 动能攻击 → 破盾效率 ×0.5（打得慢）</li>
 *   <li>护盾打穿 → 破盾爆炸（10% 最大生命 AOE）+ 目标获得护盾元素状态</li>
 * </ul></p>
 */
@Mod.EventBusSubscriber(modid = Tcdex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ElementalDamageEvents {

    /** 匹配元素破盾效率 */
    private static final float MATCH_EFFICIENCY = 2.0f;
    /** 不匹配/动能破盾效率 */
    private static final float MISMATCH_EFFICIENCY = 0.5f;
    /** 破盾爆炸：目标最大生命 × 10% */
    private static final float BREAK_HEALTH_PERCENT = 0.1f;
    /** 破盾爆炸半径 */
    private static final float BREAK_RADIUS = 1.5f;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) {
            return;
        }

        // 已是 TCDEX 类型伤害（元素/动能/纯粹）→ 递归保护
        if (ModDamageSources.isElementDamage(event.getSource())
                || event.getSource().is(ModDamageSources.KINETIC_DAMAGE_TYPE)
                || event.getSource().is(ModDamageSources.PURE_DAMAGE_TYPE)) {
            return;
        }

        // 找攻击者：近战为玩家本人，远程为弹射物（归属玩家）
        Entity direct = event.getSource().getDirectEntity();
        Player player = null;
        if (direct instanceof Player p) {
            player = p;
        } else if (direct instanceof Projectile projectile && projectile.getOwner() instanceof Player p) {
            player = p;
        }
        if (player == null) {
            return;
        }

        // 玩家手持匠魂工具上的元素充能词条（主手优先；取第一个生效；null = 动能武器）
        // 首次生效时在工具 NBT 中固化随机元素（之后不可改变）
        ToolStack tool = null;
        ElementType element = null;
        for (ItemStack stack : List.of(player.getMainHandItem(), player.getOffhandItem())) {
            ToolStack candidate = asTool(stack);
            if (candidate == null) {
                continue;
            }
            ElementalModifier elemental = findElemental(candidate);
            if (elemental != null) {
                tool = candidate;
                element = elemental.getElement(candidate);
                candidate.updateStack(stack); // 固化写回
                break;
            }
            if (tool == null) {
                tool = candidate;
            }
        }

        // ===== 元素护盾结算（优先于伤害类型转化） =====
        IElementalEntity targetData = IElementalEntity.of(target);
        if (targetData.getShieldAmount() > 0 && targetData.getShieldElement() != null) {
            handleShield(event, target, targetData, player, tool, element);
            return;
        }

        // ===== 伤害类型转化：动能（默认）或元素（打上元素词条） =====
        float amount = event.getAmount();
        float resistance = 1.0f;
        if (element != null) {
            resistance = ElementManager.getResistance(target, element);
            amount *= resistance;
            // 元素攻击 hook 联动：工具上词条可调整元素伤害
            amount = dispatchElementalDamage(tool, element, amount);
        }
        event.setCanceled(true);
        target.invulnerableTime = 0;
        target.hurt(attackSource(player, element), amount);

        // 调试输出：伤害链路
        if (TcdexDebug.isElementalEnabled()) {
            player.sendSystemMessage(Component.literal(String.format(Locale.ROOT,
                    "[元素调试] %s 攻击 %s | 类型: %s | 抗性: %.2f | 伤害: %.2f → %.2f",
                    player.getDisplayName().getString(), target.getDisplayName().getString(),
                    element != null ? element.getId() : "kinetic", resistance,
                    event.getAmount(), amount)));
        }
    }

    /** 护盾结算：按匹配效率扣盾（可被元素攻击 hook 调整），打穿则破盾爆炸 + 溢出伤害回灌 */
    private static void handleShield(LivingHurtEvent event, LivingEntity target, IElementalEntity targetData,
                                     Player player, ToolStack tool, ElementType attackElement) {
        ElementType shieldElement = targetData.getShieldElement();
        float efficiency = attackElement == shieldElement ? MATCH_EFFICIENCY : MISMATCH_EFFICIENCY;
        // 元素攻击 hook 联动：工具上词条可调整破盾效率
        efficiency = dispatchShieldEfficiency(tool, shieldElement, efficiency);

        float amount = event.getAmount();
        event.setCanceled(true);
        target.invulnerableTime = 0;

        // 调试输出：护盾结算开始
        if (TcdexDebug.isElementalEnabled()) {
            player.sendSystemMessage(Component.literal(String.format(Locale.ROOT,
                    "[元素调试] 护盾: %s (%.1f) | 攻击元素: %s | 效率: %.2f | 本击: %.2f",
                    shieldElement.getId(), targetData.getShieldAmount(),
                    attackElement != null ? attackElement.getId() : "kinetic", efficiency, amount)));
        }

        // 伤害打入护盾（按效率换算），返回溢出
        float overflow = targetData.consumeShield(amount * efficiency);
        if (overflow > 0) {
            // 破盾：爆炸 + 目标获得护盾元素状态
            shieldBreak(target, shieldElement);
            // 溢出伤害按原效率折算回真实值，以攻击类型结算
            target.hurt(attackSource(player, attackElement), overflow / efficiency);
            if (TcdexDebug.isElementalEnabled()) {
                player.sendSystemMessage(Component.literal(String.format(Locale.ROOT,
                        "[元素调试] 破盾! 溢出伤害: %.2f | 目标获得 %s 状态",
                        overflow / efficiency, shieldElement.getId())));
            }
        } else {
            // 未打穿：护盾全吃，播放受击反馈
            Level level = target.level();
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 0.8F, 1.2F);
            if (TcdexDebug.isElementalEnabled()) {
                player.sendSystemMessage(Component.literal(String.format(Locale.ROOT,
                        "[元素调试] 护盾未破: 剩余 %.1f", targetData.getShieldAmount())));
            }
        }
    }

    /** 元素攻击 hook 派发：工具上所有词条链式调整元素伤害 */
    private static float dispatchElementalDamage(ToolStack tool, ElementType element, float amount) {
        if (tool == null) {
            return amount;
        }
        for (ModifierEntry entry : tool.getModifierList()) {
            amount = entry.getHook(TcdexHooks.ELEMENTAL_ATTACK).modifyElementalDamage(tool, entry, element, amount);
        }
        return amount;
    }

    /** 元素攻击 hook 派发：工具上所有词条链式调整护盾破盾效率 */
    private static float dispatchShieldEfficiency(ToolStack tool, ElementType shieldElement, float efficiency) {
        if (tool == null) {
            return efficiency;
        }
        for (ModifierEntry entry : tool.getModifierList()) {
            efficiency = entry.getHook(TcdexHooks.ELEMENTAL_ATTACK).modifyShieldEfficiency(tool, entry, shieldElement, efficiency);
        }
        return efficiency;
    }

    /** 破盾演出：AOE 爆炸（吃护盾元素抗性）+ 目标获得护盾元素状态（50 层）。只影响非玩家实体（命运2：破盾爆炸不伤玩家） */
    private static void shieldBreak(LivingEntity target, ElementType shieldElement) {
        Level level = target.level();
        float resistance = ElementManager.getResistance(target, shieldElement);
        float damage = target.getMaxHealth() * BREAK_HEALTH_PERCENT * resistance;
        DamageSource source = ModDamageSources.element(target, shieldElement);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(BREAK_RADIUS), e -> e != target && e.isAlive() && !(e instanceof Player))) {
            entity.hurt(source, damage);
        }
        // 目标获得护盾元素状态（半层，可配合关键词联动）
        IElementalEntity.of(target).addElementState(shieldElement, 50, 100);

        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.8F, 1.4F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 0.5, target.getZ(), 8, 0.4, 0.4, 0.4, 0.02);
        }
    }

    /** 攻击伤害源：有元素词条 → 元素伤害；否则 → 动能伤害 */
    private static DamageSource attackSource(Player player, ElementType element) {
        return element != null ? ModDamageSources.element(player, element) : ModDamageSources.kinetic(player);
    }

    /** 将物品转为匠魂 ToolStack（非匠魂/空物品返回 null） */
    private static ToolStack asTool(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IModifiable)) {
            return null;
        }
        ToolStack tool = ToolStack.from(stack);
        return tool.isBroken() ? null : tool;
    }

    /** 查找工具上的元素充能词条（第一个生效） */
    private static ElementalModifier findElemental(ToolStack tool) {
        if (tool == null) {
            return null;
        }
        for (ModifierEntry entry : tool.getModifierList()) {
            if (entry.getModifier() instanceof ElementalModifier elemental) {
                return elemental;
            }
        }
        return null;
    }
}
