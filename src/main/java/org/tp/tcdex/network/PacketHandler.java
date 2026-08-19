package org.tp.tcdex.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.tp.tcdex.Tcdex;

/**
 * TCDEX 网络通道（SimpleChannel）。
 */
public final class PacketHandler {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(Tcdex.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private PacketHandler() {
    }

    /** 注册全部数据包（在 mod 构造早期调用） */
    public static void register() {
        CHANNEL.registerMessage(0, PlayerStateSyncPacket.class,
                PlayerStateSyncPacket::encode,
                PlayerStateSyncPacket::decode,
                PlayerStateSyncPacket::handle);
        CHANNEL.registerMessage(1, MonsterShieldSyncPacket.class,
                MonsterShieldSyncPacket::encode,
                MonsterShieldSyncPacket::decode,
                MonsterShieldSyncPacket::handle);
        CHANNEL.registerMessage(2, TranscendenceActivatePacket.class,
                TranscendenceActivatePacket::encode,
                TranscendenceActivatePacket::decode,
                TranscendenceActivatePacket::handle);
    }
}
