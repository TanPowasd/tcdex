package org.tp.tcdex.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.tp.tcdex.chain.ChainEntry;
import org.tp.tcdex.hud.ChainHud;
import org.tp.tcdex.element.ElementType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 连携状态同步包（服务端 → 客户端，HUD 显示用）。
 */
public class ChainStateSyncPacket {

    private final List<ChainEntry> mainChain;
    private final List<ChainEntry> focusChain;
    private final float groupOverflow;
    private final int detonateCooldown;
    private final int chainBuffTicks;

    public ChainStateSyncPacket(List<ChainEntry> mainChain, List<ChainEntry> focusChain,
                                float groupOverflow, int detonateCooldown, int chainBuffTicks) {
        this.mainChain = mainChain;
        this.focusChain = focusChain;
        this.groupOverflow = groupOverflow;
        this.detonateCooldown = detonateCooldown;
        this.chainBuffTicks = chainBuffTicks;
    }

    public static void encode(ChainStateSyncPacket msg, FriendlyByteBuf buf) {
        writeChain(msg.mainChain, buf);
        writeChain(msg.focusChain, buf);
        buf.writeFloat(msg.groupOverflow);
        buf.writeInt(msg.detonateCooldown);
        buf.writeInt(msg.chainBuffTicks);
    }

    private static void writeChain(List<ChainEntry> chain, FriendlyByteBuf buf) {
        buf.writeByte(chain.size());
        for (ChainEntry entry : chain) {
            buf.writeByte(entry.element().ordinal());
            buf.writeLong(entry.lastUsedTime());
            buf.writeFloat(entry.contribution());
        }
    }

    private static List<ChainEntry> readChain(FriendlyByteBuf buf) {
        int count = buf.readByte();
        List<ChainEntry> list = new ArrayList<>();
        ElementType[] values = ElementType.values();
        for (int i = 0; i < count; i++) {
            int ordinal = buf.readByte();
            long time = buf.readLong();
            float contribution = buf.readFloat();
            if (ordinal >= 0 && ordinal < values.length) {
                list.add(new ChainEntry(values[ordinal], time, contribution));
            }
        }
        return list;
    }

    public static ChainStateSyncPacket decode(FriendlyByteBuf buf) {
        List<ChainEntry> main = readChain(buf);
        List<ChainEntry> focus = readChain(buf);
        float group = buf.readFloat();
        int cooldown = buf.readInt();
        int buff = buf.readInt();
        return new ChainStateSyncPacket(main, focus, group, cooldown, buff);
    }

    public static void handle(ChainStateSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ChainHud.sync(msg.mainChain, msg.focusChain,
                msg.groupOverflow, msg.detonateCooldown, msg.chainBuffTicks));
        ctx.get().setPacketHandled(true);
    }
}
