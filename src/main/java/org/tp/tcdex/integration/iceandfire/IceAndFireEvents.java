package org.tp.tcdex.integration.iceandfire;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.energy.ElementEnergyManager;
import org.tp.tcdex.modifier.elemental.IElementalEntity;
import org.tp.tcdex.reaction.ElementReactionEngine;
import org.tp.tcdex.shield.ElementalShieldHelper;

/**
 * 冰与火之舞伤害联动事件（由 IceAndFireIntegration 手动注册）。
 *
 * <p>让龙息、龙爪、其他冰火生物攻击也能：
 * <ul>
 *   <li>参与 TCDEX 元素护盾破盾结算</li>
 *   <li>触发元素反应</li>
 *   <li>对任意目标（不只是玩家）施加元素状态</li>
 *   <li>玩家被命中时获得元素能量</li>
 * </ul></p>
 */
public class IceAndFireEvents {

    private static final float IAF_STACK_SCALE = 0.6f;

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.isCanceled() || event.getEntity().level().isClientSide) {
            return;
        }

        Entity direct = event.getSource().getDirectEntity();
        Entity source = event.getSource().getEntity();
        ElementType element = elementFromEntity(direct);
        if (element == null) {
            element = elementFromEntity(source);
        }
        if (element == null) {
            return;
        }

        LivingEntity target = event.getEntity();
        LivingEntity attacker = source instanceof LivingEntity living ? living : null;

        // 玩家被冰火生物攻击时，ElementalStateEvents.onMonsterAttackPlayer 已经处理元素附着/能量，
        // 这里避免重复叠加；非玩家目标才由本事件补全元素联动。
        if (target instanceof Player && attacker != null && !(attacker instanceof Player)) {
            return;
        }

        // 先触发元素反应与状态附着（即使被护盾吸收也会积累元素）
        ElementReactionEngine.tryTriggerReaction(target, element, attacker);
        float stacks = Math.max(1.0f, element.getStacksPerHit() * IAF_STACK_SCALE);
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

    /** 根据冰火实体注册名解析元素；非冰火实体返回 null */
    private static ElementType elementFromEntity(Entity entity) {
        if (entity == null) {
            return null;
        }
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (key == null || !"iceandfire".equals(key.getNamespace())) {
            return null;
        }
        return matchElement(key.getPath());
    }

    /** 冰与火之舞实体名 → TCDEX 元素 */
    private static ElementType matchElement(String path) {
        String p = path.toLowerCase();
        if (p.contains("fire_dragon") || p.contains("ifrit") || p.contains("salamander") || p.contains("fire")) {
            return ElementType.SOLAR;
        }
        if (p.contains("ice_dragon") || p.contains("frost") || p.contains("snow") || p.contains("ice")) {
            return ElementType.STASIS;
        }
        if (p.contains("lightning_dragon") || p.contains("lightning") || p.contains("electric")) {
            return ElementType.ARC;
        }
        if (p.contains("sea_serpent") || p.contains("amphithere") || p.contains("siren")) {
            return ElementType.TIDE;
        }
        if (p.contains("hippogryph") || p.contains("pixie") || p.contains("stymphalian")) {
            return ElementType.MISTFLOW;
        }
        if (p.contains("myrmex") || p.contains("hydra")) {
            return ElementType.STRAND;
        }
        if (p.contains("deathworm") || p.contains("ghost") || p.contains("lich") || p.contains("troll")
                || p.contains("cyclops") || p.contains("cockatrice") || p.contains("gorgon")) {
            return ElementType.VOID;
        }
        return null;
    }
}
