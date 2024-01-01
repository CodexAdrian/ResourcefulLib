package com.teamresourceful.resourcefullib.common.network.base;

import com.teamresourceful.resourcefullib.common.network.Packet;
import net.minecraft.server.level.ServerPlayer;

public interface Networking {


    <T extends Packet<T>> void register(ClientboundPacketType<T> type);

    <T extends Packet<T>> void register(ServerboundPacketType<T> type);

    <T extends Packet<T>> void sendToServer(T message);

    <T extends Packet<T>> void sendToPlayer(T message, ServerPlayer player);

    /**
     * Checks if the player can receive packets from this channel.
     *
     * @param player The player to check.
     * @return True if the player can receive packets from this channel, false otherwise.
     *
     * @implNote On forge this will only check if it has the channel not if it can receive that specific packet.
     */
    boolean canSendToPlayer(ServerPlayer player, PacketType<?> type);
}
