package me.eureka.kiriyaga.addon.enchansed_utils;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.*;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.LightType;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.EVENT_BUS;
import static meteordevelopment.meteorclient.utils.Utils.mc;

public class Block2Utils {
    private static final Vec3d hitPos = new Vec3d(0, 0, 0);

    public static boolean breaking;
    private static boolean breakingThisTick;

    public static void init() {
        EVENT_BUS.subscribe(Block2Utils.class);
    }

    // Get Block

    public static Block getBlock(BlockPos p) {
        if (p == null) return null;
        return mc.world.getBlockState(p).getBlock();
    }

    public static boolean isAir(Block block) {
        return block == Blocks.AIR;
    }

    public static boolean isAnvilBlock(BlockPos pos) {
        return Block2Utils.getBlock(pos) == Blocks.ANVIL || Block2Utils.getBlock(pos) == Blocks.CHIPPED_ANVIL || Block2Utils.getBlock(pos) == Blocks.DAMAGED_ANVIL;
    }

    public static boolean isTrapBlock(BlockPos pos) {
        return getBlock(pos) == Blocks.OBSIDIAN || getBlock(pos) == Blocks.ENDER_CHEST;
    }

    public static boolean isWeb(BlockPos pos) {
        return getBlock(pos) == Blocks.COBWEB || getBlock(pos) == Block.getBlockFromItem(Items.STRING);
    }

    public static boolean isBlastResistant(Block block) {
        if (block == Blocks.BEDROCK || block == Blocks.OBSIDIAN || block == Blocks.ENDER_CHEST || block == Blocks.ANCIENT_DEBRIS || block == Blocks.CRYING_OBSIDIAN || block == Blocks.ENCHANTING_TABLE || block == Blocks.NETHERITE_BLOCK || block == Blocks.ANVIL || block == Blocks.CHIPPED_ANVIL || block == Blocks.DAMAGED_ANVIL) return true;
        return block == Blocks.RESPAWN_ANCHOR && !mc.world.getDimension().isBedWorking();
    }

    public static List<BlockPos> getSphere(BlockPos centerPos, int radius, int height) {
        ArrayList<BlockPos> blocks = new ArrayList<>();
        for (int i = centerPos.getX() - radius; i < centerPos.getX() + radius; i++) {
            for (int j = centerPos.getY() - height; j < centerPos.getY() + height; j++) {
                for (int k = centerPos.getZ() - radius; k < centerPos.getZ() + radius; k++) {
                    BlockPos pos = new BlockPos(i, j, k);
                    if (distanceBetween(centerPos, pos) <= radius && !blocks.contains(pos)) blocks.add(pos);
                }
            }
        }

        return blocks;
    }

    public static Direction rayTraceCheck(BlockPos blockPos, boolean bl) {
        Vec3d vec3d = new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());
        for (Direction direction : Direction.values()) {
            RaycastContext raycastContext = new RaycastContext(vec3d, new Vec3d((double)blockPos.getX() + 0.5 + (double)direction.getVector().getX() * 0.5, (double)blockPos.getY() + 0.5 + (double)direction.getVector().getY() * 0.5, (double)blockPos.getZ() + 0.5 + (double)direction.getVector().getZ() * 0.5), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
            BlockHitResult blockHitResult = mc.world.raycast(raycastContext);
            if (blockHitResult == null || blockHitResult.getType() != HitResult.Type.BLOCK || !blockHitResult.getBlockPos().equals(blockPos)) continue;

            return direction;
        }

        if (bl) {
            if ((double)blockPos.getY() > vec3d.y) return Direction.DOWN;

            return Direction.UP;
        }

        return null;
    }

    // Placing

    public static boolean place(BlockPos blockPos, FindItemResult findItemResult, int rotationPriority) {
        return place(blockPos, findItemResult, rotationPriority, true);
    }

    public static boolean place(BlockPos blockPos, FindItemResult findItemResult, boolean rotate, int rotationPriority) {
        return place(blockPos, findItemResult, rotate, rotationPriority, true);
    }

    public static boolean place(BlockPos blockPos, FindItemResult findItemResult, boolean rotate, int rotationPriority, boolean checkEntities) {
        return place(blockPos, findItemResult, rotate, rotationPriority, true, checkEntities);
    }

    public static boolean place(BlockPos blockPos, FindItemResult findItemResult, int rotationPriority, boolean checkEntities) {
        return place(blockPos, findItemResult, true, rotationPriority, true, checkEntities);
    }

    public static boolean place(BlockPos blockPos, FindItemResult findItemResult, boolean rotate, int rotationPriority, boolean swingHand, boolean checkEntities) {
        return place(blockPos, findItemResult, rotate, rotationPriority, swingHand, checkEntities, true);
    }

    public static boolean place(BlockPos blockPos, FindItemResult findItemResult, boolean rotate, int rotationPriority, boolean swingHand, boolean checkEntities, boolean swapBack) {
        if (findItemResult.isOffhand()) return place(blockPos, Hand.OFF_HAND, mc.player.getInventory().selectedSlot, rotate, rotationPriority, swingHand, checkEntities, swapBack);
        else if (findItemResult.isHotbar()) return place(blockPos, Hand.MAIN_HAND, findItemResult.getSlot(), rotate, rotationPriority, swingHand, checkEntities, swapBack);
        return false;
    }

