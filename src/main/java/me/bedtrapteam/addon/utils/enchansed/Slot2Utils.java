/*
 * Decompiled with CFR 0.150.
 *
 * Could not load the following classes:
 *  net.minecraft.entity.player.PlayerInventory
 *  net.minecraft.network.Packet
 *  net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket
 *  net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
 */
package me.bedtrapteam.addon.utils.enchansed;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import me.bedtrapteam.addon.utils.PacketUtils;
import me.bedtrapteam.addon.utils.TickUtils;
import me.bedtrapteam.addon.utils.Utils;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Slot2Utils {
    public static final Slot2Utils get = new Slot2Utils();
    private final AtomicInteger selected_slot = new AtomicInteger(0);
    private final AtomicLong update_time = new AtomicLong(-1L);
    private final AtomicInteger manual_selected_slot = new AtomicInteger(0);
    private final AtomicLong return_back_time = new AtomicLong(-1L);

    public void onTick() {
        assert (mc.player != null);
        if (!mc.player.isCreative() || mc.currentScreen != null || !mc.options.keyLoadToolbarActivator.isPressed() && !mc.options.keySaveToolbarActivator.isPressed()) {
            for (int i = 0; i < 9; ++i) {
                if (!mc.options.keysHotbar[i].wasPressed()) continue;
                this.manual_selected_slot.set(i);
            }
        }
    }

    public void onSyncedTick() {
        int _selected_slot = this.GetSelectedSlot();
        int _manual_selected_slot = this.manual_selected_slot.get();
        if (PacketUtils.get.CanChangeHotbarSlot() && _selected_slot != _manual_selected_slot && Utils.GetCurTime() > this.return_back_time.get()) {
            this.UpdateSelectedSlot(_manual_selected_slot);
        }
    }

    public boolean onPacketSend(PacketEvent.Send event) {
        UpdateSelectedSlotC2SPacket packet;
        int slot;
        if (event.packet instanceof UpdateSelectedSlotC2SPacket && !PlayerInventory.isValidHotbarIndex((int)(slot = (packet = (UpdateSelectedSlotC2SPacket)event.packet).getSelectedSlot()))) {
            return true;
        }
        return false;
    }

    public void onPacketSent(PacketEvent.Sent event) {
        if (event.packet instanceof UpdateSelectedSlotC2SPacket) {
            UpdateSelectedSlotC2SPacket packet = (UpdateSelectedSlotC2SPacket)event.packet;
            int slot = packet.getSelectedSlot();
            if (PlayerInventory.isValidHotbarIndex((int)slot)) {
                this.selected_slot.set(slot);
            }
            this.update_time.set(Utils.GetCurTime());
            this.UpdateSwitchBackTimer();
        }
    }

    public boolean onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof UpdateSelectedSlotS2CPacket) {
            UpdateSelectedSlotS2CPacket packet = (UpdateSelectedSlotS2CPacket)event.packet;
            if (Utils.GetCurTime() - this.update_time.get() > Utils.GetLatency2()) {
                this.selected_slot.set(packet.getSlot());
            }
        }
        return false;
    }

    public int GetSelectedSlot() {
        int _selected_slot = this.selected_slot.get();
        return PlayerInventory.isValidHotbarIndex((int)_selected_slot) ? _selected_slot : -1;
    }

    public void UpdateSelectedSlot(int slot) {
        assert (mc.player != null);
        mc.player.getInventory().selectedSlot = slot;
        mc.player.networkHandler.sendPacket((Packet)new UpdateSelectedSlotC2SPacket(slot));
    }

    public void UpdateSwitchBackTimer() {
        this.return_back_time.set(Utils.GetCurTime() + 2L * TickUtils.GetIntervalPerTick());
    }
}

