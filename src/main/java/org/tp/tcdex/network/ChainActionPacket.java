package org.tp.tcdex.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.tp.tcdex.chain.ChainActionType;
import org.tp.tcdex.chain.ChainManager;

import java.util.function.Supplier;

/**
 * 连携按键动作请求包（客户端 → 服务端）。
 */
public class ChainActionPacket {

    private final ChainActionType action;

    public ChainActionPacket() {
        this(ChainActionType.SMART);
    }

    public ChainActionPacket(ChainActionType action) {
        this.action = action;
    }

    public static void encode(ChainActionPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.action);
    }

    public static ChainActionPacket decode(FriendlyByteBuf buf) {
        return new ChainActionPacket(buf.readEnum(ChainActionType.class));
    }

    public static void handle(ChainActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ChainManager.handleAction(player, msg.action);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
