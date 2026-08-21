package org.tp.tcdex.transcendence;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tp.tcdex.Tcdex;
import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.energy.ElementEnergyManager;
import org.tp.tcdex.modifier.elemental.ElementalModifier;
import org.tp.tcdex.modifier.elemental.PrismResonanceModifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.List;

/**
 * 超越能量积累事件（玩家基础机制，无词条依赖）：
 * <ul>
 *   <li>玩家攻击命中（近战/远程）→ 按攻击元素积累（光系/暗系/棱镜/动能）</li>
 *   <li>玩家击杀 → 额外获得击杀奖励能量</li>
 * </ul>
 * 攻击元素判定与 {@link org.tp.tcdex.event.ElementalDamageEvents} 一致：
 * 棱镜共鸣优先，其次元素充能固化元素，否则动能。
 */
@Mod.EventBusSubscriber(modid = Tcdex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TranscendenceEvents {

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onPlayerAttackHit(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide || event.isCanceled()) {
            return;
        }
        Player player = eventPlayer(event.getSource().getDirectEntity());
        if (player == null) {
            return;
        }
        ElementType element = heldAttackElement(player);
        TranscendenceManager.gainEnergy(player, element,
                TranscendenceManager.HIT_GAIN, TranscendenceManager.HIT_GAIN_PRISM, TranscendenceManager.HIT_GAIN_OTHER);
        ElementEnergyManager.onPlayerAttack(player, element);
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onPlayerKill(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        Player player = eventPlayer(event.getSource().getDirectEntity());
        if (player == null) {
            return;
        }
        ElementType element = heldAttackElement(player);
        TranscendenceManager.gainEnergy(player, element,
                TranscendenceManager.KILL_GAIN, TranscendenceManager.KILL_GAIN_PRISM, TranscendenceManager.KILL_GAIN_OTHER);
        ElementEnergyManager.onPlayerKill(player, element);
    }

    /** 事件直接来源 → 玩家（近战本人 / 弹射物归属者） */
    private static Player eventPlayer(Entity direct) {
        if (direct instanceof Player p) {
            return p;
        }
        if (direct instanceof Projectile projectile && projectile.getOwner() instanceof Player p) {
            return p;
        }
        return null;
    }

    /** 玩家手持工具的当前攻击元素（主手优先；null = 动能/五项之力） */
    private static ElementType heldAttackElement(Player player) {
        for (ItemStack stack : List.of(player.getMainHandItem(), player.getOffhandItem())) {
            if (stack.isEmpty() || !(stack.getItem() instanceof IModifiable)) {
                continue;
            }
            ToolStack tool = ToolStack.from(stack);
            if (tool.isBroken()) {
                continue;
            }
            for (ModifierEntry entry : tool.getModifierList()) {
                if (entry.getModifier() instanceof PrismResonanceModifier) {
                    return ElementType.PRISM;
                }
            }
            ElementType element = ElementalModifier.parseElement(tool.getPersistentData().getString(ElementalModifier.ELEMENT_KEY));
            if (element != null) {
                return element;
            }
            return null; // 动能武器（或五项之力）：光暗混合少量
        }
        return null;
    }

    private TranscendenceEvents() {
    }
}
