package org.tp.tcdex.shield;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tp.tcdex.Config;
import org.tp.tcdex.Tcdex;

/**
 * 玩家护盾 HUD（命运2 风格：蓝色护盾条 + 回复白色闪烁）。
 *
 * <p>绘制在血条上方：深色背景 + 蓝色填充（按护盾比例），
 * 回复中（客户端最近收到的值在增长）时白色闪烁。</p>
 */
@Mod.EventBusSubscriber(modid = Tcdex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class PlayerShieldHud {

    /** 客户端缓存的护盾值（由 PlayerShieldSyncPacket 同步） */
    private static float clientShield;
    private static float clientMaxShield;
    /** 是否回复中（最近一次同步值比上次大） */
    private static boolean regenerating;

    private PlayerShieldHud() {
    }

    /** 网络包同步入口（仅在收到更大的值时标记回复中） */
    public static void sync(float shield, float maxShield) {
        regenerating = shield > clientShield && shield < maxShield;
        clientShield = shield;
        clientMaxShield = maxShield;
    }

    @SubscribeEvent
    public static void onRenderHud(RenderGuiEvent.Post event) {
        if (!Config.playerShieldHud || clientMaxShield <= 0) {
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

        var window = event.getWindow();
        GuiGraphics graphics = event.getGuiGraphics();
        int x = window.getGuiScaledWidth() / 2 - 91;
        int y = window.getGuiScaledHeight() - 39 - 8; // 血条上方 8px

        float ratio = Math.max(0.0f, Math.min(1.0f, clientShield / clientMaxShield));
        int barWidth = 90;
        int barHeight = 3;

        // 背景
        graphics.fill(x, y, x + barWidth, y + barHeight, 0x66000000);
        if (ratio <= 0) {
            return;
        }
        // 蓝色护盾条（命运2 护盾蓝）
        graphics.fill(x, y, x + (int) (barWidth * ratio), y + barHeight, 0xFF33A7FF);
        // 回复闪烁（白色覆盖，4 tick 交替）
        if (regenerating && (mc.gui.getGuiTicks() / 4) % 2 == 0) {
            graphics.fill(x, y, x + (int) (barWidth * ratio), y + barHeight, 0xCCFFFFFF);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
