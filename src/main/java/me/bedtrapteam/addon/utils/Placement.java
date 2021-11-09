package me.bedtrapteam.addon.utils;

import me.bedtrapteam.addon.utils.enchansed.Player2Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class Placement {
    private final static MinecraftClient mc = MinecraftClient.getInstance();

    private final BlockPos neighbour;
    private final Direction opposite;

    private final float yaw;
    private final float pitch;

    private final Hand hand;
    private final boolean swing;
    private final boolean rotate;

    private final int slot;

    public Placement(BlockPos neighbour, Direction opposite, float yaw, float pitch, Hand hand, boolean swing, boolean rotate, int slot) {
        this.neighbour = neighbour;
        this.opposite = opposite;
        this.yaw = yaw;
        this.pitch = pitch;
        this.hand = hand;
        this.swing = swing;
        this.rotate = rotate;
        this.slot = slot;
    }

    public BlockPos getNeighbour() {
        return neighbour;
    }

    public Direction getOpposite() {
        return opposite;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public Runnable getAction() {
        return () -> {
            if (hand == Hand.MAIN_HAND && slot != -1 && mc.player.getInventory().selectedSlot != slot) {
                mc.player.getInventory().selectedSlot = slot;
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
            }

            if (mc.player.isSprinting() && !StateStorage.syncSprinting) {
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
                StateStorage.syncSneaking = true;
            }

            if (!mc.player.isSneaking() && !StateStorage.syncSneaking && Player2Utils.shouldSneakWhileClicking(mc.world.getBlockState(getNeighbour()).getBlock())) {
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
                StateStorage.syncSneaking = true;
            }

            Vec3d hitVec = new Vec3d(getNeighbour().getX() + 0.5, getNeighbour().getX() + 0.5, getNeighbour().getX() + 0.5).add(new Vec3d(getOpposite().getUnitVector()).multiply(0.5));

            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(hand, new BlockHitResult(hitVec, getOpposite(), getNeighbour(), false)));

            if (swing) {
                mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
            }
        };
    }

    public boolean isRotate() {
        return rotate;
    }
}
