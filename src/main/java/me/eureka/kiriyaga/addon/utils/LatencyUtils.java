/*
 * Decompiled with CFR 0.150.
 *
 * Could not load the following classes:
 *  net.minecraft.network.packet.c2s.play.KeepAliveC2SPacket
 *  net.minecraft.client.network.PlayerListEntry
 */
package me.eureka.kiriyaga.addon.utils;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import net.minecraft.network.packet.c2s.play.KeepAliveC2SPacket;
import net.minecraft.client.network.PlayerListEntry;

public class LatencyUtils
extends Manager {
    public static final LatencyUtils get = new LatencyUtils();
    private long last_latency = -1L;
    private long real_latency = 0L;
    private boolean waiting_for_ping_update = false;

    public void onTick() {
        if (this.waiting_for_ping_update) {
            long cur_latency = this.GetLatency();
            if (cur_latency != this.last_latency) {
                this.real_latency = this.last_latency == -1L ? cur_latency : cur_latency * 4L - this.last_latency * 3L;
                this.last_latency = cur_latency;
            }
            this.waiting_for_ping_update = false;
        }
    }

    public void onPacketSent(PacketEvent.Sent event) {
        if (event.packet instanceof KeepAliveC2SPacket) {
            this.waiting_for_ping_update = true;
        }
    }

    public long GetRealLatency() {
        return this.real_latency;
    }

    private long GetLatency() {
        assert (LatencyUtils.mc.player != null);
        PlayerListEntry playerListEntry = LatencyUtils.mc.player.networkHandler.getPlayerListEntry(LatencyUtils.mc.player.getUuid());
        return playerListEntry != null ? (long)playerListEntry.getLatency() : 0L;
    }

    public void ClearInfo() {
        this.last_latency = -1L;
        this.real_latency = 0L;
        this.waiting_for_ping_update = true;
    }
}

