package org.tp.tcdex.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tp.tcdex.Config;
import org.tp.tcdex.Tcdex;
import org.tp.tcdex.element.ElementType;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 元素附着 HUD（命运2/原神风格）：
 * <ul>
 *   <li>名字标签：带元素附着的怪物名字后追加彩色 "[元素附着]" 标记</li>
 *   <li>准星目标：瞄准目标时在准星下方显示当前附着元素与附着量条</li>
 * </ul>
 * 数据由 {@link org.tp.tcdex.network.MonsterAuraSyncPacket} 从服务端同步。
 */
@Mod.EventBusSubscriber(modid = Tcdex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ElementAuraHud {

    /** 实体 id → (元素 → 附着量) */
    private static final Map<Integer, Map<ElementType, Float>> AURA_CACHE = new HashMap<>();

    private ElementAuraHud() {
    }

    /** 网络包同步入口 */
    public static void onAuraSync(int entityId, byte elementId, float aura) {
        if (elementId <= 0 || elementId > ElementType.values().length) {
            AURA_CACHE.remove(entityId);
            return;
        }
        ElementType element = ElementType.values()[elementId - 1];
        Map<ElementType, Float> auras = AURA_CACHE.computeIfAbsent(entityId, k -> new HashMap<>());
        if (aura <= 0) {
            auras.remove(element);
            if (auras.isEmpty()) {
                AURA_CACHE.remove(entityId);
            }
        } else {
            auras.put(element, aura);
        }
    }

    /** 名字标签：彩色 [元素附着] 标记 */
    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (!Config.monsterAuraHud) {
            return;
        }
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity living) || living instanceof Player) {
            return;
        }
        Map<ElementType, Float> auras = AURA_CACHE.get(living.getId());
        if (auras == null || auras.isEmpty()) {
            return;
        }
        Component suffix = Component.literal(" [");
        boolean first = true;
        for (Map.Entry<ElementType, Float> entry : auras.entrySet()) {
            if (!first) {
                suffix = suffix.copy().append(Component.literal("+"));
            }
            first = false;
            suffix = suffix.copy().append(Component.translatable("modifier.tcdex.elemental.element." + entry.getKey().getId())
                    .withStyle(style -> style.withColor(TextColor.fromRgb(entry.getKey().getColor()))));
        }
        suffix = suffix.copy().append(Component.literal("附着]"));
        event.setContent(event.getContent().copy().append(suffix));
    }

    /** 准星目标：显示附着元素与附着量条 */
    @SubscribeEvent
    public static void onRenderHud(RenderGuiEvent.Post event) {
        if (!Config.monsterAuraHud) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.hitResult == null) {
            return;
        }
        HitResult hit = mc.hitResult;
        if (!(hit instanceof EntityHitResult entityHit) || !(entityHit.getEntity() instanceof LivingEntity living)
                || living instanceof Player || !living.isAlive()) {
            return;
        }
        Map<ElementType, Float> auras = AURA_CACHE.get(living.getId());
        if (auras == null || auras.isEmpty()) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = mc.font;
        var window = event.getWindow();
        int x = window.getGuiScaledWidth() / 2;
        int y = window.getGuiScaledHeight() / 2 + 22;
        int index = 0;

        for (Map.Entry<ElementType, Float> entry : auras.entrySet()) {
            ElementType element = entry.getKey();
            float aura = Math.max(0.0f, entry.getValue());
            String text = Component.translatable("modifier.tcdex.elemental.element." + element.getId()).getString()
                    + " " + String.format("%.1f", aura);
            graphics.drawString(font, text, x - font.width(text) / 2, y + index * 10, element.getColor());
            index++;
        }
    }

    /** 惰性清理：移除已不存在的实体缓存（每帧遍历，缓存通常很小） */
    @SubscribeEvent
    public static void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            AURA_CACHE.clear();
            return;
        }
        Iterator<Map.Entry<Integer, Map<ElementType, Float>>> it = AURA_CACHE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Map<ElementType, Float>> entry = it.next();
            if (mc.level.getEntity(entry.getKey()) == null) {
                it.remove();
            }
        }
    }
}
