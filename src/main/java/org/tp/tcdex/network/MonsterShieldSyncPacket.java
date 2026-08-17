package org.tp.tcdex.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.tp.tcdex.hud.MonsterShieldHud;

import java.util.function.Supplier;

/**
 * 怪物护盾同步包（服务端 → 追踪该实体的玩家）。
 *
 * <p>护盾元素/护盾值变化时广播（生成分配、扣减、破盾），客户端缓存供 HUD 显示。
 * 编码：护盾元素 = ordinal + 1，0 = 无护盾。</p>
 */
public class MonsterShieldSyncPacket {

    private final int entityId;
    private final byte elementId;
    private final float amount;

    public MonsterShieldSyncPacket(int entityId, byte elementId, float amount) {
        this.entityId = entityId;
        this.elementId = elementId;
        this.amount = amount;
    }

    public static void encode(MonsterShieldSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeByte(msg.elementId);
        buf.writeFloat(msg.amount);
    }

    public static MonsterShieldSyncPacket decode(FriendlyByteBuf buf) {
        return new MonsterShieldSyncPacket(buf.readInt(), buf.readByte(), buf.readFloat());
    }

    public static void handle(MonsterShieldSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> MonsterShieldHud.onShieldSync(msg.entityId, msg.elementId, msg.amount));
        ctx.get().setPacketHandled(true);
    }
}
