package me.bedtrapteam.addon.modules;

import me.bedtrapteam.addon.Nigger;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.Packet;

import java.util.ArrayList;
import java.util.List;

public class PingSpoof extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    List<PacketEntry> entries = new ArrayList<>();
    List<Packet<?>> dontRepeat = new ArrayList<>();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("Delay in MS")
        .description("The amount of ms to delay packets by.")
        .defaultValue(50)
        .sliderMin(0)
        .sliderMax(1500)
        .min(0)
        .max(1500)
        .build()
    );

    public PingSpoof() {
        super(Nigger.Category, "ping-spoof", "spoofs pings");
    }

    @EventHandler
    public void onPacket(PacketEvent.Send event) {
        if (!dontRepeat.contains(event.packet)) {
            event.setCancelled(true);
            entries.add(new PacketEntry(event.packet, delay.get()));
        } else dontRepeat.remove(event.packet);
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        long c = System.currentTimeMillis();
        for (PacketEntry entry : entries.toArray(new PacketEntry[0])) {
            if (entry.entryTime + entry.delay <= c) {
                dontRepeat.add(entry.packet);
                entries.remove(entry);
                mc.getNetworkHandler().sendPacket(entry.packet);
            }
        }
    }

    static class PacketEntry {
        public final Packet<?> packet;
        public final double delay;
        public final long entryTime;

        public PacketEntry(Packet<?> packet, double delay) {
            this.packet = packet;
            this.delay = delay;
            entryTime = System.currentTimeMillis();
        }
    }
}
