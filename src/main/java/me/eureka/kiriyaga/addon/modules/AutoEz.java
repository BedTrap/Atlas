package me.eureka.kiriyaga.addon.modules;

import me.eureka.kiriyaga.addon.Nigger;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AutoEz extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> message = sgGeneral.add(new StringSetting.Builder()
            .name("message")
            .description("Send a chat message about killing a player.")
            .defaultValue("U got fucked up by n1gger++ addon.")
            .build()
    );

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-friends")
            .defaultValue(true)
            .build()
    );

    ArrayList<Pair<UUID, Long>> players = new ArrayList<>();
    ArrayList<String> msgplayers = new ArrayList<>();


    public AutoEz() {
        super(Nigger.Category, "auto-ez", "Send a chat message after killing a player.");
    }

    @Override
    public void onActivate() {
        players.clear();
        msgplayers.clear();
    }

    private boolean checkFriend(PlayerEntity p) {
        return (ignoreFriends.get() && Friends.get().isFriend(p));
    }

    @EventHandler
    private void AttackEntity(AttackEntityEvent e) {
        if (e.entity instanceof EndCrystalEntity) {
            List<AbstractClientPlayerEntity> worldplayers = mc.world.getPlayers();
            for (int x = 0; x < worldplayers.size(); x++) {
                PlayerEntity p = worldplayers.get(x);
                if (!p.isSpectator() && !p.isCreative() && !p.isInvulnerable() && !mc.player.equals(p) && !checkFriend(p) && p.distanceTo(e.entity) < 12) {

                    Pair<UUID, Long> pair = new Pair<>(p.getUuid(), System.currentTimeMillis());
                    int index = -1;
                    for (int w = 0; w < players.size(); w++) {
                        if (players.get(w).getLeft().equals(p.getUuid())) {
                            index = w;
                            break;
                        }
                    }
                    if (index == -1) {
                        players.add(pair);
                    } else {
                        players.set(index, pair);
                    }

                }
            }
        }

        if (e.entity instanceof PlayerEntity) {
            PlayerEntity p = (PlayerEntity) e.entity;
            if (!p.isSpectator() && !p.isCreative() && !p.isInvulnerable() && !mc.player.equals(p) && !checkFriend(p)) {

                Pair<UUID, Long> pair = new Pair<>(p.getUuid(), System.currentTimeMillis());
                int index = -1;
                for (int w = 0; w < players.size(); w++) {
                    if (players.get(w).getLeft().equals(p.getUuid())) {
                        index = w;
                        break;
                    }
                }
                if (index == -1) {
                    players.add(pair);
                } else {
                    players.set(index, pair);
                }
            }
        }
    }


    @EventHandler
    private void onTick(TickEvent.Pre e) {
        if (players.size() == 0) return;

        ArrayList<Pair<UUID, Long>> newPlayers = players;

        for (int x = 0; x < players.size(); x++) {
            Pair<UUID, Long> w = players.get(x);
            long time = w.getRight();

            PlayerEntity p = mc.world.getPlayerByUuid(w.getLeft());

            if (System.currentTimeMillis() - time > 2000 || p == null) {
                newPlayers.remove(x);
                continue;
            }

            if (p.isDead()) {
                if (!msgplayers.contains(p.getName().asString()))
                    msgplayers.add(p.getName().asString());
                newPlayers.remove(x);
                MeteorExecutor.execute(() -> send());
            }
        }

        players = newPlayers;
    }

    private void send() {
        int size = msgplayers.size();
        try {
            Thread.sleep(500);
        } catch (Exception e) {
        }
        if (size != msgplayers.size()) {
            MeteorExecutor.execute(() -> send());
            return;
        }

        if (msgplayers.size() == 0) return;
        String message = this.message.get();

        if (message.contains("n1gger++ addon")) {
            message = message.replace("{name}", String.join(", ", msgplayers));
        } else {
            message = message.replace("{name}", String.join(", ", msgplayers)) + " | n1gger++ addon.";
        }
        mc.player.sendChatMessage(message);

        msgplayers.clear();
    }

}
