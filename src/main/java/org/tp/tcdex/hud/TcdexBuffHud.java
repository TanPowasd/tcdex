package org.tp.tcdex.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tp.tcdex.Config;
import org.tp.tcdex.Tcdex;
import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.modifier.elemental.ElementStatus;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 命运2 风格 Buff HUD：屏幕左上角垂直列出本 mod 的增益/减益状态。
 *
 * <p>每项 = 图标色块 + 名称 + 剩余时间；增益白色/绿色、减益红色、中性黄色。
 * 所有数据由 {@link org.tp.tcdex.network.PlayerStateSyncPacket} 从服务端同步
 * （persistentData / 工具 NBT 客户端不可实时读取）。</p>
 */
@Mod.EventBusSubscriber(modid = Tcdex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class TcdexBuffHud {

    // ===== 网络同步缓存（PlayerStateSyncPacket 更新） =====

    /** 急切刀锋剩余 tick */
    private static int eagerBuffTicks;
    /** 急切冷却剩余 tick */
    private static int eagerCooldownTicks;
    /** 万般皆允形态：0=无 1=虚 2=允 */
    private static byte apMode;
    private static float apForbidden;
    private static int apSinTicks;
    private static int apCombo;
    /** 吞噬 buff 剩余 tick */
    private static int devourTicks;
    /** 玩家元素状态 */
    private static final Map<ElementType, ElementStatus> ELEMENT_STATES = new EnumMap<>(ElementType.class);

    /** 整体缩放（图标 + 文字一起缩小） */
    private static final float SCALE = 0.7f;
    /** 图标色块尺寸 */
    private static final int ENTRY_ICON = 10;
    private static final int ENTRY_GAP = 2;
    private static final int ENTRY_HEIGHT = ENTRY_ICON + ENTRY_GAP;

    private TcdexBuffHud() {
    }

    /** 网络包同步入口 */
    public static void syncAll(int eagerBuff, int eagerCooldown,
                               byte mode, float forbidden, int sinTicks, int combo,
                               int devour,
                               Map<ElementType, ElementStatus> elementStates) {
        eagerBuffTicks = eagerBuff;
        eagerCooldownTicks = eagerCooldown;
        apMode = mode;
        apForbidden = forbidden;
        apSinTicks = sinTicks;
        apCombo = combo;
        devourTicks = devour;
        ELEMENT_STATES.clear();
        ELEMENT_STATES.putAll(elementStates);
    }

    /** 一条 HUD 状态 */
    private record BuffEntry(Component name, int color, String timeText) {
    }

    @SubscribeEvent
    public static void onRenderHud(RenderGuiEvent.Post event) {
        if (!Config.playerBuffHud) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        Player player = mc.player;
        if (player.getHealth() <= 0) {
            return;
        }

        List<BuffEntry> entries = collectEntries();
        if (entries.isEmpty()) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = mc.font;

        // 左边缘贴边，垂直居中（整体随条目数居中）
        int x = 2;
        int totalHeight = entries.size() * ENTRY_HEIGHT;
        int yStart = (event.getWindow().getGuiScaledHeight() - totalHeight) / 2;

        graphics.pose().pushPose();
        graphics.pose().translate(x, yStart, 0);
        graphics.pose().scale(SCALE, SCALE, 1.0f);
        int y = 0;
        int textOffset = (ENTRY_ICON - 8) / 2; // 文字垂直居中于图标

        for (BuffEntry entry : entries) {
            // 图标色块（深色描边 + 状态色填充）
            graphics.fill(0, y, ENTRY_ICON, y + ENTRY_ICON, 0xCC000000 | (entry.color() & 0xFFFFFF));
            graphics.fill(2, y + 2, ENTRY_ICON - 2, y + ENTRY_ICON - 2, entry.color());
            // 名称
            graphics.drawString(font, entry.name(), ENTRY_ICON + 3, y + textOffset, entry.color());
            // 剩余时间（名称右侧）
            if (!entry.timeText().isEmpty()) {
                graphics.drawString(font, entry.timeText(), ENTRY_ICON + 3 + font.width(entry.name()) + 5, y + textOffset, 0xFFFFFFFF);
            }
            y += ENTRY_HEIGHT;
        }
        graphics.pose().popPose();
    }

    /** 从网络缓存收集当前应显示的 buff 列表 */
    private static List<BuffEntry> collectEntries() {
        List<BuffEntry> entries = new ArrayList<>();

        // ===== 急切刀锋 =====
        if (eagerBuffTicks > 0) {
            entries.add(new BuffEntry(Component.translatable("modifier.tcdex.eager_edge"),
                    0xFFFFFFFF, secondsText(eagerBuffTicks)));
        }
        if (eagerCooldownTicks > 0) {
            entries.add(new BuffEntry(Component.translatable("hud.tcdex.eager_cooldown"),
                    0xFFFF5A5A, secondsText(eagerCooldownTicks)));
        }

        // ===== 万般皆允 =====
        if (apMode == 2) {
            entries.add(new BuffEntry(Component.translatable("hud.tcdex.all_permitted.yun"),
                    0xFFE8C14C, String.format(Locale.ROOT, "禁忌 %.0f/100", apForbidden)));
        } else if (apMode == 1) {
            entries.add(new BuffEntry(Component.translatable("hud.tcdex.all_permitted.xu"),
                    0xFFB14EFF, String.format(Locale.ROOT, "禁忌 %.0f/100", apForbidden)));
        }
        if (apSinTicks > 0) {
            entries.add(new BuffEntry(Component.translatable("hud.tcdex.all_permitted.sin"),
                    0xFFFF5A5A, secondsText(apSinTicks)));
        }
        if (apCombo > 0) {
            entries.add(new BuffEntry(Component.translatable("hud.tcdex.all_permitted.combo", apCombo),
                    0xFFFFFF40, ""));
        }

        // ===== 吞噬（Devour，虚空元素色） =====
        if (devourTicks > 0) {
            entries.add(new BuffEntry(Component.translatable("effect.tcdex.devour"),
                    0xFF9B59B6, secondsText(devourTicks)));
        }

        // ===== 玩家元素状态 =====
        for (Map.Entry<ElementType, ElementStatus> entry : ELEMENT_STATES.entrySet()) {
            ElementType element = entry.getKey();
            ElementStatus status = entry.getValue();
            if (status.duration <= 0) {
                continue;
            }
            Component name = Component.translatable("modifier.tcdex.elemental.element." + element.getId());
            entries.add(new BuffEntry(name, element.getColor(), secondsText(status.duration)));
        }

        return entries;
    }

    /** tick → "Xs"（向上取整秒） */
    private static String secondsText(long ticks) {
        return (ticks + 19) / 20 + "s";
    }
}
