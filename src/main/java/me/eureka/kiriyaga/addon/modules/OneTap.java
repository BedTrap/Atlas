package me.eureka.kiriyaga.addon.modules;

import me.eureka.kiriyaga.addon.Nigger;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;

public class OneTap extends Module {
    private final SettingGroup sg_general = this.settings.getDefaultGroup();

    private final Setting<Boolean> bows = sg_general.add(new BoolSetting.Builder().name("bow").defaultValue(true).build());
    private final Setting<Boolean> pearls = sg_general.add(new BoolSetting.Builder().name("pearl").defaultValue(false).build());
    private final Setting<Integer> wait = sg_general.add(new IntSetting.Builder().name("wait").min(0).max(2500).sliderMin(0).sliderMax(2500).defaultValue(300).build());
    private final Setting<Integer> packets = sg_general.add(new IntSetting.Builder().name("packets").min(0).max(2500).sliderMin(0).sliderMax(2500).defaultValue(120).build());
    private final Setting<Boolean> bypass = sg_general.add(new BoolSetting.Builder().name("bypass").defaultValue(true).build());

    public OneTap() {
        super(Nigger.Category, "one-tap", "One tapping by using bow exploit.");
    }

    private boolean shooting;
    private long lastShootTime;

    @Override
    public void onActivate() {
        shooting = false;
        lastShootTime = System.currentTimeMillis();
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof PlayerActionC2SPacket packet) {
            if (packet.getAction() == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM) {
                ItemStack handStack = mc.player.getStackInHand(Hand.MAIN_HAND);

                if (!handStack.isEmpty() && handStack.getItem() != null && handStack.getItem() instanceof BowItem && bows.get()) {
                    doSpoofs();
                }
            }

        } else if (event.packet instanceof PlayerInteractItemC2SPacket packet2) {
            if (packet2.getHand() == Hand.MAIN_HAND) {
                ItemStack handStack = mc.player.getStackInHand(Hand.MAIN_HAND);

                if (!handStack.isEmpty() && handStack.getItem() != null) {
                    if (handStack.getItem() instanceof EnderPearlItem && pearls.get()) {
                        doSpoofs();
                    }
                }
            }
        }
    }

    private void doSpoofs() {
        if (System.currentTimeMillis() - lastShootTime >= wait.get()) {
            shooting = true;
            lastShootTime = System.currentTimeMillis();

            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));

            for (int index = 0; index < packets.get(); ++index) {
                if (bypass.get()) {
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1e-10, mc.player.getZ(), false));
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() - 1e-10, mc.player.getZ(), true));
                } else {
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() - 1e-10, mc.player.getZ(), true));
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1e-10, mc.player.getZ(), false));
                }

            }

            shooting = false;
        }
    }
}
