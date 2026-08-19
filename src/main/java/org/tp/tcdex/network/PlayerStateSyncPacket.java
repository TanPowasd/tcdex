package org.tp.tcdex.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.tp.tcdex.element.ElementType;
import org.tp.tcdex.hud.TcdexBuffHud;
import org.tp.tcdex.modifier.elemental.ElementStatus;
import org.tp.tcdex.shield.PlayerShieldHud;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 玩家状态同步包（服务端 → 客户端，HUD 显示用）：
 * 护盾值、急切刀锋 buff/冷却、万般皆允形态状态、超越光暗能量槽、玩家元素状态。
 * 服务端权威收集（玩家/工具 NBT），客户端仅渲染。
 */
public class PlayerStateSyncPacket {

    private final float shield;
    private final float maxShield;
    private final int eagerBuffTicks;
    private final int eagerCooldownTicks;
    private final byte apMode;        // 0=无 1=虚 2=允
    private final float apForbidden;
    private final int apSinTicks;
    private final int apCombo;
    private final int devourTicks;
    private final float transLight;   // 超越：光之能量（0-100）
    private final float transDark;    // 超越：暗之能量（0-100）
    private final int transActiveTicks; // 超越：激活剩余 tick
    private final Map<ElementType, ElementStatus> elementStates;

    public PlayerStateSyncPacket(float shield, float maxShield,
                                 int eagerBuffTicks, int eagerCooldownTicks,
                                 byte apMode, float apForbidden, int apSinTicks, int apCombo,
                                 int devourTicks,
                                 float transLight, float transDark, int transActiveTicks,
                                 Map<ElementType, ElementStatus> elementStates) {
        this.shield = shield;
        this.maxShield = maxShield;
        this.eagerBuffTicks = eagerBuffTicks;
        this.eagerCooldownTicks = eagerCooldownTicks;
        this.apMode = apMode;
        this.apForbidden = apForbidden;
        this.apSinTicks = apSinTicks;
        this.apCombo = apCombo;
        this.devourTicks = devourTicks;
        this.transLight = transLight;
        this.transDark = transDark;
        this.transActiveTicks = transActiveTicks;
        this.elementStates = elementStates;
    }

    public static void encode(PlayerStateSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.shield);
        buf.writeFloat(msg.maxShield);
        buf.writeInt(msg.eagerBuffTicks);
        buf.writeInt(msg.eagerCooldownTicks);
        buf.writeByte(msg.apMode);
        buf.writeFloat(msg.apForbidden);
        buf.writeInt(msg.apSinTicks);
        buf.writeInt(msg.apCombo);
        buf.writeInt(msg.devourTicks);
        buf.writeFloat(msg.transLight);
        buf.writeFloat(msg.transDark);
        buf.writeInt(msg.transActiveTicks);
        buf.writeByte(msg.elementStates.size());
        for (Map.Entry<ElementType, ElementStatus> entry : msg.elementStates.entrySet()) {
            buf.writeByte(entry.getKey().ordinal());
            buf.writeFloat(entry.getValue().stacks);
            buf.writeInt(entry.getValue().duration);
        }
    }

    public static PlayerStateSyncPacket decode(FriendlyByteBuf buf) {
        float shield = buf.readFloat();
        float maxShield = buf.readFloat();
        int eagerBuffTicks = buf.readInt();
        int eagerCooldownTicks = buf.readInt();
        byte apMode = buf.readByte();
        float apForbidden = buf.readFloat();
        int apSinTicks = buf.readInt();
        int apCombo = buf.readInt();
        int devourTicks = buf.readInt();
        float transLight = buf.readFloat();
        float transDark = buf.readFloat();
        int transActiveTicks = buf.readInt();
        Map<ElementType, ElementStatus> states = new EnumMap<>(ElementType.class);
        int count = buf.readByte();
        ElementType[] values = ElementType.values();
        for (int i = 0; i < count; i++) {
            int ordinal = buf.readByte();
            if (ordinal >= 0 && ordinal < values.length) {
                states.put(values[ordinal], new ElementStatus(buf.readFloat(), buf.readInt()));
            }
        }
        return new PlayerStateSyncPacket(shield, maxShield, eagerBuffTicks, eagerCooldownTicks,
                apMode, apForbidden, apSinTicks, apCombo, devourTicks,
                transLight, transDark, transActiveTicks, states);
    }

    public static void handle(PlayerStateSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            PlayerShieldHud.sync(msg.shield, msg.maxShield);
            TcdexBuffHud.syncAll(msg.eagerBuffTicks, msg.eagerCooldownTicks,
                    msg.apMode, msg.apForbidden, msg.apSinTicks, msg.apCombo,
                    msg.devourTicks,
                    msg.transLight, msg.transDark, msg.transActiveTicks,
                    msg.elementStates);
        });
        ctx.get().setPacketHandled(true);
    }
}
