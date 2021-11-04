package me.bedtrapteam.addon.modules;

import me.bedtrapteam.addon.Nigger;
import me.bedtrapteam.addon.utils.enchansed.Player2Utils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class BurrowAlert extends Module {
    private int burrowMsgWait;
    public static List<PlayerEntity> burrowedPlayers = new ArrayList();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder().name("range").description("How far away from you to check for burrowed players.").defaultValue(2).min(0).sliderMax(10).build());
    private final Setting<Boolean> announce = sgGeneral.add(new BoolSetting.Builder().name("announce").description("Troll players in chat that burrow.").defaultValue(true).build());


    public BurrowAlert() {
        super(Nigger.Category, "burrow-alert", "Alerts you when players are burrowed.");
    }

    public void onActivate() {
        this.burrowMsgWait = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        Iterator var2 = this.mc.world.getEntities().iterator();

        while(var2.hasNext()) {
            PlayerEntity player = (PlayerEntity)var2.next();
            if (this.isValid(player)) {
                burrowedPlayers.add(player);
                if (announce.get()) {
                    if (this.burrowMsgWait <= 0) {
                        this.sendBurrowTroll(player);
                        this.burrowMsgWait = 100;
                    } else {
                        --this.burrowMsgWait;
                    }
                }

                ChatUtils.warning(title, player.getName() + " is burrowed!");
            }

            if (burrowedPlayers.contains(player) && !isBurrowed(player, true)) {
                burrowedPlayers.remove(player);
                ChatUtils.warning(title, player.getName() + " is no longer burrowed.");
            }
        }

    }

    private void sendBurrowTroll(PlayerEntity p) {
        if (isBurrowed(p, true)) {
            if (p != this.mc.player) {
                Random random = new Random();
                int selector = random.nextInt(0) + 5;
                String burrowMessage = "Imagine using burrow, " + p.getName();
                if (selector == 1) {
                    burrowMessage = p.getName() + " just burrowed, what a loser!";
                }

                if (selector == 2) {
                    burrowMessage = "Imagine needing burrow, " + p.getName();
                }

                if (selector == 3) {
                    burrowMessage = "Imagine using burrow, " + p.getName();
                }

                if (selector == 4) {
                    burrowMessage = "Keep burrowing, " + p.getName() + "! It won't save you.";
                }

                if (selector == 5) {
                    burrowMessage = "Spamming burrow won't save you, " + p.getName();
                }

                this.mc.player.sendChatMessage(burrowMessage);
            }
        }
    }

    private boolean isValid(PlayerEntity p) {
        if (p == mc.player) return false;
        return mc.player.distanceTo(p) <= range.get() && !burrowedPlayers.contains(p) && isBurrowed(p, true) && !Player2Utils.isPlayerMoving(p);
    }

    public boolean isBurrowed(PlayerEntity p, boolean holeCheck) {
        BlockPos pos = p.getBlockPos();
        if (holeCheck && isInHole(p)) return false;
        return mc.world.getBlockState(pos).getBlock() == Blocks.ENDER_CHEST || mc.world.getBlockState(pos).getBlock()== Blocks.OBSIDIAN || isAnvilBlock(pos);
    }

    public boolean isAnvilBlock(BlockPos pos) {
        return mc.world.getBlockState(pos).getBlock() == Blocks.ANVIL || mc.world.getBlockState(pos).getBlock() == Blocks.CHIPPED_ANVIL || mc.world.getBlockState(pos).getBlock() == Blocks.DAMAGED_ANVIL;
    }

    public boolean isInHole(PlayerEntity p) {
        BlockPos pos = p.getBlockPos();
        return !mc.world.getBlockState(pos.add(1, 0, 0)).isAir()
            && !mc.world.getBlockState(pos.add(-1, 0, 0)).isAir()
            && !mc.world.getBlockState(pos.add(0, 0, 1)).isAir()
            && !mc.world.getBlockState(pos.add(0, 0, -1)).isAir()
            && !mc.world.getBlockState(pos.add(0, -1, 0)).isAir();
    }

}
