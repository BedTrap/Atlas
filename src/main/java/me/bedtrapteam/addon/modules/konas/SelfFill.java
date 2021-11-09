package me.bedtrapteam.addon.modules.konas;

import me.bedtrapteam.addon.Atlas;
import me.bedtrapteam.addon.utils.Checker;
import me.bedtrapteam.addon.utils.ItemUtils;
import me.bedtrapteam.addon.utils.Timer;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class SelfFill extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> offset = sgGeneral.add(new DoubleSetting.Builder().name("offset").defaultValue(1).min(-30).max(30).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").defaultValue(true).build());
    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder().name("swing").defaultValue(true).build());
    private final Setting<Boolean> strict = sgGeneral.add(new BoolSetting.Builder().name("strict").defaultValue(false).build());
    private final Setting<Boolean> onlyEChest = sgGeneral.add(new BoolSetting.Builder().name("only-chests").defaultValue(false).build());

    public SelfFill() {
        super(Atlas.Konas, "self-fill", "Place block in yourself");
    }

    private State state = State.WAITING;
    private Timer timer = new Timer();
    int i = 0;

    public enum State {
        WAITING,
        DISABLING
    }

    @EventHandler
    public void onUpdate(TickEvent.Post event) {
        if (i == 0) {
            ItemUtils.negors();
            i++;
        }
        if (state == State.DISABLING) {
            if (timer.hasPassed(500)) {
                toggle();
            }
            return;
        }
        if (!mc.player.isOnGround()) {
            toggle();
            return;
        }
        if (mc.world.getBlockState(mc.player.getBlockPos()).getBlock() == Blocks.AIR) {
            BlockPos pos = mc.player.getBlockPos();

            BlockPos currentPos = pos.down();
            Direction currentFace = Direction.UP;

            Vec3d vec = new Vec3d(currentPos.getX() + 0.5, currentPos.getY() + 0.5, currentPos.getZ() + 0.5)
                .add(new Vec3d(currentFace.getUnitVector()).multiply(0.5));

            if (rotate.get()) {
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(mc.player.getYaw(), 0, true));
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYaw(), 90, true));
            }

            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.42, mc.player.getZ(), mc.player.isOnGround()));
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.75, mc.player.getZ(), mc.player.isOnGround()));
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.01, mc.player.getZ(), mc.player.isOnGround()));
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.16, mc.player.getZ(), mc.player.isOnGround()));

            boolean changeItem = mc.player.getInventory().selectedSlot != getBlockSlot().getSlot();
            int startingItem = mc.player.getInventory().selectedSlot;

            if (changeItem) {
                mc.player.getInventory().selectedSlot = getBlockSlot().getSlot();
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(getBlockSlot().getSlot()));
            }

            mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(vec, currentFace, currentPos, false)));
            if (swing.get()) {
                mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }

            if (changeItem) {
                mc.player.getInventory().selectedSlot = startingItem;
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(startingItem));
            }

            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + offset.get(), mc.player.getZ(), false));
            timer.reset();
            state = State.DISABLING;
        } else {
            toggle();
        }
    }

    @EventHandler
    public void onSPacketPlayerPosLook(PacketEvent.Receive event) {
        if (mc.currentScreen instanceof DownloadingTerrainScreen) {
            toggle();
            return;
        }
        if (event.packet instanceof PlayerPositionLookS2CPacket && !strict.get()) {
            Rotations.rotate(mc.player.getYaw(), mc.player.getPitch());
            ((PlayerPositionLookS2CPacket) event.packet).getFlags().remove(PlayerPositionLookS2CPacket.Flag.X_ROT);
            ((PlayerPositionLookS2CPacket) event.packet).getFlags().remove(PlayerPositionLookS2CPacket.Flag.Y_ROT);
        }
    }

    private FindItemResult getBlockSlot() {
        FindItemResult findItemResult;
        findItemResult = InvUtils.findInHotbar(Items.OBSIDIAN);

        if ((!findItemResult.found() || mc.player.getInventory().getMainHandStack().getItem() == Items.ENDER_CHEST) && !onlyEChest.get()) {
            findItemResult = InvUtils.findInHotbar(Items.ENDER_CHEST);
        }

        return findItemResult;
    }

    @Override
    public void onActivate() {
        i = 0;
        Checker.Check();

        if (mc.player == null || mc.world == null) {
            toggle();
            return;
        }
        if (!mc.player.isOnGround()) {
            toggle();
            return;
        }
        state = State.WAITING;
        if (!getBlockSlot().found()) {
            ChatUtils.info(title, "No blocks found!");
            toggle();
        }
    }

    @Override
    public void onDeactivate() {
        Checker.Check();
    }
}
