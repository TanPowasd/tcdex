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
import org.tp.tcdex.artifact.ArtifactManager;
import org.tp.tcdex.damage.ModDamageSources;
import org.tp.tcdex.debug.TcdexDebug;
import org.tp.tcdex.element.ElementManager;
import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.modifier.elemental.ElementalModifier;
import org.tp.tcdex.modifier.elemental.FiveForcesModifier;
import org.tp.tcdex.modifier.elemental.IElementalEntity;
import org.tp.tcdex.modifier.elemental.PrismResonanceModifier;
import org.tp.tcdex.modifier.hook.TcdexHooks;
import org.tp.tcdex.shield.PrismShieldConfig;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

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
 *   <li>棱镜盾（Boss 专属，吸收型）：见 {@link #handlePrismShield}——伤害被盾完全吸收，
 *       破盾前打不到血量；磨损效率 棱镜 ×2 / 其他元素 ×0.5 / 动能 ×0.1</li>
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

        // 已是 TCDEX 类型伤害（元素/动能/纯粹/灼烧 DoT）→ 递归保护
        // （含 scorch：玩家自身灼烧 DoT 不能被当成"玩家攻击"重新转化/护盾结算）
        if (ModDamageSources.isTcdexDamage(event.getSource())) {
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

        // 超越激活：玩家全部攻击伤害 ×1.3（含护盾磨损/破盾溢出，近战远程统一；玩家基础机制，见 TranscendenceManager）
        event.setAmount(org.tp.tcdex.transcendence.TranscendenceManager.applyDamageMultiplier(player, event.getAmount()));

        // 玩家手持匠魂工具上的棱镜共鸣/元素充能/五项之力词条（主手优先；取第一个生效；null = 动能武器）
        // 棱镜共鸣固定棱镜伤害（专属来源，与元素充能互斥；若经命令强加两者，棱镜共鸣优先）
        // 五项之力每次攻击随机元素（与元素充能互斥，权重遵循配置；命中施加对应元素状态）
        ToolStack tool = null;
        ElementType element = null;
        boolean fiveForces = false;
        for (ItemStack stack : List.of(player.getMainHandItem(), player.getOffhandItem())) {
            ToolStack candidate = asTool(stack);
            if (candidate == null) {
                continue;
            }
            if (findModifier(candidate, PrismResonanceModifier.class) != null) {
                tool = candidate;
                element = ElementType.PRISM;
                break;
            }
            ElementalModifier elemental = findModifier(candidate, ElementalModifier.class);
            if (elemental != null) {
                tool = candidate;
                element = elemental.getElement(candidate);
                candidate.updateStack(stack); // 固化写回
                break;
            }
            if (findModifier(candidate, FiveForcesModifier.class) != null) {
                tool = candidate;
                element = ElementManager.rollElement(player.level().random); // 每次攻击随机
                fiveForces = true;
                break;
            }
            if (tool == null) {
                tool = candidate;
            }
        }

        // ===== 元素护盾结算（优先于伤害类型转化） =====
        IElementalEntity targetData = IElementalEntity.of(target);
        if (targetData.getShieldAmount() > 0 && targetData.getShieldElement() != null) {
            // 棱镜盾（Boss 专属，吸收型）：伤害被盾完全吸收，破盾前打不到血量；
            // 磨损效率 棱镜 ×2 / 其他元素 ×0.5 / 动能 ×0.1
            if (targetData.getShieldElement() == ElementType.PRISM) {
                handlePrismShield(event, target, targetData, player, tool, element);
                return;
            }
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
            // 圣遗物元素伤害加成
            amount *= 1.0f + ArtifactManager.getTotalElementDamageBonus(player);
            // 五项之力：命中施加本次 roll 到的元素状态（与伤害元素同源一致，走 ELEMENTAL_STATE_APPLY hook）
            if (fiveForces) {
                FiveForcesModifier.applyHitState(tool, player, target, element);
            }
        } else {
            // 动能攻击 hook 联动：动能武器词条可调整动能伤害（目标参数开放：看目标是否带盾/带标记）
            amount = dispatchKineticDamage(tool, target, amount);
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
        // 棱镜折射所有光 = 匹配所有盾：棱镜攻击对任意元素护盾都按匹配效率（×2）
        float efficiency = (attackElement == shieldElement || attackElement == ElementType.PRISM)
                ? MATCH_EFFICIENCY : MISMATCH_EFFICIENCY;
        // 破盾效率 hook 联动：元素攻击走 ELEMENTAL_ATTACK，动能攻击走 KINETIC_ATTACK
        efficiency = attackElement == null
                ? dispatchKineticShieldEfficiency(tool, shieldElement, efficiency)
                : dispatchShieldEfficiency(tool, shieldElement, efficiency);

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
            // 破盾：爆炸 + 目标获得护盾元素状态（破盾 hook 可调整爆炸伤害/触发联动）
            float breakDamage = target.getMaxHealth() * BREAK_HEALTH_PERCENT;
            breakDamage = dispatchShieldBreak(tool, target, shieldElement, breakDamage, player);
            shieldBreak(target, shieldElement, breakDamage);
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

    /**
     * 棱镜盾结算（凋零/末影龙 Boss 专属，吸收型）：
     * 伤害被盾**完全吸收**（磨损护盾），破盾前打不到血量。
     * 磨损效率：棱镜 ×2（匹配）/ 其他元素 ×0.5（"50% 减免"）/ 动能 ×0.1（"90% 减免"）。
     * 打穿结算：
     * - 棱镜伤害打穿 → 护盾永久失效（清除元素，不再回复）
     * - 非棱镜伤害打穿 → 护盾元素保留，脱战回复可重新长满
     * 破盾那击的溢出伤害按效率折算回真实值伤血（破盾后伤害才能打到血量）；
     * 元素攻击保留（分配时固化，与护盾状态无关）。
     */
    private static void handlePrismShield(LivingHurtEvent event, LivingEntity target, IElementalEntity targetData,
                                          Player player, ToolStack tool, ElementType attackElement) {
        // 磨损效率（配置化）：棱镜匹配；动能；其他元素
        float efficiency;
        if (attackElement == ElementType.PRISM) {
            efficiency = PrismShieldConfig.getMatchEfficiency();
        } else if (attackElement == null) {
            efficiency = PrismShieldConfig.getKineticEfficiency();
        } else {
            efficiency = PrismShieldConfig.getElementEfficiency();
        }
        // 破盾效率 hook 联动：元素攻击走 ELEMENTAL_ATTACK，动能攻击走 KINETIC_ATTACK
        efficiency = attackElement == null
                ? dispatchKineticShieldEfficiency(tool, ElementType.PRISM, efficiency)
                : dispatchShieldEfficiency(tool, ElementType.PRISM, efficiency);

        float amount = event.getAmount();
        event.setCanceled(true);
        target.invulnerableTime = 0;

        // 受击重置脱战计时（棱镜盾回复需要脱战 10 秒）
        targetData.markShieldHit(target.level().getGameTime());

        // 伤害打入护盾（按效率磨损），返回溢出。
        // permanent = 棱镜伤害打穿 → 清除护盾元素，永久失效（不再回复）；
        // 非棱镜伤害打穿 → 保留护盾元素，脱战回复可重新长满。
        boolean prismAttack = attackElement == ElementType.PRISM;
        float overflow = targetData.consumeShield(amount * efficiency, prismAttack);
        if (overflow > 0) {
            // 破盾：爆炸 + 目标获得棱镜状态（破盾 hook 可调整爆炸伤害/触发联动）；元素攻击保留（分配时固化）
            float breakDamage = target.getMaxHealth() * BREAK_HEALTH_PERCENT;
            breakDamage = dispatchShieldBreak(tool, target, ElementType.PRISM, breakDamage, player);
            shieldBreak(target, ElementType.PRISM, breakDamage);
            // 破盾那击：溢出伤害按效率折算回真实值，以攻击类型结算（破盾后伤害才能打到血量）
            target.hurt(attackSource(player, attackElement), overflow / efficiency);
            if (TcdexDebug.isElementalEnabled()) {
                player.sendSystemMessage(Component.literal(String.format(Locale.ROOT,
                        "[元素调试] 棱镜盾被打穿! 本击 %.2f (效率 %.2f) | 溢出伤血 %.2f | %s",
                        amount, efficiency, overflow / efficiency,
                        prismAttack ? "永久失效(不再回复)" : "可脱战回复")));
            }
        } else {
            // 未打穿：盾全吃（破盾前打不到血量），播放受击反馈
            Level level = target.level();
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 0.8F, 1.2F);
            if (TcdexDebug.isElementalEnabled()) {
                player.sendSystemMessage(Component.literal(String.format(Locale.ROOT,
                        "[元素调试] 棱镜盾: 磨损 %.2f (效率 %.2f) | 剩余 %.1f",
                        amount * efficiency, efficiency, targetData.getShieldAmount())));
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

    /** 动能攻击 hook 派发：工具上所有词条链式调整动能伤害 */
    private static float dispatchKineticDamage(ToolStack tool, LivingEntity target, float amount) {
        if (tool == null) {
            return amount;
        }
        for (ModifierEntry entry : tool.getModifierList()) {
            amount = entry.getHook(TcdexHooks.KINETIC_ATTACK).modifyKineticDamage(tool, entry, target, amount);
        }
        return amount;
    }

    /** 动能攻击 hook 派发：工具上所有词条链式调整动能破盾效率 */
    private static float dispatchKineticShieldEfficiency(ToolStack tool, ElementType shieldElement, float efficiency) {
        if (tool == null) {
            return efficiency;
        }
        for (ModifierEntry entry : tool.getModifierList()) {
            efficiency = entry.getHook(TcdexHooks.KINETIC_ATTACK).modifyKineticShieldEfficiency(tool, entry, shieldElement, efficiency);
        }
        return efficiency;
    }

    /** 破盾 hook 派发：工具上词条链式调整破盾爆炸伤害 + 触发破盾回调 */
    private static float dispatchShieldBreak(ToolStack tool, LivingEntity target, ElementType shieldElement,
                                             float damage, @Nullable Player attacker) {
        if (tool == null) {
            return damage;
        }
        for (ModifierEntry entry : tool.getModifierList()) {
            damage = entry.getHook(TcdexHooks.SHIELD_BREAK)
                    .modifyBreakExplosion(tool, entry, target, shieldElement, damage);
        }
        for (ModifierEntry entry : tool.getModifierList()) {
            entry.getHook(TcdexHooks.SHIELD_BREAK)
                    .onShieldBreak(tool, entry, target, shieldElement, attacker);
        }
        return damage;
    }

    /** 破盾演出：AOE 爆炸（吃护盾元素抗性）+ 目标获得护盾元素状态（50 层）。只影响非玩家实体（命运2：破盾爆炸不伤玩家） */
    private static void shieldBreak(LivingEntity target, ElementType shieldElement, float baseDamage) {
        Level level = target.level();
        float damage = baseDamage * ElementManager.getResistance(target, shieldElement);
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

    /** 查找工具上的指定词条（第一个生效） */
    private static <T extends slimeknights.tconstruct.library.modifiers.Modifier> T findModifier(ToolStack tool, Class<T> type) {
        if (tool == null) {
            return null;
        }
        for (ModifierEntry entry : tool.getModifierList()) {
            if (type.isInstance(entry.getModifier())) {
                return type.cast(entry.getModifier());
            }
        }
        return null;
    }
}
