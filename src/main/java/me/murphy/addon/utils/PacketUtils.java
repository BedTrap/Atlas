/*
 * Decompiled with CFR 0.150.
 *
 * Could not load the following classes:
 *  net.minecraft.network.Packet
 *  net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket
 *  net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket
 *  net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
 *  net.minecraft.network.packet.c2s.play.PickFromInventoryC2SPacket
 *  net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
 *  net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
 *  net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
 *  net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
 */
package me.murphy.addon.utils;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PickFromInventoryC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;

public class PacketUtils
extends Manager {
    public static final PacketUtils get = new PacketUtils();
    private boolean slot_changed = false;
    private int packet_count = 0;

    public void onPacketSent(PacketEvent.Sent event) {
        if (this.IsWorldPacket(event.packet) || this.IsUseItemPacket(event.packet)) {
            ++this.packet_count;
        } else if (this.IsUpdateSlotPacket(event.packet)) {
            this.slot_changed = true;
        }
    }

    public boolean onPacketSend(PacketEvent.Send event) {
        return false;
    }

    private boolean IsWorldPacket(Packet<?> packet) {
        return packet instanceof PlayerActionC2SPacket || packet instanceof PlayerInteractBlockC2SPacket || packet instanceof PlayerInteractEntityC2SPacket;
    }

    private boolean IsUseItemPacket(Packet<?> packet) {
        return packet instanceof PlayerInteractItemC2SPacket;
    }

    private boolean IsUpdateSlotPacket(Packet<?> packet) {
        return packet instanceof UpdateSelectedSlotC2SPacket;
    }

    private boolean IsInvPacket(Packet<?> packet) {
        return packet instanceof ClickSlotC2SPacket || packet instanceof ButtonClickC2SPacket || packet instanceof PickFromInventoryC2SPacket;
    }

    public boolean CanChangeHotbarSlot() {
        return !this.slot_changed;
    }
//
    public boolean CanInteractWorld() {
        return 1 - this.packet_count > 0;
    }

//    public int InteractWorldPacketsLeft() {
//        return L1teor.get.GetPacketLimit() - this.packet_count;
//    }

    public void Reset() {
        this.slot_changed = false;
        this.packet_count = 0;
    }
}

