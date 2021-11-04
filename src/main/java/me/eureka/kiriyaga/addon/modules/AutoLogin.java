package me.eureka.kiriyaga.addon.modules;

import me.eureka.kiriyaga.addon.Nigger;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

import java.util.Collections;
import java.util.List;

public class AutoLogin extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<String>> server = sgGeneral.add(new StringListSetting.Builder().name("server").defaultValue(Collections.emptyList()).build());
    private final Setting<List<String>> password = sgGeneral.add(new StringListSetting.Builder().name("password").defaultValue(Collections.emptyList()).build());

    public AutoLogin() {
        super(Nigger.Category, "auto-login", "Automatically sends password on cracked servers.");
    }

    @EventHandler
    public void onPacket(PacketEvent.Receive event) {
        assert mc.player != null;
        if (!(event.packet instanceof GameMessageS2CPacket)) return;
        String message = ((GameMessageS2CPacket) event.packet).getMessage().getString();
        if (message.contains("/l")) {
            for (String s : server.get()) {
                if (Utils.getWorldName().equals(s)) {
                    int server_index = server.get().indexOf(s);
                    String pass_index = password.get().get(server_index);
                    mc.player.sendChatMessage("/l " + pass_index);
                    break;
                }
            }
        }
    }
}