    public static boolean place(BlockPos blockPos, Hand hand, int slot, boolean rotate, int rotationPriority, boolean swingHand, boolean checkEntities, boolean swapBack) {
        if (slot < 0 || slot > 8) return false;
        if (!canPlace(blockPos, checkEntities)) return false;

        ((IVec3d) hitPos).set(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);

        BlockPos neighbour;
        Direction side = getPlaceSide(blockPos);

        if (side == null) {
            side = Direction.UP;
            neighbour = blockPos;
        } else {
            neighbour = blockPos.offset(side.getOpposite());
            hitPos.add(side.getOffsetX() * 0.5, side.getOffsetY() * 0.5, side.getOffsetZ() * 0.5);
        }

        Direction s = side;

        if (rotate) {
            Rotations.rotate(Rotations.getYaw(hitPos), Rotations.getPitch(hitPos), rotationPriority, () -> {
                InvUtils.swap(slot, swapBack);

                place(new BlockHitResult(hitPos, s, neighbour, false), hand, swingHand);

                if (swapBack) InvUtils.swapBack();
            });
        } else {
            InvUtils.swap(slot, swapBack);

            place(new BlockHitResult(hitPos, s, neighbour, false), hand, swingHand);

            if (swapBack) InvUtils.swapBack();
        }


        return true;
    }

    private static void place(BlockHitResult blockHitResult, Hand hand, boolean swing) {
        boolean wasSneaking = mc.player.input.sneaking;
        mc.player.input.sneaking = false;

        ActionResult result = mc.interactionManager.interactBlock(mc.player, mc.world, hand, blockHitResult);

        if (result.shouldSwingHand()) {
            if (swing) mc.player.swingHand(hand);
            else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
        }

        mc.player.input.sneaking = wasSneaking;
    }

