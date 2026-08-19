package org.tp.tcdex.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.tp.tcdex.transcendence.TranscendenceManager;

import java.util.function.Supplier;

/**
 * 激活超越请求包（客户端 → 服务端，按键触发）。
 * 服务端校验双槽已满后激活 Transcendence 爆发。
 */
public class TranscendenceActivatePacket {

    public TranscendenceActivatePacket() {
    }

    public static void encode(TranscendenceActivatePacket msg, FriendlyByteBuf buf) {
    }

    public static TranscendenceActivatePacket decode(FriendlyByteBuf buf) {
        return new TranscendenceActivatePacket();
    }

    public static void handle(TranscendenceActivatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                TranscendenceManager.tryActivate(player, player.level().getGameTime());
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
