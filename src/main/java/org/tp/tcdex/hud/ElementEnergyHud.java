package org.tp.tcdex.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tp.tcdex.Config;
import org.tp.tcdex.Tcdex;
import org.tp.tcdex.element.ElementType;

/**
 * 元素能量 HUD：在屏幕下方显示元素能量条与当前爆发元素。
 *
 * <p>数据由 {@link org.tp.tcdex.network.PlayerStateSyncPacket} 从服务端同步。</p>
 */
@Mod.EventBusSubscriber(modid = Tcdex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ElementEnergyHud {

    private static float energy;
    private static float maxEnergy;
    private static float rechargeEfficiency;
    private static byte burstElement;

    private ElementEnergyHud() {
    }

    public static void sync(float energy, float maxEnergy, float rechargeEfficiency, byte burstElement) {
        ElementEnergyHud.energy = energy;
        ElementEnergyHud.maxEnergy = Math.max(1.0f, maxEnergy);
        ElementEnergyHud.rechargeEfficiency = rechargeEfficiency;
        ElementEnergyHud.burstElement = burstElement;
    }

    @SubscribeEvent
    public static void onRenderHud(RenderGuiEvent.Post event) {
        if (!Config.elementEnergyHud) {
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
        int y = window.getGuiScaledHeight() - 39 - 8 - 5; // 护盾条上方再往上一点

        float ratio = Math.max(0.0f, Math.min(1.0f, energy / maxEnergy));
        int barWidth = 90;
        int barHeight = 2;

        // 背景
        graphics.fill(x, y, x + barWidth, y + barHeight, 0x66000000);
        if (ratio <= 0) {
            return;
        }

        // 能量条颜色：充满时金色，否则青色
        int color = ratio >= 1.0f ? 0xFFE8C14C : 0xFF33D6FF;
        graphics.fill(x, y, x + (int) (barWidth * ratio), y + barHeight, color);

        // 当前爆发元素缩写（可选的简单文字由 TcdexBuffHud 展示，这里只画条）
        if (burstElement > 0 && burstElement <= ElementType.values().length) {
            ElementType element = ElementType.values()[burstElement - 1];
            String name = element.getId().substring(0, 1).toUpperCase();
            graphics.drawString(mc.font, name, x + barWidth + 3, y - 1, element.getColor());
        }
    }
}
