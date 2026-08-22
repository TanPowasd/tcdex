package org.tp.tcdex.integration.irons_spellbooks;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.tp.tcdex.chain.ElementActionType;
import org.tp.tcdex.chain.ElementCombatEvents;
import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.energy.ElementEnergyManager;
import org.tp.tcdex.modifier.elemental.IElementalEntity;
import org.tp.tcdex.reaction.ElementReactionEngine;
import org.tp.tcdex.shield.ElementalShieldHelper;

/**
 * 铁魔法法术元素化事件（由 IronSpellsIntegration 手动注册）。
 *
 * <p>深化内容：</p>
 * <ul>
 *   <li>覆盖更多法术类型：火/冰/雷/虚空/自然/风/重力/圣光等</li>
 *   <li>法术命中任意目标都会施加元素状态并尝试触发元素反应</li>
 *   <li>元素法术参与 TCDEX 元素护盾破盾结算</li>
 *   <li>玩家施法命中时获得元素能量</li>
 * </ul>
 */
public class IronSpellsEvents {

    private static final float SPELL_STACK_SCALE = 0.6f;

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onSpellHit(LivingHurtEvent event) {
        if (event.isCanceled() || event.getEntity().level().isClientSide) {
            return;
        }
        Entity direct = event.getSource().getDirectEntity();
        Entity sourceEntity = event.getSource().getEntity();
        ElementType element = elementFromEntity(direct);
        if (element == null) {
            element = elementFromEntity(sourceEntity);
        }
        if (element == null) {
            return;
        }

        LivingEntity target = event.getEntity();
        LivingEntity source = sourceEntity instanceof LivingEntity living ? living : null;

        // 玩家施法命中：获得元素能量并计入连携链
        if (source instanceof Player player) {
            ElementEnergyManager.onPlayerAttack(player, element);
            ElementCombatEvents.report(player, element, ElementActionType.SKILL, target);
        }

        // 触发元素反应 + 施加元素状态（即使被护盾吸收也会积累元素）
        ElementReactionEngine.tryTriggerReaction(target, element, source);
        float stacks = Math.max(1.0f, element.getStacksPerHit() * SPELL_STACK_SCALE);
        IElementalEntity.of(target).addElementState(element, stacks, element.getStateDuration());

        if (target instanceof Player player) {
            ElementEnergyManager.onPlayerDamagedByElement(player, element);
        }

        // 参与元素护盾结算：返回 0 表示伤害被护盾完全吸收
        float remaining = ElementalShieldHelper.damageShield(target, element, event.getAmount());
        if (remaining <= 0) {
            event.setCanceled(true);
        } else if (Math.abs(remaining - event.getAmount()) > 0.001f) {
            event.setAmount(remaining);
        }
    }

    /** 根据铁魔法实体注册名解析元素；非铁魔法实体返回 null */
    private static ElementType elementFromEntity(Entity entity) {
        if (entity == null) {
            return null;
        }
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (key == null || !"irons_spellbooks".equals(key.getNamespace())) {
            return null;
        }
        return matchSpellElement(key.getPath());
    }

    private static ElementType matchSpellElement(String path) {
        String p = path.toLowerCase();
        if (p.contains("fire") || p.contains("flame") || p.contains("burn") || p.contains("magma")
                || p.contains("blaze") || p.contains("holy") || p.contains("light")) {
            return ElementType.SOLAR;
        }
        if (p.contains("ice") || p.contains("frost") || p.contains("snow") || p.contains("cold")) {
            return ElementType.STASIS;
        }
        if (p.contains("lightning") || p.contains("electro") || p.contains("volt") || p.contains("thunder")) {
            return ElementType.ARC;
        }
        if (p.contains("void") || p.contains("evocation") || p.contains("shadow") || p.contains("wither")
                || p.contains("ender") || p.contains("blood") || p.contains("dark")) {
            return ElementType.VOID;
        }
        if (p.contains("nature") || p.contains("poison") || p.contains("vine") || p.contains("thorn")
                || p.contains("root") || p.contains("leaf")) {
            return ElementType.STRAND;
        }
        if (p.contains("wind") || p.contains("air") || p.contains("gust") || p.contains("storm")
                || p.contains("cyclone")) {
            return ElementType.MISTFLOW;
        }
        if (p.contains("gravity") || p.contains("star") || p.contains("black_hole") || p.contains("magnet")
                || p.contains("telekin")) {
            return ElementType.SINKSTAR;
        }
        return null;
    }
}
