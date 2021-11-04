package me.bedtrapteam.addon.utils.enchansed;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IExplosion;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.Dimension;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.*;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.LightType;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.EVENT_BUS;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Block2Utils {
    private static final Explosion explosion = new Explosion(null, null, 0, 0, 0, 6, false, Explosion.DestructionType.DESTROY);
    private static final Vec3d hitPos = new Vec3d(0, 0, 0);
    public static MinecraftClient mc = MinecraftClient.getInstance();
    public static Vec3i[] city = {new Vec3i(1, 0, 0), new Vec3i(-1, 0, 0), new Vec3i(0, 0, 1), new Vec3i(0, 0, -1)};
    public static final Logger LOG = LogManager.getLogger();

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

    public static ArrayList<Vec3d> getAreaAsVec3ds(final BlockPos centerPos, final double l, final double d, final double h, final boolean sphere) {
        final ArrayList<Vec3d> cuboidBlocks = new ArrayList<Vec3d>();
        for (double i = centerPos.getX() - l; i < centerPos.getX() + l; ++i) {
            for (double j = centerPos.getY() - d; j < centerPos.getY() + d; ++j) {
                for (double k = centerPos.getZ() - h; k < centerPos.getZ() + h; ++k) {
                    final Vec3d pos2 = new Vec3d(Math.floor(i), Math.floor(j), Math.floor(k));
                    cuboidBlocks.add(pos2);
                }
            }
        }
        if (sphere) {
            cuboidBlocks.removeIf(pos -> pos.distanceTo(blockPosToVec3d(centerPos)) > l);
        }
        return cuboidBlocks;
    }

    public static Vec3d blockPosToVec3d(final BlockPos blockPos) {
        return new Vec3d((double)blockPos.getX(), (double)blockPos.getY(), (double)blockPos.getZ());
    }

    public static boolean can_place(BlockPos blockPos, boolean checkEntities) {
        if (blockPos == null) return false;

        // Check y level
        if (blockPos.getY() > 256 || blockPos.getY() < 0) return false;

        // Check if current block is replaceable
        if (!mc.world.getBlockState(blockPos).getMaterial().isReplaceable()) return false;

        // Check if intersects entities
        return !checkEntities || mc.world.canPlace(Blocks.STONE.getDefaultState(), blockPos, ShapeContext.absent());
    }

    //Always Calculate damage, then armour, then enchantments, then potion effect
    public static float crystalDamage(LivingEntity player, Vec3d crystal, boolean predict, boolean ignoreTerrain) {
        if (player instanceof PlayerEntity && ((PlayerEntity) player).getAbilities().creativeMode) return 0;
        Vec3d v = predict ? player.getVelocity() : new Vec3d(0, 0, 0);
        Vec3d playerPos = player.getPos().add(v);

        // Calculate crystal damage
        float modDistance = (float) Math.sqrt(playerPos.squaredDistanceTo(crystal));
        if (modDistance > 12) return 0;

        float exposure = getExposure(crystal, player, predict, ignoreTerrain);
        float impact = (float) (1.0 - (modDistance / 12.0)) * exposure;
        float damage = (impact * impact + impact) / 2 * 7 * (6 * 2) + 1;

        // Multiply damage by difficulty
        damage = (float) getDamageForDifficulty(damage);

        // Reduce by armour
        damage = DamageUtil.getDamageLeft(damage, (float) player.getArmor(), (float) player.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS).getValue());

        // Reduce by resistance
        damage = (float) resistanceReduction(player, damage);

        // Reduce by enchants
        ((IExplosion) explosion).set(crystal, 6, false);
        damage = (float) blastProtReduction(player, damage, explosion);

        return Math.max(damage, 0F);
    }

    private static double getDamageForDifficulty(double damage) {
        return switch (mc.world.getDifficulty()) {
            case PEACEFUL -> 0;
            case EASY -> Math.min(damage / 2.0F + 1.0F, damage);
            case HARD -> damage * 3.0F / 2.0F;
            default -> damage;
        };
    }

    private static double blastProtReduction(LivingEntity player, double damage, Explosion explosion) {
        int protLevel = EnchantmentHelper.getProtectionAmount(player.getArmorItems(), DamageSource.explosion(explosion));
        if (protLevel > 20) protLevel = 20;

        damage *= 1 - (protLevel / 25.0);
        return damage < 0 ? 0 : damage;
    }

    private static double resistanceReduction(LivingEntity player, double damage) {
        if (player.hasStatusEffect(StatusEffects.RESISTANCE)) {
            int lvl = (player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() + 1);
            damage *= 1 - (lvl * 0.2);
        }

        return damage < 0 ? 0 : damage;
    }

    private static float getExposure(Vec3d source, LivingEntity entity, boolean predict, boolean ignoreTerrain) {
        Vec3d v = predict ? entity.getVelocity() : new Vec3d(0, 0, 0);
        Box box = entity.getBoundingBox().offset(v);
        double d = 1.0D / ((box.maxX - box.minX) * 2.0D + 1.0D);
        double e = 1.0D / ((box.maxY - box.minY) * 2.0D + 1.0D);
        double f = 1.0D / ((box.maxZ - box.minZ) * 2.0D + 1.0D);
        double g = (1.0D - Math.floor(1.0D / d) * d) / 2.0D;
        double h = (1.0D - Math.floor(1.0D / f) * f) / 2.0D;
        if (!(d < 0.0D) && !(e < 0.0D) && !(f < 0.0D)) {
            int i = 0;//nonsolid
            int j = 0;//total

            for (float k = 0.0F; k <= 1.0F; k = (float) ((double) k + d)) {
                for (float l = 0.0F; l <= 1.0F; l = (float) ((double) l + e)) {
                    for (float m = 0.0F; m <= 1.0F; m = (float) ((double) m + f)) {
                        double n = MathHelper.lerp(k, box.minX, box.maxX);
                        double o = MathHelper.lerp(l, box.minY, box.maxY);
                        double p = MathHelper.lerp(m, box.minZ, box.maxZ);
                        Vec3d vec3d = new Vec3d(n + g, o, p + h);
                        if (raycast(new RaycastContext(vec3d, source, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity), ignoreTerrain).getType() == HitResult.Type.MISS) ++i;
                        ++j;
                    }
                }
            }

            return (float) i / (float) j;
        } else {
            return 0.0F;
        }
    }

    private static BlockHitResult raycast(RaycastContext context, boolean ignoreTerrain) {
        return BlockView.raycast(context.getStart(), context.getEnd(), context, (raycastContext, blockPos) -> {
            BlockState blockState;
            blockState = mc.world.getBlockState(blockPos);
            if (blockState.getBlock().getBlastResistance() < 600 && ignoreTerrain) blockState = Blocks.AIR.getDefaultState();

            Vec3d vec3d = raycastContext.getStart();
            Vec3d vec3d2 = raycastContext.getEnd();

            VoxelShape voxelShape = raycastContext.getBlockShape(blockState, mc.world, blockPos);
            BlockHitResult blockHitResult = mc.world.raycastBlock(vec3d, vec3d2, blockPos, voxelShape, blockState);
            VoxelShape voxelShape2 = VoxelShapes.empty();
            BlockHitResult blockHitResult2 = voxelShape2.raycast(vec3d, vec3d2, blockPos);

            double d = blockHitResult == null ? Double.MAX_VALUE : raycastContext.getStart().squaredDistanceTo(blockHitResult.getPos());
            double e = blockHitResult2 == null ? Double.MAX_VALUE : raycastContext.getStart().squaredDistanceTo(blockHitResult2.getPos());

            return d <= e ? blockHitResult : blockHitResult2;
        }, (raycastContext) -> {
            Vec3d vec3d = raycastContext.getStart().subtract(raycastContext.getEnd());
            return BlockHitResult.createMissed(raycastContext.getEnd(), Direction.getFacing(vec3d.x, vec3d.y, vec3d.z), new BlockPos(raycastContext.getEnd()));
        });
    }

    public static boolean isSurrounded(LivingEntity target) {
        assert mc.world != null;
        return !mc.world.getBlockState(target.getBlockPos().add(1, 0, 0)).isAir()
            && !mc.world.getBlockState(target.getBlockPos().add(-1, 0, 0)).isAir()
            && !mc.world.getBlockState(target.getBlockPos().add(0, 0, 1)).isAir()
            && !mc.world.getBlockState(target.getBlockPos().add(0, 0, -1)).isAir();
    }

    public static boolean isFucked(LivingEntity target) {
        assert mc.world != null;
        assert mc.player != null;
        int count = 0;
        int count2 = 0;
        if (isBurrowed(target)) return false;

        if(isBurrowed(mc.player) && target.getBlockPos().getX() == mc.player.getBlockPos().getX() && target.getBlockPos().getZ() == mc.player.getBlockPos().getZ() && target.getBlockPos().getY() - mc.player.getBlockPos().getY() <= 2) return true;

        if (!mc.world.getBlockState(target.getBlockPos().add(0, 2, 0)).isAir()) return true;

        if (!mc.world.getBlockState(target.getBlockPos().add(1, 0, 0)).isAir()) count++;
        if (!mc.world.getBlockState(target.getBlockPos().add(-1, 0, 0)).isAir()) count++;
        if (!mc.world.getBlockState(target.getBlockPos().add(0, 0, 1)).isAir()) count++;
        if (!mc.world.getBlockState(target.getBlockPos().add(0, 0, -1)).isAir()) count++;

        if (count == 3) return true;

        if (!mc.world.getBlockState(target.getBlockPos().add(1, 1, 0)).isAir()) count2++;
        if (!mc.world.getBlockState(target.getBlockPos().add(-1, 1, 0)).isAir()) count2++;
        if (!mc.world.getBlockState(target.getBlockPos().add(0, 1, 1)).isAir()) count2++;
        if (!mc.world.getBlockState(target.getBlockPos().add(0, 1, -1)).isAir()) count2++;

        return count < 4 && count2 == 4;
    }

    public static boolean isBurrowed(LivingEntity target) {
        return !mc.world.getBlockState(target.getBlockPos()).isAir();
    }

    public static boolean obbySurrounded(LivingEntity entity) {
        BlockPos entityBlockPos = entity.getBlockPos();
        return isBlastRes(mc.world.getBlockState(entity.getBlockPos().add(1, 0, 0)).getBlock())
            && isBlastRes(mc.world.getBlockState(entityBlockPos.add(-1, 0, 0)).getBlock())
            && isBlastRes(mc.world.getBlockState(entityBlockPos.add(0, 0, 1)).getBlock())
            && isBlastRes(mc.world.getBlockState(entityBlockPos.add(0, 0, -1)).getBlock());
    }

    public static boolean isBlastRes(Block block) {
        if (block.getBlastResistance() < 600) return false;
        if (block == Blocks.RESPAWN_ANCHOR) {
            return getDimension() == Dimension.Nether;
        }
        return true;
    }

    public static Dimension getDimension() {
        return switch (MinecraftClient.getInstance().world.getRegistryKey().getValue().getPath()) {
            case "the_nether" -> Dimension.Nether;
            case "the_end" -> Dimension.End;
            default -> Dimension.Overworld;
        };
    }

    public static boolean placeBlock(BlockPos blockPos, int slot, Hand hand, boolean airPlace) {
        if (slot == -1) return false;

        int preSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = slot;

        boolean a = placeBlock(blockPos, hand, true, airPlace);

        mc.player.getInventory().selectedSlot = preSlot;
        return a;
    }

    public static boolean placeBlock(BlockPos blockPos, Hand hand, boolean swing, boolean airPlace) {
        if (!can_place(blockPos, true)) return false;

        // Try to find a neighbour to click on to avoid air place
        for (Direction side : Direction.values()) {

            BlockPos neighbor = blockPos.offset(side);
            Direction side2 = side.getOpposite();

            // Check if neighbour isn't empty
            if (mc.world.getBlockState(neighbor).isAir() || BlockUtils.isClickable(mc.world.getBlockState(neighbor).getBlock())) continue;

            // Calculate hit pos
            ((IVec3d) hitPos).set(neighbor.getX() + 0.5 + side2.getVector().getX() * 0.5, neighbor.getY() + 0.5 + side2.getVector().getY() * 0.5, neighbor.getZ() + 0.5 + side2.getVector().getZ() * 0.5);

            // Place block
            boolean wasSneaking = mc.player.input.sneaking;
            mc.player.input.sneaking = false;

            mc.interactionManager.interactBlock(mc.player, mc.world, hand, new BlockHitResult(hitPos, side2, neighbor, false));
            if (swing) mc.player.swingHand(hand);

            mc.player.input.sneaking = wasSneaking;
            return true;
        }

        if (!airPlace) return false;
        // Air place if no neighbour was found
        ((IVec3d) hitPos).set(blockPos);

        mc.interactionManager.interactBlock(mc.player, mc.world, hand, new BlockHitResult(hitPos, Direction.UP, blockPos, false));
        if (swing) mc.player.swingHand(hand);

        return true;
    }

    public static int getSurroundBreak(LivingEntity target, BlockPos pos) {
        BlockPos targetBlockPos = target.getBlockPos();
        if (!mc.world.getBlockState(targetBlockPos.add(1, 0, 0)).isOf(Blocks.BEDROCK) && mc.player.getPos().distanceTo(Vec3d.ofCenter(targetBlockPos.add(1, 0, 0))) <= 6) {
            if (targetBlockPos.add(2, -1, 0).equals(pos)) return 5;
            if (targetBlockPos.add(2, -1, 1).equals(pos)) return 4;
            if (targetBlockPos.add(2, -1, -1).equals(pos)) return 4;
            if (targetBlockPos.add(2, -2, 0).equals(pos)) return 3;
            if (targetBlockPos.add(2, -2, 1).equals(pos)) return 2;
            if (targetBlockPos.add(2, -2, -1).equals(pos)) return 2;
            if (targetBlockPos.add(1, -2, 0).equals(pos)) return 1;
            if (targetBlockPos.add(1, -2, 1).equals(pos)) return 1;
            if (targetBlockPos.add(1, -2, -1).equals(pos)) return 1;
            if (targetBlockPos.add(1, -1, 1).equals(pos)) return 1;
            if (targetBlockPos.add(1, -1, -1).equals(pos)) return 1;
        }
        if (!mc.world.getBlockState(targetBlockPos.add(-1, 0, 0)).isOf(Blocks.BEDROCK) && mc.player.getPos().distanceTo(Vec3d.ofCenter(targetBlockPos.add(-1, 0, 0))) <= 6) {
            if (targetBlockPos.add(-2, -1, 0).equals(pos)) return 5;
            if (targetBlockPos.add(-2, -1, 1).equals(pos)) return 4;
            if (targetBlockPos.add(-2, -1, -1).equals(pos)) return 4;
            if (targetBlockPos.add(-2, -2, 0).equals(pos)) return 3;
            if (targetBlockPos.add(-2, -2, 1).equals(pos)) return 2;
            if (targetBlockPos.add(-2, -2, -1).equals(pos)) return 2;
            if (targetBlockPos.add(-1, -2, 0).equals(pos)) return 1;
            if (targetBlockPos.add(-1, -2, 1).equals(pos)) return 1;
            if (targetBlockPos.add(-1, -2, -1).equals(pos)) return 1;
            if (targetBlockPos.add(-1, -1, 1).equals(pos)) return 1;
            if (targetBlockPos.add(-1, -1, -1).equals(pos)) return 1;
        }
        if (!mc.world.getBlockState(targetBlockPos.add(0, 0, 1)).isOf(Blocks.BEDROCK) && mc.player.getPos().distanceTo(Vec3d.ofCenter(targetBlockPos.add(0, 0, 1))) <= 6) {
            if (targetBlockPos.add(0, -1, 2).equals(pos)) return 5;
            if (targetBlockPos.add(1, -1, 2).equals(pos)) return 4;
            if (targetBlockPos.add(-1, -1, 2).equals(pos)) return 4;
            if (targetBlockPos.add(0, -2, 2).equals(pos)) return 3;
            if (targetBlockPos.add(1, -2, 2).equals(pos)) return 2;
            if (targetBlockPos.add(-1, -2, 2).equals(pos)) return 2;
            if (targetBlockPos.add(0, -2, 1).equals(pos)) return 1;
            if (targetBlockPos.add(1, -2, 1).equals(pos)) return 1;
            if (targetBlockPos.add(-1, -2, 1).equals(pos)) return 1;
            if (targetBlockPos.add(1, -1, 1).equals(pos)) return 1;
            if (targetBlockPos.add(-1, -1, 1).equals(pos)) return 1;
        }
        if (!mc.world.getBlockState(targetBlockPos.add(0, 0, -1)).isOf(Blocks.BEDROCK) && mc.player.getPos().distanceTo(Vec3d.ofCenter(targetBlockPos.add(0, 0, -1))) <= 6) {
            if (targetBlockPos.add(0, -1, -2).equals(pos)) return 5;
            if (targetBlockPos.add(1, -1, -2).equals(pos)) return 4;
            if (targetBlockPos.add(-1, -1, -2).equals(pos)) return 4;
            if (targetBlockPos.add(0, -2, -2).equals(pos)) return 3;
            if (targetBlockPos.add(1, -2, -2).equals(pos)) return 2;
            if (targetBlockPos.add(-1, -2, -2).equals(pos)) return 2;
            if (targetBlockPos.add(0, -2, -1).equals(pos)) return 1;
            if (targetBlockPos.add(1, -2, -1).equals(pos)) return 1;
            if (targetBlockPos.add(-1, -2, -1).equals(pos)) return 1;
            if (targetBlockPos.add(1, -1, -1).equals(pos)) return 1;
            if (targetBlockPos.add(-1, -1, -1).equals(pos)) return 1;
        }
        return 0;
    }

    public static boolean isSurroundBroken(LivingEntity target) {
        BlockPos targetBlockPos = target.getBlockPos();
        for (Vec3i block : city) {
            double x = targetBlockPos.add(block).getX();
            double y = targetBlockPos.add(block).getY();
            double z = targetBlockPos.add(block).getZ();
            if (mc.world.getBlockState(targetBlockPos.add(block)).isOf(Blocks.BEDROCK)) continue;
            if (mc.player.getPos().distanceTo(new Vec3d(x, y, z)) > 6) continue;
            if (!mc.world.getOtherEntities(null, new Box(x, y, z, x + 1, y + 1, z + 1)).isEmpty()) return true;
        }
        return false;
    }

    public static Vec3d crystalEdgePos(EndCrystalEntity crystal) {
        Vec3d crystalPos = crystal.getPos();
        //X
        if (crystalPos.x < mc.player.getX()) crystalPos.add(Math.min(1, mc.player.getX() - crystalPos.x), 0, 0);
        else if (crystalPos.x > mc.player.getX()) crystalPos.add(Math.max(-1, mc.player.getX() - crystalPos.x), 0, 0);
        //Y
        if (crystalPos.y < mc.player.getY()) crystalPos.add(0, Math.min(2, mc.player.getY() - crystalPos.y), 0);
        //Z
        if (crystalPos.z < mc.player.getZ()) crystalPos.add(0, 0, Math.min(1, mc.player.getZ() - crystalPos.z));
        else if (crystalPos.z > mc.player.getZ()) crystalPos.add(0, 0, Math.max(-1, mc.player.getZ() - crystalPos.z));

        return crystalPos;
    }

    public static BlockHitResult getPlaceResult(BlockPos pos) {
        Vec3d eyesPos = new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());
        for (Direction direction : Direction.values()) {
            RaycastContext raycastContext = new RaycastContext(eyesPos, new Vec3d(
                pos.getX() + 0.5 + direction.getVector().getX() * 0.5,
                pos.getY() + 0.5 + direction.getVector().getY() * 0.5,
                pos.getZ() + 0.5 + direction.getVector().getZ() * 0.5),
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
            BlockHitResult result = mc.world.raycast(raycastContext);
            if (result != null && result.getType() == HitResult.Type.BLOCK && result.getBlockPos().equals(pos)) {
                return result;
            }
        }
        return new BlockHitResult(eyesPos, pos.getY() < mc.player.getY() ? Direction.UP : Direction.DOWN, new BlockPos(pos), false);
    }

    public static boolean obbyDoubleSurrounded(LivingEntity entity) {
        BlockPos entityBlockPos = entity.getBlockPos();
        return isBlastRes(mc.world.getBlockState(entity.getBlockPos().add(1, 0, 0)).getBlock()) &&
            isBlastRes(mc.world.getBlockState(entityBlockPos.add(-1, 0, 0)).getBlock()) &&
            isBlastRes(mc.world.getBlockState(entityBlockPos.add(0, 0, 1)).getBlock()) &&
            isBlastRes(mc.world.getBlockState(entityBlockPos.add(0, 0, -1)).getBlock()) &&
            isBlastRes(mc.world.getBlockState(entity.getBlockPos().add(1, 1, 0)).getBlock()) &&
            isBlastRes(mc.world.getBlockState(entityBlockPos.add(-1, 1, 0)).getBlock()) &&
            isBlastRes(mc.world.getBlockState(entityBlockPos.add(0, 1, 1)).getBlock()) &&
            isBlastRes(mc.world.getBlockState(entityBlockPos.add(0, 1, -1)).getBlock());
    }

    public static boolean isRetard(LivingEntity entity) {
        BlockPos entityBlockPos = entity.getBlockPos();
        return isBlastRes(mc.world.getBlockState(entity.getBlockPos().add(1, 0, 0)).getBlock()) &&
            isBlastRes(mc.world.getBlockState(entityBlockPos.add(-1, 0, 0)).getBlock()) &&
            isBlastRes(mc.world.getBlockState(entityBlockPos.add(0, 0, 1)).getBlock()) &&
            isBlastRes(mc.world.getBlockState(entityBlockPos.add(0, 0, -1)).getBlock()) &&
            isBlastRes(mc.world.getBlockState(entity.getBlockPos().add(1, 1, 0)).getBlock()) &&
            isBlastRes(mc.world.getBlockState(entityBlockPos.add(-1, 1, 0)).getBlock()) &&
            isBlastRes(mc.world.getBlockState(entityBlockPos.add(0, 1, 1)).getBlock()) &&
            isBlastRes(mc.world.getBlockState(entityBlockPos.add(0, 1, -1)).getBlock()) &&
            isBlastRes(mc.world.getBlockState(entityBlockPos.add(0, -1, 0)).getBlock()) &&
            isBlastRes(mc.world.getBlockState(entityBlockPos.add(0, 2, 0)).getBlock());
    }

    public enum MobSpawn {
        Never,
        Potential,
        Always
    }
}
