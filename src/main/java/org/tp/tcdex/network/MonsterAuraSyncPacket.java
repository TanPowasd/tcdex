package org.tp.tcdex.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.tp.tcdex.hud.ElementAuraHud;

import java.util.function.Supplier;

/**
 * 怪物元素附着量同步包（服务端 → 追踪该实体的玩家）。
 *
 * <p>用于客户端 HUD 显示目标身上当前附着的元素与附着量。
 * 编码：elementId = ordinal + 1；aura <= 0 表示清除该元素附着。</p>
 */
public class MonsterAuraSyncPacket {

    private final int entityId;
    private final byte elementId;
    private final float aura;

    public MonsterAuraSyncPacket(int entityId, byte elementId, float aura) {
        this.entityId = entityId;
        this.elementId = elementId;
        this.aura = aura;
    }

    public static void encode(MonsterAuraSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeByte(msg.elementId);
        buf.writeFloat(msg.aura);
    }

    public static MonsterAuraSyncPacket decode(FriendlyByteBuf buf) {
        return new MonsterAuraSyncPacket(buf.readInt(), buf.readByte(), buf.readFloat());
    }

    public static void handle(MonsterAuraSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ElementAuraHud.onAuraSync(msg.entityId, msg.elementId, msg.aura));
        ctx.get().setPacketHandled(true);
    }
}
