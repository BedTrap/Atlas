package me.bedtrapteam.addon.modules.atlas.combat;

import me.bedtrapteam.addon.Atlas;
import me.bedtrapteam.addon.utils.Checker;
import me.bedtrapteam.addon.utils.InitializeUtils;
import me.bedtrapteam.addon.utils.ItemUtils;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class AutoEz extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgKillMessage = settings.createGroup("Kill");
    private final SettingGroup sgHelmMessage = settings.createGroup("Breaking Helm");
    private final SettingGroup sgLogOut = settings.createGroup("Log Out");

    private final Setting<Boolean> breakingHelm = sgGeneral.add(new BoolSetting.Builder().name("breaking-helm").description("Chat alert to send when you breaking enemy's helm.").defaultValue(true).build());
    private final Setting<Boolean> logOut = sgGeneral.add(new BoolSetting.Builder().name("log-out").description("Chat alert to send enemy logged out.").defaultValue(false).build());
    private final Setting<Boolean> antiSpam = sgGeneral.add(new BoolSetting.Builder().name("anti-spam").description("Adds random number to the message to prevent being kicked.").defaultValue(false).build());
    private final Setting<List<String>> killMessage = sgKillMessage.add(new StringListSetting.Builder().name("kill-message").defaultValue(Arrays.asList("<target> added to the <player> montage")).build());
    private final Setting<List<String>> helmMessage = sgHelmMessage.add(new StringListSetting.Builder().name("helm-message").defaultValue(Arrays.asList("<target> helm get owned")).build());
    private final Setting<List<String>> logMessage = sgLogOut.add(new StringListSetting.Builder().name("log-message").defaultValue(Arrays.asList("<target> got scared")).build());

    public AutoEz() {
        super(Atlas.Combat, "auto-ez", "Toxic module. Send messages on every kill or broken helm. Beta version");
    }

    private boolean msgKill = false;
    private boolean msgHelm = false;
    private boolean msgLog = false;
    private int timer;
    int b = 0;

    private final List<PlayerListEntry> lastPlayerList = new ArrayList<>();
    private final List<PlayerEntity> lastPlayers = new ArrayList<>();
    private PlayerEntity target;

    @Override
    public void onActivate() {
        Checker.Check();

        msgKill = false;
        lastPlayerList.addAll(mc.getNetworkHandler().getPlayerList());
        updateLastPlayers();
        timer = 10;

        b = 0;
    }

    @Override
    public void onDeactivate() {
        Checker.Check();
    }

    @EventHandler
    public void onAttack(AttackEntityEvent event) {
        if (event.entity instanceof PlayerEntity) {
            target = (PlayerEntity) event.entity;
            msgKill = false;
        }
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (b == 0) {
            ItemUtils.Check();
            b++;
        }
        if (target == null) msgKill = false;
        if (target != null && mc.player != null) {
            // Break Helm
            if (breakingHelm.get() && target.isAlive() && !target.getInventory().getArmorStack(3).isEmpty()) {
                ItemStack head = target.getInventory().getArmorStack(3);
                int durabilityHead = head.getMaxDamage() - head.getDamage();
                if (durabilityHead <= 6 && !msgHelm) {
                    sendChatMessage(getHelmMessage());
                    msgHelm = true;
                }
            }
            // Kill Message
            if ((target.getHealth() <= 0.0f || target.isDead()) && !msgKill) {
                sendChatMessage(getKillMessage());

                msgHelm = false;
                msgKill = true;
                target = null;
            }
            // Log Out
            if (logOut.get()) {
                if (mc.getNetworkHandler().getPlayerList().size() != lastPlayerList.size()) {
                    for (PlayerListEntry entry : lastPlayerList) {
                        if (mc.getNetworkHandler().getPlayerList().stream().anyMatch(playerListEntry -> playerListEntry.getProfile().getName().equals(target.getName().asString())))
                            msgLog = false;
                        if (mc.getNetworkHandler().getPlayerList().stream().anyMatch(playerListEntry -> playerListEntry.getProfile().equals(entry.getProfile())))
                            continue;

                        if (target != null && !msgLog) {
                            for (PlayerEntity player : lastPlayers) {
                                if (player.getUuid().equals(entry.getProfile().getId())
                                    && entry.getProfile().getName().equals(target.getName().asString())) {
                                    sendChatMessage(getLogMessage());
                                    msgLog = true;
                                }
                            }
                        }
                    }

                    lastPlayerList.clear();
                    lastPlayerList.addAll(mc.getNetworkHandler().getPlayerList());
                    updateLastPlayers();
                }
            }
        }
        if (timer <= 0) {
            updateLastPlayers();
            timer = 10;
        } else {
            timer--;
        }
    }

    private void sendChatMessage(String message) {
        String text = (name == null ? message : message.replaceAll(
            "<target>", target.getName().asString()).replace(
            "<player>", mc.getSession().getUsername()));
        if (antiSpam.get()) {
            Random random = new Random();
            String random_ = " " + random.nextInt(999);
            mc.player.sendChatMessage(text + random_);
        } else {
            mc.player.sendChatMessage(text);
        }
    }

    public String getKillMessage() {
        Random random = new Random();
        int size = random.nextInt(killMessage.get().size());
        int message = killMessage.get().indexOf(size);
        for (int i = 0; i < killMessage.get().size(); i++) {
            if (i == size) message = i;
        }
        return killMessage.get().get(message);
    }

    public String getHelmMessage() {
        Random random = new Random();
        int size = random.nextInt(helmMessage.get().size());
        int message = helmMessage.get().indexOf(size);
        for (int i = 0; i < helmMessage.get().size(); i++) {
            if (i == size) message = i;
        }
        return helmMessage.get().get(message);
    }

    public String getLogMessage() {
        Random random = new Random();
        int size = random.nextInt(logMessage.get().size());
        int message = logMessage.get().indexOf(size);
        for (int i = 0; i < logMessage.get().size(); i++) {
            if (i == size) message = i;
        }
        return logMessage.get().get(message);
    }

    private void updateLastPlayers() {
        lastPlayers.clear();
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity) lastPlayers.add((PlayerEntity) entity);
        }
    }
}
