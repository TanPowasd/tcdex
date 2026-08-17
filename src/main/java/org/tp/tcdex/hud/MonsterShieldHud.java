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
 * 怪物护盾 HUD（命运2 风格）：
 * <ul>
 *   <li><b>名字标签</b>：带护盾的怪物名字后追加彩色 "[元素盾]" 标记</li>
 *   <li><b>准星目标条</b>：瞄准带护盾目标时，准星下方显示元素色护盾条 + 元素名（按剩余值比例填充）</li>
 * </ul>
 * 数据由 {@link org.tp.tcdex.network.MonsterShieldSyncPacket} 从服务端广播缓存
 * （护盾生成/扣减/破盾时发送）。配置 {@code monsterShieldHud} 可关闭。
 */
@Mod.EventBusSubscriber(modid = Tcdex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class MonsterShieldHud {

    /** 实体 id → 护盾信息（网络包缓存） */
    private static final Map<Integer, ShieldInfo> SHIELD_CACHE = new HashMap<>();

    /** 护盾元素 ordinal + 1（0 = 无盾）与剩余值 */
    private record ShieldInfo(byte elementId, float amount) {
    }

    private MonsterShieldHud() {
    }

    /** 网络包同步入口 */
    public static void onShieldSync(int entityId, byte elementId, float amount) {
        if (elementId == 0 || amount <= 0) {
            SHIELD_CACHE.remove(entityId);
        } else {
            SHIELD_CACHE.put(entityId, new ShieldInfo(elementId, amount));
        }
    }

    /** 名字标签：彩色 [元素盾] 标记 */
    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (!Config.monsterShieldHud) {
            return;
        }
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity living) || living instanceof Player) {
            return;
        }
        ShieldInfo info = SHIELD_CACHE.get(living.getId());
        if (info == null) {
            return;
        }
        ElementType element = elementById(info.elementId());
        if (element == null) {
            return;
        }
        Component suffix = Component.literal(" [")
                .append(Component.translatable("modifier.tcdex.elemental.element." + element.getId())
                        .withStyle(style -> style.withColor(TextColor.fromRgb(element.getColor()))))
                .append(Component.literal("盾]"));
        event.setContent(event.getContent().copy().append(suffix));
    }

    /** 准星目标：屏幕中央下方显示元素色护盾条 */
    @SubscribeEvent
    public static void onRenderHud(RenderGuiEvent.Post event) {
        if (!Config.monsterShieldHud) {
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
        ShieldInfo info = SHIELD_CACHE.get(living.getId());
        if (info == null) {
            return;
        }
        ElementType element = elementById(info.elementId());
        if (element == null || info.amount() <= 0) {
            return;
        }
        float max = living.getMaxHealth() * 0.5f; // 护盾上限 = 最大生命 × 50%（与服务端一致）
        float ratio = Math.max(0.0f, Math.min(1.0f, info.amount() / Math.max(1.0f, max)));

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = mc.font;
        var window = event.getWindow();
        int barWidth = 90;
        int barHeight = 4;
        int x = window.getGuiScaledWidth() / 2 - barWidth / 2;
        int y = window.getGuiScaledHeight() / 2 + 16;

        // 元素名（护盾条上方，元素色）
        Component name = Component.translatable("modifier.tcdex.elemental.element." + element.getId())
                .withStyle(style -> style.withColor(TextColor.fromRgb(element.getColor())));
        graphics.drawString(font, name, x + (barWidth - font.width(name)) / 2, y - 10, element.getColor());

        // 护盾条：背景 + 元素色填充
        graphics.fill(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, 0xAA000000);
        graphics.fill(x, y, x + (int) (barWidth * ratio), y + barHeight, element.getColor());

        // 惰性清理：移除已不存在的实体缓存（每帧遍历，缓存通常很小）
        if (living.level().getEntity(living.getId()) == null) {
            SHIELD_CACHE.remove(living.getId());
        }
    }

    /** 按缓存编码解析元素 */
    private static ElementType elementById(byte id) {
        if (id <= 0 || id > ElementType.values().length) {
            return null;
        }
        return ElementType.values()[id - 1];
    }
}
