package me.bedtrapteam.addon.modules.konas;

import me.bedtrapteam.addon.Atlas;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

public class KAntiSpam extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> discordLinks = sgGeneral.add(new BoolSetting.Builder().name("discord-invites").defaultValue(true).build());
    private final Setting<Boolean> domains = sgGeneral.add(new BoolSetting.Builder().name("domains").defaultValue(false).build());
    private final Setting<Boolean> announcer = sgGeneral.add(new BoolSetting.Builder().name("announcer").defaultValue(true).build());

    private static String[] discordStringArray = {"discord.gg",};
    private static String[] domainStringArray = {".com", ".ru", ".net", ".in", ".ir", ".au", ".uk", ".de", ".br", ".xyz", ".org", ".co", ".cc", ".me", ".tk", ".us", ".bar", ".gq", ".nl", ".space"};
    private static String[] announcerStringArray = {"Looking for new anarchy servers?", "I just walked", "I just flew", "I just placed", "I just ate", "I just healed", "I just took", "I just spotted", "I walked", "I flew", "I walked", "I flew", "I placed", "I ate", "I healed", "I took", "I gained", "I mined", "I lost", "I moved"};

    public KAntiSpam() {
        super(Atlas.Konas,"k-anti-spam", "Hides spam in chat");
    }

    @EventHandler
    private void onPacket(PacketEvent.Receive event) {
        if (mc.world == null || mc.player == null) return;

        if (!(event.packet instanceof GameMessageS2CPacket)) {
            return;
        }

        GameMessageS2CPacket chatMessage = (GameMessageS2CPacket) event.packet;

        if (detectSpam(chatMessage.getMessage().getString())) {
            event.setCancelled(true);
        }
    }

    private boolean detectSpam(String message) {
        if (discordLinks.get()) {
            for (String discordSpam : discordStringArray) {
                if (message.contains(discordSpam)) {
                    return true;
                }
            }
        }

        if (announcer.get()) {
            for (String announcerSpam : announcerStringArray) {
                if (message.contains(announcerSpam)) {
                    return true;
                }
            }
        }

        if (domains.get()) {
            for (String domainSpam : domainStringArray) {
                if (message.contains(domainSpam)) {
                    return true;
                }
            }
        }

        return false;
    }
}