    public static boolean placeEnhanced(BlockPos blockPos, Hand hand, int n, boolean bl, int n2, boolean bl2, boolean bl3, boolean bl4, boolean bl5) {
        BlockPos blockPos1;
        Vec3d vec3d;
        if (n == -1 || !canPlace(blockPos, bl3)) return false;
        Direction direction = getPlaceSide2(blockPos);
        Vec3d vec3d1 = vec3d = bl ? new Vec3d(0.0, 0.0, 0.0) : hitPos;

        if (direction == null) {
            direction = Direction.UP;
            blockPos1 = blockPos;
            ((IVec3d)vec3d).set(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
        } else {
            blockPos1 = blockPos.offset(direction.getOpposite());
            ((IVec3d)vec3d).set(blockPos1.getX() + 0.5 + direction.getOffsetX() * 0.5, blockPos1.getY() + 0.6 + direction.getOffsetY() * 0.5, blockPos1.getZ() + 0.5 + direction.getOffsetZ() * 0.5);
        }

        if (bl) {
            Direction direction1 = direction;
            Rotations.rotate(Rotations.getYaw(vec3d), Rotations.getPitch(vec3d), n2, () -> placeEnhanced2(n, vec3d, hand, direction1, blockPos1, bl2, bl4, bl5));
        } else placeEnhanced(n, vec3d, hand, direction, blockPos1, bl2, bl4, bl5);

        return true;
    }

    public static boolean placeEnhanced(BlockPos blockPos, Hand hand, int n, boolean bl, int n2, boolean bl2) {
        return placeEnhanced(blockPos, hand, n, bl, n2, true, bl2, true, true);
    }

    private static void placeEnhanced(int n, Vec3d vec3d, Hand hand, Direction direction, BlockPos blockPos, boolean bl, boolean bl2, boolean bl3) {
        int n2 = mc.player.getInventory().selectedSlot;
        if (bl2) mc.player.getInventory().selectedSlot = n;
        boolean bl4 = mc.player.input.sneaking;
        mc.player.input.sneaking = false;
        mc.interactionManager.interactBlock(mc.player, mc.world, hand, new BlockHitResult(vec3d, direction, blockPos, false));
        if (bl) mc.player.swingHand(hand);
        else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
        mc.player.input.sneaking = bl4;
        if (bl3) mc.player.getInventory().selectedSlot = n2;
    }

    private static void placeEnhanced2(int n, Vec3d vec3d, Hand hand, Direction direction, BlockPos blockPos, boolean bl, boolean bl2, boolean bl3) {
        placeEnhanced(n, vec3d, hand, direction, blockPos, bl, bl2, bl3);
    }

    public static boolean canPlace(BlockPos blockPos, boolean checkEntities) {
        if (blockPos == null) return false;

        // Check y level
        if (!World.isValid(blockPos)) return false;

        // Check if current block is replaceable
        if (!mc.world.getBlockState(blockPos).getMaterial().isReplaceable()) return false;

        // Check if intersects entities
        return !checkEntities || mc.world.canPlace(mc.world.getBlockState(blockPos), blockPos, ShapeContext.absent());
    }

    public static boolean canPlace(BlockPos blockPos) {
        return canPlace(blockPos, true);
    }

    public static Direction getPlaceSide(BlockPos blockPos) {
        for (Direction side : Direction.values()) {
            BlockPos neighbor = blockPos.offset(side);
            Direction side2 = side.getOpposite();

            BlockState state = mc.world.getBlockState(neighbor);

            // Check if neighbour isn't empty
            if (state.isAir() || isClickable(state.getBlock())) continue;

            // Check if neighbour is a fluid
            if (!state.getFluidState().isEmpty()) continue;

            return side2;
        }

        return null;
    }

    private static Direction getPlaceSide2(BlockPos blockPos) {
        for (Direction side : Direction.values()) {
            BlockPos blockPos1 = blockPos.offset(side);
            Direction side1 = side.getOpposite();
            BlockState blockState = mc.world.getBlockState(blockPos1);
            if (blockState.isAir() || isClickable(blockState.getBlock()) || !blockState.getFluidState().isEmpty()) continue;

            return side1;
        }

        return null;
    }

    // Breaking

    @EventHandler(priority = EventPriority.HIGHEST + 100)
    private static void onTickPre(TickEvent.Pre event) {
        breakingThisTick = false;
    }

    @EventHandler(priority = EventPriority.LOWEST - 100)
    private static void onTickPost(TickEvent.Post event) {
        if (!breakingThisTick && breaking) {
            breaking = false;
            if (mc.interactionManager != null) mc.interactionManager.cancelBlockBreaking();
        }
    }

    public static boolean breakBlock(BlockPos blockPos, boolean swing) {
        if (!canBreak(blockPos, mc.world.getBlockState(blockPos))) return false;

        // Creating new instance of block pos because minecraft assigns the parameter to a field and we don't want it to change when it has been stored in a field somewhere
        BlockPos pos = blockPos instanceof BlockPos.Mutable ? new BlockPos(blockPos) : blockPos;

        if (mc.interactionManager.isBreakingBlock()) mc.interactionManager.updateBlockBreakingProgress(pos, Direction.UP);
        else mc.interactionManager.attackBlock(pos, Direction.UP);

        if (swing) mc.player.swingHand(Hand.MAIN_HAND);
        else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

        breaking = true;
        breakingThisTick = true;

        return true;
    }

    public static boolean canBreak(BlockPos blockPos, BlockState state) {
        if (!mc.player.isCreative() && state.getHardness(mc.world, blockPos) < 0) return false;
        return state.getOutlineShape(mc.world, blockPos) != VoxelShapes.empty();
    }

    public static boolean canBreak(BlockPos blockPos) {
        return canBreak(blockPos, mc.world.getBlockState(blockPos));
    }

    public static boolean canInstaBreak(BlockPos blockPos, BlockState state) {
        return mc.player.isCreative() || state.calcBlockBreakingDelta(mc.player, mc.world, blockPos) >= 1;
    }

    public static boolean canInstaBreak(BlockPos blockPos) {
        return canInstaBreak(blockPos, mc.world.getBlockState(blockPos));
    }

    // Other

    public static boolean isClickable(Block block) {
        return block instanceof CraftingTableBlock || block instanceof AnvilBlock || block instanceof AbstractButtonBlock || block instanceof AbstractPressurePlateBlock || block instanceof BlockWithEntity || block instanceof BedBlock || block instanceof FenceGateBlock || block instanceof DoorBlock || block instanceof NoteBlock || block instanceof TrapdoorBlock;
    }

    public static MobSpawn isValidMobSpawn(BlockPos blockPos) {
        if (!(mc.world.getBlockState(blockPos).getBlock() instanceof AirBlock) ||
            mc.world.getBlockState(blockPos.down()).getBlock() == Blocks.BEDROCK) return MobSpawn.Never;

        if (!topSurface(mc.world.getBlockState(blockPos.down()))) {
            if (mc.world.getBlockState(blockPos.down()).getCollisionShape(mc.world, blockPos.down()) != VoxelShapes.fullCube()) return MobSpawn.Never;
            if (mc.world.getBlockState(blockPos.down()).isTranslucent(mc.world, blockPos.down())) return MobSpawn.Never;
        }

        if (mc.world.getLightLevel(blockPos, 0) <= 7) return MobSpawn.Potential;
        else if (mc.world.getLightLevel(LightType.BLOCK, blockPos) <= 7) return MobSpawn.Always;

        return MobSpawn.Never;
    }

    public static boolean topSurface(BlockState blockState) {
        if (blockState.getBlock() instanceof SlabBlock && blockState.get(SlabBlock.TYPE) == SlabType.TOP) return true;
        else return blockState.getBlock() instanceof StairsBlock && blockState.get(StairsBlock.HALF) == BlockHalf.TOP;
    }

    // Util

    public static double distanceBetween(BlockPos pos1, BlockPos pos2) {
        double d = pos1.getX() - pos2.getX();
        double e = pos1.getY() - pos2.getY();
        double f = pos1.getZ() - pos2.getZ();
        return MathHelper.sqrt((float) (d * d + e * e + f * f));
    }

    public enum MobSpawn {
        Never,
        Potential,
        Always
    }
}
