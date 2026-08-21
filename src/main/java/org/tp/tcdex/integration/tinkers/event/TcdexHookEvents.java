package org.tp.tcdex.integration.tinkers.event;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.tp.tcdex.Tcdex;
import org.tp.tcdex.integration.tinkers.modifier.hook.KillingHook;
import org.tp.tcdex.integration.tinkers.modifier.hook.TcdexHooks;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.HashSet;

/**
 * 自定义 hook 的 Forge 事件派发点。
 *
 * <p>匠魂 3.10 没有原生击杀 hook，这里在 LivingDeathEvent（LOWEST 优先级）中
 * 遍历攻击者双手的匠魂工具，把击杀事件派发给工具上的 {@link TcdexHooks#KILLING_HOOK} 实现。</p>
 */
public class TcdexHookEvents {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void triggerKillingHook(LivingDeathEvent event) {
        Entity source = event.getSource().getEntity();
        if (!(source instanceof LivingEntity attacker)) {
            return;
        }

        // 双手工具去重（同一把工具切换手持有两个实例的情况）
        HashSet<ToolStack> tools = new HashSet<>();
        ToolStack mainHand = Modifier.getHeldTool(attacker, InteractionHand.MAIN_HAND);
        ToolStack offHand = Modifier.getHeldTool(attacker, InteractionHand.OFF_HAND);
        if (mainHand != null) {
            tools.add(mainHand);
        }
        if (offHand != null) {
            tools.add(offHand);
        }

        LivingEntity target = event.getEntity();
        for (ToolStack tool : tools) {
            for (ModifierEntry entry : tool.getModifierList()) {
                KillingHook hook = entry.getHook(TcdexHooks.KILLING_HOOK);
                hook.onKillLivingTarget(tool, event, attacker, target, entry.getLevel());
            }
        }
    }
}
