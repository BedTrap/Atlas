package me.bedtrapteam.addon.modules.atlas.misc;

import me.bedtrapteam.addon.Atlas;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class ChestExplorer extends Module {
    private final SettingGroup sg_general = settings.getDefaultGroup();

    private final Setting<Boolean> auto_swap = sg_general.add(new BoolSetting.Builder().name("auto-swap").defaultValue(true).build());
    private final Setting<Boolean> toggle_on_leave = sg_general.add(new BoolSetting.Builder().name("toggle-on-leave").defaultValue(true).build());
    private final Setting<Keybind> toggle_on_keybind = sg_general.add(new KeybindSetting.Builder().name("toggle-on-keybind").defaultValue(Keybind.none()).build());

    public ChestExplorer() {
        super(Atlas.Misc, "chest-explorer", "Automatically goes to closest chest, looting it and breaking.");
    }

    public BlockPos pos;
    private int stage;

    @Override
    public void onActivate() {
        stage = 1;
    }

    @Override
    public void onDeactivate() {
        stage = 1;
        mc.player.sendChatMessage("#stop");
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;
        if (toggle_on_keybind.get().isPressed()) {
            ChatUtils.info(title, "Keybind is pressed, toggling...");
            toggle();
        }
        switch (stage) {
            case 1 -> {
                if (Utils.getPlayerSpeed() < 3) {
                    mc.player.sendChatMessage("#goto chest");
                }
                stage++;
            }
            case 60 -> {
                //mc.currentScreen = null;

                if (mc.crosshairTarget instanceof BlockHitResult result) {
                    if (mc.world.getBlockState(result.getBlockPos()).getBlock() == Blocks.CHEST) {
                        pos = result.getBlockPos();
                    } else {
                        stage = 1;
                    }
                }
                if(pos != null) mine(pos);
            }
            default -> {
                stage++;
                if (mc.currentScreen == null ||  !(mc.currentScreen instanceof GenericContainerScreen)) return;
                steal(((GenericContainerScreen) mc.currentScreen).getScreenHandler());
            }
        }
    }

    private void mine(BlockPos blockPos) {
        if (auto_swap.get()) {
            FindItemResult axe = InvUtils.find(itemStack -> itemStack.getItem() == Items.DIAMOND_AXE || itemStack.getItem() == Items.NETHERITE_AXE);
            InvUtils.swap(axe.getSlot(), false);
        }
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.UP));
        mc.player.swingHand(Hand.MAIN_HAND);
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP));
    }

    @EventHandler
    public void onLeave(GameLeftEvent event) {
        if (toggle_on_leave.get()) {
            toggle();
        }
    }

    private void moveSlots(ScreenHandler handler, int start, int end) {
        if (mc.currentScreen == null) return;
        for (int i = start; i < end; i++) {
            if (!handler.getSlot(i).hasStack()) continue;

            int sleep = 50;
            if (sleep > 0) {
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            InvUtils.quickMove().slotId(i);
        }
    }

    private int getRows(ScreenHandler handler) {
        return (handler instanceof GenericContainerScreenHandler ? ((GenericContainerScreenHandler) handler).getRows() : 3);
    }

    public void steal(ScreenHandler handler) {
        if (mc.currentScreen == null) return;
        MeteorExecutor.execute(() -> moveSlots(handler, 0, getRows(handler) * 9));
    }
}
