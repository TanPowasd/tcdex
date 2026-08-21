package org.tp.tcdex.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.tp.tcdex.energy.ElementEnergyManager;

import java.util.function.Supplier;

/**
 * 元素爆发激活请求包（客户端 → 服务端，按键触发）。
 * 服务端校验能量满后释放当前武器对应的七元素爆发。
 */
public class ElementBurstActivatePacket {

    public ElementBurstActivatePacket() {
    }

    public static void encode(ElementBurstActivatePacket msg, FriendlyByteBuf buf) {
    }

    public static ElementBurstActivatePacket decode(FriendlyByteBuf buf) {
        return new ElementBurstActivatePacket();
    }

    public static void handle(ElementBurstActivatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ElementEnergyManager.tryActivateBurst(player, player.level().getGameTime());
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
