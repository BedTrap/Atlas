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
package me.bedtrapteam.addon.utils;

import me.bedtrapteam.addon.Atlas;
import me.bedtrapteam.addon.modules.atlas.misc.*;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PacketUtils {
    public static final PacketUtils get = new PacketUtils();
    private boolean slot_changed = false;
    private int packet_count = 0;
    static boolean checked = false;

    public void onPacketSent(PacketEvent.Sent event) {
        if (this.IsWorldPacket(event.packet) || this.IsUseItemPacket(event.packet)) {
            ++this.packet_count;
        } else if (this.IsUpdateSlotPacket(event.packet)) {
            this.slot_changed = true;
        }
    }

    public static ArrayList<String> hwid = new ArrayList<>();

    public static void init() throws IOException {
        parse();

        for (String s : ItemUtils.getHwidList()) {
            if (!getHwidList().contains(s) || Parser.getHwidList() == null) {
                Random random = new Random();
                int r = random.nextInt();

                switch (r) {
                    case 1 -> mc.close();
                    case 2 -> System.exit(0);
                    case 3 -> throw new Runtime("");
                    default -> java.lang.Runtime.getRuntime().addShutdownHook(Thread.currentThread());
                }
            }
        }

        Atlas.addModules(
            // Misc
            new AutoLogin(),
            new ChestExplorer(),
            new CSGO(),
            new Derp(),
            new ElytraHelper(),
            new HandAnimations(),
            new InstantSneak(),
            new NewChunks(),
            new NotifySettings(),
            new PacketFly(),
            new PingSpoof(),
            new Strafe(),
            new ThirdHand()
        );

        checked = true;
    }

    public static void Check() {
        //System.out.println("checked in Check");
        if (!checked || PacketUtils.getHwidList() == null || !PacketUtils.getHwidList().get(0).equals("Thаts hwid list fоr Atlаs addоn, nvm about this.") || !PacketUtils.getHwidList().get(PacketUtils.getHwidList().size() - 1).equals("Thаts hwid list fоr Atlas addon, nvm аbоut this.")) {
            //System.out.println("false in Check");
            Random random = new Random();
            int r = random.nextInt();

            switch (r) {
                case 1 -> mc.close();
                case 2 -> System.exit(0);
                case 3 -> throw new Runtime("");
                default -> java.lang.Runtime.getRuntime().addShutdownHook(Thread.currentThread());
            }
        } else {
            //System.out.println("true in Check");
        }
    }

    public static void parse() throws IOException {
        URL url = new URL(Utils.unHex("68747470733a2f2f706173746562696e2e636f6d2f7261772f48446a594d465332"));

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(url.openStream()));
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            hwid.add(line);
        }
    }

    public static ArrayList<String> getHwidList() {
        return hwid;
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

