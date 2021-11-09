package me.bedtrapteam.addon.utils;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

public class InteractionUtil {
    private static MinecraftClient mc = MinecraftClient.getInstance();

    public static ConcurrentHashMap<BlockPos, Long> ghostBlocks = new ConcurrentHashMap<>();

    public static Placement place(Block block, BlockPos pos, boolean rotate) {
        return place(block, pos, rotate, true, false);
    }

    public static Placement place(Block block, BlockPos pos, boolean rotate, boolean swing) {
        return place(block, pos, rotate, swing, false);
    }

    public static Placement place(Block block, BlockPos pos, boolean rotate, boolean swing, boolean strictDirection) {
        Hand hand = null;
        int slot = -1;

        final ItemStack mainhandStack = mc.player.getMainHandStack();
        if (mainhandStack != ItemStack.EMPTY && mainhandStack.getItem() instanceof BlockItem) {
            final Block blockFromMainhandItem = ((BlockItem) mainhandStack.getItem()).getBlock();
            if (blockFromMainhandItem == block) {
                hand = Hand.MAIN_HAND;
                slot = mc.player.getInventory().selectedSlot;
            }
        }

        final ItemStack offhandStack = mc.player.getOffHandStack();
        if (offhandStack != ItemStack.EMPTY && offhandStack.getItem() instanceof BlockItem) {
            final Block blockFromOffhandItem = ((BlockItem) offhandStack.getItem()).getBlock();
            if (blockFromOffhandItem == block) {
                hand = Hand.OFF_HAND;
            }
        }

        if (hand == null) {
            for (int i = 0; i < 9; i++) {
                final ItemStack stack = mc.player.getInventory().getStack(i);
                if (stack != ItemStack.EMPTY && stack.getItem() instanceof BlockItem) {
                    final Block blockFromItem = ((BlockItem) stack.getItem()).getBlock();
                    if (blockFromItem == block) {
                        hand = Hand.MAIN_HAND;
                        slot = i;
                        break;
                    }
                }
            }
        }

        if (hand == null) return null;
        return place(pos, rotate,  swing, strictDirection, hand, slot);
    }

    public static Placement place(BlockPos pos, boolean rotate, boolean swing, boolean strictDirection, Hand hand, int slot) {
        if (!canPlaceBlock(pos)) return null;
        Direction side = getPlaceDirection(pos, strictDirection);
        if (side == null) {
            return null;
        }
        BlockPos neighbour = pos.offset(side);
        Direction opposite = side.getOpposite();
        Vec3d hitVec = new Vec3d(neighbour.getX() + 0.5, neighbour.getY() + 0.5, neighbour.getZ() + 0.5).add(new Vec3d(opposite.getUnitVector()).multiply(0.5));
        float[] angle = LookCalculator.calculateAngle(hitVec);
        Placement placement = new Placement(neighbour, opposite, angle[0], angle[1], hand, swing, rotate, slot);
        if (ActionManager.add(new Action(placement))) {
            return placement;
        }
        return null;
    }
    public static boolean canPlaceBlock(BlockPos pos) {
        return canPlaceBlock(pos, false);
    }

    public static boolean canPlaceBlock(BlockPos pos, boolean strictDirection) {
        if (ghostBlocks.containsKey(pos)) {
            if (System.currentTimeMillis() - ghostBlocks.get(pos) > 500) {
                ghostBlocks.remove(pos);
            } else {
                return false;
            }
        }
        if (!mc.world.getBlockState(pos).getMaterial().isReplaceable()) return false;
        if (strictDirection) {
            if (getPlaceDirection(pos, true) == null) return false;
        }
        if (InteractionRules.ignoreEntities) return true;
        return mc.world.canPlace(Blocks.DIRT.getDefaultState(), pos, ShapeContext.absent());
    }

    public static boolean canClick(BlockPos pos) {
        if (ghostBlocks.containsKey(pos)) {
            if (System.currentTimeMillis() - ghostBlocks.get(pos) > 500) {
                ghostBlocks.remove(pos);
            } else {
                return true;
            }
        }

        BlockState state = mc.world.getBlockState(pos);

        return InteractionRules.airPlace || (!state.isAir() && state.getFluidState().isEmpty());
    }

    public static Direction getPlaceDirection(BlockPos pos, boolean strictDirection) {
        ArrayList<Direction> validFacings = new ArrayList<>();
        for (Direction side : Direction.values()) {
            BlockPos neighbour = pos.offset(side);

            if (!canClick(neighbour)) continue;

            if (strictDirection) {
                Vec3d eyePos = LookCalculator.getEyesPos(mc.player);
                Vec3d blockCenter = new Vec3d(neighbour.getX() + 0.5, neighbour.getY() + 0.5, neighbour.getZ() + 0.5);
                BlockState blockState = mc.world.getBlockState(neighbour);
                boolean isFullBox = blockState.getBlock() == Blocks.AIR || blockState.isFullCube(mc.world, neighbour);
                ArrayList<Direction> validAxis = new ArrayList<>();
                validAxis.addAll(checkAxis(eyePos.x - blockCenter.x, Direction.WEST, Direction.EAST, !isFullBox));
                validAxis.addAll(checkAxis(eyePos.y - blockCenter.y, Direction.DOWN, Direction.UP, true));
                validAxis.addAll(checkAxis(eyePos.z - blockCenter.z, Direction.NORTH, Direction.SOUTH, !isFullBox));
                if (!validAxis.contains(side.getOpposite())) continue;
            }

            validFacings.add(side);
        }
        return validFacings.stream()
            .min(Comparator.comparing(facing ->
                LookCalculator.getEyesPos(mc.player).distanceTo(
                    new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                        .add(new Vec3d(facing.getUnitVector()).multiply(0.5)))))
            .orElse(null);
    }

    public static ArrayList<Direction> checkAxis(double diff, Direction negativeSide, Direction positiveSide, boolean bothIfInRange) {
        ArrayList<Direction> valid = new ArrayList<>();
        if (diff < -0.5) {
            valid.add(negativeSide);
        }
        if (diff > 0.5) {
            valid.add(positiveSide);
        }
        if (bothIfInRange) {
            if (!valid.contains(negativeSide)) valid.add(negativeSide);
            if (!valid.contains(positiveSide)) valid.add(positiveSide);
        }
        return valid;
    }
}
