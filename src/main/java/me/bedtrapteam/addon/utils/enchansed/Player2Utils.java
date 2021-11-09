package me.bedtrapteam.addon.utils.enchansed;

import baritone.api.BaritoneAPI;
import baritone.api.utils.Rotation;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.NoFall;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.misc.BaritoneUtils;
import meteordevelopment.meteorclient.utils.player.DamageUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.Dimension;
import net.minecraft.block.*;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Player2Utils {
    private static final Vec3d hitPos = new Vec3d(0.0, 0.0, 0.0);
    private static final double diagonal = 1 / Math.sqrt(2);
    private static final Vec3d horizontalVelocity = new Vec3d(0, 0, 0);

    private static final Color color = new Color();

    public static void swingHand(boolean offhand) {
        if (offhand) mc.player.swingHand(Hand.OFF_HAND);
        else mc.player.swingHand(Hand.MAIN_HAND);
    }

    public static boolean isPlayerMoving(PlayerEntity p) {
        return p.forwardSpeed != 0 || p.sidewaysSpeed != 0;
    }

    // Place Block Main Hand

    public static boolean placeBlockMainHand(final BlockPos pos) {
        return placeBlockMainHand(pos, true);
    }

    public static boolean placeBlockMainHand(final BlockPos pos, final Boolean rotate) {
        return placeBlockMainHand(pos, rotate, true);
    }

    public static boolean placeBlockMainHand(final BlockPos pos, final Boolean rotate, final Boolean airPlace) {
        return placeBlockMainHand(pos, rotate, airPlace, false);
    }

    public static boolean placeBlockMainHand(final BlockPos pos, final Boolean rotate, final Boolean airPlace, final Boolean ignoreEntity) {
        return placeBlockMainHand(pos, rotate, airPlace, ignoreEntity, null);
    }

    public static boolean placeBlockMainHand(final BlockPos pos, final Boolean rotate, final Boolean airPlace, final Boolean ignoreEntity, final Direction overrideSide) {
        return placeBlock2(Hand.MAIN_HAND, pos, rotate, airPlace, ignoreEntity, overrideSide);
    }

    // Place Block

    public static boolean placeBlock(BlockPos blockPos, Hand hand, boolean bl) {
        return Player2Utils.placeBlock(blockPos, hand, true, bl);
    }

    public static boolean placeBlock(BlockPos blockPos, int n, Hand hand, boolean bl) {
        if (n == -1) return false;
        int n2 = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = n;
        boolean bl2 = placeBlock(blockPos, hand, true, bl);
        mc.player.getInventory().selectedSlot = n2;
        return bl2;
    }

    public static boolean placeBlock(BlockPos blockPos, Hand hand, boolean bl, boolean bl2) {
        if (!Block2Utils.can_place(blockPos, true)) return false;

        for (Direction direction : Direction.values()) {
            BlockPos blockPos1 = blockPos.offset(direction);
            Direction direction1 = direction.getOpposite();
            if (mc.world.getBlockState(blockPos1).isAir() || BlockUtils.isClickable(mc.world.getBlockState(blockPos1).getBlock())) continue;
            ((IVec3d)hitPos).set(blockPos1.getX() + 0.5 + direction1.getVector().getX() * 0.5, blockPos1.getY() + 0.5 + direction1.getVector().getY() * 0.5, blockPos1.getZ() + 0.5 + direction1.getVector().getZ() * 0.5);
            boolean bl3 = mc.player.input.sneaking;
            mc.player.input.sneaking = false;
            mc.interactionManager.interactBlock(mc.player, mc.world, hand, new BlockHitResult(hitPos, direction1, blockPos1, false));
            if (bl) mc.player.swingHand(hand);
            mc.player.input.sneaking = bl3;
            return true;
        }
        if (!bl2) return false;

        ((IVec3d)hitPos).set(blockPos);
        mc.interactionManager.interactBlock(mc.player, mc.world, hand, new BlockHitResult(hitPos, Direction.UP, blockPos, false));
        if (bl) mc.player.swingHand(hand);
        return true;
    }

    public static boolean placeBlock2(final Hand hand, final BlockPos pos) {
        placeBlock2(hand, pos, true, false);
        return true;
    }

    public static boolean placeBlock2(final Hand hand, final BlockPos pos, final Boolean rotate) {
        placeBlock2(hand, pos, rotate, false);
        return true;
    }

    public static boolean placeBlock2(final Hand hand, final BlockPos pos, final Boolean rotate, final Boolean airPlace) {
        placeBlock2(hand, pos, rotate, airPlace, false);
        return true;
    }

    public static boolean placeBlock2(final Hand hand, final BlockPos pos, final Boolean rotate, final Boolean airPlace, final Boolean ignoreEntity) {
        placeBlock2(hand, pos, rotate, airPlace, ignoreEntity, null);
        return true;
    }

    public static boolean placeBlock2(final Hand hand, final BlockPos pos, final Boolean rotate, final Boolean airPlace, final Boolean ignoreEntity, final Direction overrideSide) {
        if (ignoreEntity && !mc.world.getBlockState(pos).getMaterial().isReplaceable()) return false;
        else if (!mc.world.getBlockState(pos).getMaterial().isReplaceable() || !mc.world.canPlace(Blocks.OBSIDIAN.getDefaultState(), pos, ShapeContext.absent())) return false;

        final Vec3d eyesPos = new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());
        Vec3d hitVec = null;
        BlockPos neighbor = null;
        Direction side2 = null;
        if (overrideSide != null) {
            neighbor = pos.offset(overrideSide.getOpposite());
            side2 = overrideSide;
        }

        final Direction[] values = Direction.values();
        final int length = values.length;
        int i = 0;
        while (i < length) {
            final Direction side3 = values[i];
            if (overrideSide == null) {
                neighbor = pos.offset(side3);
                side2 = side3.getOpposite();
                if (mc.world.getBlockState(neighbor).isAir() || mc.world.getBlockState(neighbor).getBlock() instanceof FluidBlock) {
                    neighbor = null;
                    side2 = null;
                    ++i;
                    continue;
                }
            }

            hitVec = new Vec3d(neighbor.getX(), neighbor.getY(), neighbor.getZ()).add(0.5, 0.5, 0.5).add(new Vec3d(side2.getUnitVector()).multiply(0.5));
            break;
        }

        if (airPlace) {
            if (hitVec == null) hitVec = new Vec3d(pos.getX(), pos.getY(), pos.getZ());
            if (neighbor == null) neighbor = pos;
            if (side2 == null) side2 = Direction.UP;
        }

        else if (hitVec == null || neighbor == null || side2 == null) return false;
        final double diffX = hitVec.x - eyesPos.x;
        final double diffY = hitVec.y - eyesPos.y;
        final double diffZ = hitVec.z - eyesPos.z;
        final double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
        final float yaw = (float)Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0f;
        final float pitch = (float)(-Math.toDegrees(Math.atan2(diffY, diffXZ)));
        final float[] rotations = { mc.player.getYaw() + MathHelper.wrapDegrees(yaw - mc.player.getYaw()), mc.player.getPitch() + MathHelper.wrapDegrees(pitch - mc.player.getPitch()) };
        if (rotate) mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(rotations[0], rotations[1], mc.player.isOnGround()));
        mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
        mc.interactionManager.interactBlock(mc.player, mc.world, hand, new BlockHitResult(hitVec, side2, neighbor, false));
        mc.player.swingHand(hand);
        mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
        return true;
    }

    public static Color getPlayerColor(PlayerEntity entity, Color defaultColor) {
        if (Friends.get().isFriend(entity)) return color.set(Friends.get().color).a(defaultColor.a);
        return defaultColor;
    }

    public static Vec3d getHorizontalVelocity(double bps) {
        float yaw = mc.player.getYaw();

        if (BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing()) {
            Rotation target = BaritoneUtils.getTarget();
            if (target != null) yaw = target.getYaw();
        }

        Vec3d forward = Vec3d.fromPolar(0, yaw);
        Vec3d right = Vec3d.fromPolar(0, yaw + 90);
        double velX = 0;
        double velZ = 0;

        boolean a = false;
        if (mc.player.input.pressingForward) {
            velX += forward.x / 20 * bps;
            velZ += forward.z / 20 * bps;
            a = true;
        }
        if (mc.player.input.pressingBack) {
            velX -= forward.x / 20 * bps;
            velZ -= forward.z / 20 * bps;
            a = true;
        }

        boolean b = false;
        if (mc.player.input.pressingRight) {
            velX += right.x / 20 * bps;
            velZ += right.z / 20 * bps;
            b = true;
        }
        if (mc.player.input.pressingLeft) {
            velX -= right.x / 20 * bps;
            velZ -= right.z / 20 * bps;
            b = true;
        }

        if (a && b) {
            velX *= diagonal;
            velZ *= diagonal;
        }

        ((IVec3d) horizontalVelocity).setXZ(velX, velZ);
        return horizontalVelocity;
    }

    public static void centerPlayer() {
        double x = MathHelper.floor(mc.player.getX()) + 0.5;
        double z = MathHelper.floor(mc.player.getZ()) + 0.5;
        mc.player.setPosition(x, mc.player.getY(), z);
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.isOnGround()));
    }

    public static void centerPlayer(BlockPos lastPos) {
        double xPos = mc.player.getPos().x;
        double zPos = mc.player.getPos().z;

        if (Math.abs(lastPos.getX() + 0.5 - mc.player.getPos().x) >= 0.2) {
            int xDir = (lastPos.getX() + 0.5 - mc.player.getPos().x > 0.0) ? 1 : -1;
            xPos += 0.3 * xDir;
        }

        if (Math.abs(lastPos.getZ() + 0.5 - mc.player.getPos().z) >= 0.2) {
            int zDir = (lastPos.getZ() + 0.5 - mc.player.getPos().z > 0.0) ? 1 : -1;
            zPos += 0.3 * zDir;
        }

        mc.player.setVelocity(0.0, 0.0, 0.0);
        mc.player.setPosition(xPos, mc.player.getY(), zPos);
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.isOnGround()));
    }

    public static boolean canSeeEntity(Entity entity) {
        Vec3d vec1 = new Vec3d(0, 0, 0);
        Vec3d vec2 = new Vec3d(0, 0, 0);

        ((IVec3d) vec1).set(mc.player.getX(), mc.player.getY() + mc.player.getStandingEyeHeight(), mc.player.getZ());
        ((IVec3d) vec2).set(entity.getX(), entity.getY(), entity.getZ());
        boolean canSeeFeet = mc.world.raycast(new RaycastContext(vec1, vec2, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player)).getType() == HitResult.Type.MISS;

        ((IVec3d) vec2).set(entity.getX(), entity.getY() + entity.getStandingEyeHeight(), entity.getZ());
        boolean canSeeEyes = mc.world.raycast(new RaycastContext(vec1, vec2, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player)).getType() == HitResult.Type.MISS;

        return canSeeFeet || canSeeEyes;
    }

    public static float[] calculateAngle(Vec3d target) {
        Vec3d eyesPos = new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());

        double dX = target.x - eyesPos.x;
        double dY = (target.y - eyesPos.y) * -1.0D;
        double dZ = target.z - eyesPos.z;

        double dist = Math.sqrt(dX * dX + dZ * dZ);

        return new float[]{(float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(dZ, dX)) - 90.0D), (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(dY, dist)))};
    }

    public static float[] calculateYaw(Vec3d target) {
        Vec3d eyesPos = new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());

        double dX = target.x - eyesPos.x;
        double dY = (target.y - eyesPos.y) * -1.0D;
        double dZ = target.z - eyesPos.z;

        double dist = Math.sqrt(dX * dX + dZ * dZ);

        return new float[]{(float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(dZ, dX)) - 90.0D), (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(dY, dist)))};
    }

    public static float[] calculatePitch(Vec3d target) {
        Vec3d eyesPos = new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());

        double dX = target.x - eyesPos.x;
        double dY = (target.y - eyesPos.y) * -1.0D;
        double dZ = target.z - eyesPos.z;

        double dist = Math.sqrt(dX * dX + dZ * dZ);

        return new float[]{(float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(dZ, dX)) - 90.0D), (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(dY, dist)))};
    }

    public static boolean shouldPause(boolean ifBreaking, boolean ifEating, boolean ifDrinking) {
        if (ifBreaking && mc.interactionManager.isBreakingBlock()) return true;
        if (ifEating && (mc.player.isUsingItem() && (mc.player.getMainHandStack().getItem().isFood() || mc.player.getOffHandStack().getItem().isFood()))) return true;
        return ifDrinking && (mc.player.isUsingItem() && (mc.player.getMainHandStack().getItem() instanceof PotionItem || mc.player.getOffHandStack().getItem() instanceof PotionItem));
    }

    public static boolean isMoving() {
        return mc.player.forwardSpeed != 0 || mc.player.sidewaysSpeed != 0;
    }

    public static boolean isSprinting() {
        return mc.player.isSprinting() && (mc.player.forwardSpeed != 0 || mc.player.sidewaysSpeed != 0);
    }

    public static ArrayList<Vec3d> selfTrapPositions = new ArrayList<Vec3d>() {{
        add(new Vec3d(1, 1, 0));
        add(new Vec3d(-1, 1, 0));
        add(new Vec3d(0, 1, 1));
        add(new Vec3d(0, 1, -1));
    }};

    public static BlockPos getSelfTrapBlock(PlayerEntity p, Boolean escapePrevention) {
        BlockPos tpos = p.getBlockPos();
        List<BlockPos> selfTrapBlocks = new ArrayList<>();
        if (!escapePrevention && Block2Utils.isTrapBlock(tpos.up(2))) return tpos.up(2);
        for (Vec3d stp : selfTrapPositions) {
            BlockPos stb = tpos.add(stp.x, stp.y, stp.z);
            if (Block2Utils.isTrapBlock(stb)) selfTrapBlocks.add(stb);
        }
        if (selfTrapBlocks.isEmpty()) return null;
        return selfTrapBlocks.get(new Random().nextInt(selfTrapBlocks.size()));
    }

    public static boolean isInHole(boolean doubles) {
        if (!meteordevelopment.meteorclient.utils.Utils.canUpdate()) return false;

        BlockPos blockPos = mc.player.getBlockPos();
        int air = 0;

        for (Direction direction : Direction.values()) {
            if (direction == Direction.UP) continue;

            BlockState state = mc.world.getBlockState(blockPos.offset(direction));

            if (state.getBlock() != Blocks.BEDROCK && state.getBlock() != Blocks.OBSIDIAN) {
                if (!doubles || direction == Direction.DOWN) return false;

                air++;

                for (Direction dir : Direction.values()) {
                    if (dir == direction.getOpposite() || dir == Direction.UP) continue;

                    BlockState blockState1 = mc.world.getBlockState(blockPos.offset(direction).offset(dir));

                    if (blockState1.getBlock() != Blocks.BEDROCK && blockState1.getBlock() != Blocks.OBSIDIAN) return false;
                }
            }
        }

        return air < 2;
    }

    public static boolean isInHole2(PlayerEntity p) {
        BlockPos pos = p.getBlockPos();
        return !mc.world.getBlockState(pos.add(1, 0, 0)).isAir() && !mc.world.getBlockState(pos.add(-1, 0, 0)).isAir() && !mc.world.getBlockState(pos.add(0, 0, 1)).isAir() && !mc.world.getBlockState(pos.add(0, 0, -1)).isAir() && !mc.world.getBlockState(pos.add(0, -1, 0)).isAir();
    }

    public static boolean isBurrowed(PlayerEntity p, boolean holeCheck) {
        BlockPos pos = p.getBlockPos();
        if (holeCheck && !isInHole2(p)) return false;
        return Block2Utils.getBlock(pos) == Blocks.ENDER_CHEST || Block2Utils.getBlock(pos) == Blocks.OBSIDIAN || Block2Utils.isAnvilBlock(pos);
    }

    public static boolean isWebbed(PlayerEntity player) {
        BlockPos pos = player.getBlockPos();
        if (Block2Utils.isWeb(pos)) return true;
        return Block2Utils.isWeb(pos.up());
    }

    public static void mineWeb(PlayerEntity p, int swordSlot) {
        if (p == null || swordSlot == -1) return;
        BlockPos pos = p.getBlockPos();
        BlockPos webPos = null;
        if (Block2Utils.isWeb(pos)) webPos = pos;
        if (Block2Utils.isWeb(pos.up())) webPos = pos.up();
        if (Block2Utils.isWeb(pos.up(2))) webPos = pos.up(2);
        if (webPos == null) return;
        Inv2Utils.updateSlot(swordSlot);
        doRegularMine(webPos);
    }

    public static void doPacketMine(BlockPos targetPos) {
        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, targetPos, Direction.UP));
        swingHand(false);
        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, targetPos, Direction.UP));
    }

    public static void doRegularMine(BlockPos targetPos) {
        mc.interactionManager.updateBlockBreakingProgress(targetPos, Direction.UP);
        Vec3d hitPos = new Vec3d(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5);
        Rotations.rotate(Rotations.getYaw(hitPos), Rotations.getPitch(hitPos), 50, () -> swingHand(false));
    }

    public static double possibleHealthReductions() {
        return possibleHealthReductions(true, true);
    }

    public static double possibleHealthReductions(boolean entities, boolean fall) {
        double damageTaken = 0;

        if (entities) {
            for (Entity entity : mc.world.getEntities()) {
                // Check for end crystals
                if (entity instanceof EndCrystalEntity && damageTaken < DamageUtils.crystalDamage(mc.player, entity.getPos())) damageTaken = DamageUtils.crystalDamage(mc.player, entity.getPos());

                    // Check for players holding swords
                else if (entity instanceof PlayerEntity && damageTaken < DamageUtils.getSwordDamage((PlayerEntity) entity, true)) {
                    if (!Friends.get().isFriend((PlayerEntity) entity) && mc.player.getPos().distanceTo(entity.getPos()) < 5) {
                        if (((PlayerEntity) entity).getActiveItem().getItem() instanceof SwordItem) damageTaken = DamageUtils.getSwordDamage((PlayerEntity) entity, true);
                    }
                }
            }

            // Check for beds if in nether
            if (Player2Utils.getDimension() != Dimension.Overworld) {
                for (BlockEntity blockEntity : Utils.blockEntities()) {
                    BlockPos bp = blockEntity.getPos();
                    Vec3d pos = new Vec3d(bp.getX(), bp.getY(), bp.getZ());

                    if (blockEntity instanceof BedBlockEntity && damageTaken < DamageUtils.bedDamage(mc.player, pos)) damageTaken = DamageUtils.bedDamage(mc.player, pos);
                }
            }
        }

        // Check for fall distance with water check
        if (fall) {
            if (!Modules.get().isActive(NoFall.class) && mc.player.fallDistance > 3) {
                double damage = mc.player.fallDistance * 0.5;

                if (damage > damageTaken && !EntityUtils.isAboveWater(mc.player)) damageTaken = damage;
            }
        }

        return damageTaken;
    }

    public static double distanceTo(Entity entity) {
        return distanceTo(entity.getX(), entity.getY(), entity.getZ());
    }

    public static double distanceTo(BlockPos blockPos) {
        return distanceTo(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    public static double distanceTo(Vec3d vec3d) {
        return distanceTo(vec3d.getX(), vec3d.getY(), vec3d.getZ());
    }

    public static double distanceTo(double x, double y, double z) {
        if (mc.player == null) return 0;
        float f = (float) (mc.player.getX() - x);
        float g = (float) (mc.player.getY() - y);
        float h = (float) (mc.player.getZ() - z);
        return MathHelper.sqrt(f * f + g * g + h * h);
    }

    public static double distanceToCamera(double x, double y, double z) {
        Camera camera = mc.gameRenderer.getCamera();
        return Math.sqrt(camera.getPos().squaredDistanceTo(x, y, z));
    }

    public static double distanceToCamera(Entity entity) {
        return distanceToCamera(entity.getX(), entity.getY(), entity.getZ());
    }

    public static Dimension getDimension() {
        if (mc.world == null) return Dimension.Overworld;

        return switch (mc.world.getRegistryKey().getValue().getPath()) {
            case "the_nether" -> Dimension.Nether;
            case "the_end" -> Dimension.End;
            default -> Dimension.Overworld;
        };
    }

    public static GameMode getGameMode() {
        PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        if (playerListEntry == null) return GameMode.SPECTATOR;
        return playerListEntry.getGameMode();
    }

    public static double getTotalHealth() {
        return mc.player.getHealth() + mc.player.getAbsorptionAmount();
    }

    public static boolean isAlive() {
        return mc.player.isAlive() && !mc.player.isDead();
    }

    public static int getPing() {
        if (mc.getNetworkHandler() == null) return 0;

        PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        if (playerListEntry == null) return 0;
        return playerListEntry.getLatency();
    }

    public static BlockPos roundBlockPos(final Vec3d vec) {
        return new BlockPos(vec.x, Math.round(vec.y), vec.z);
    }

    public static boolean shouldSneakWhileClicking(Block block) {
        return block instanceof EnderChestBlock || block instanceof AnvilBlock || block instanceof AbstractButtonBlock || block instanceof AbstractPressurePlateBlock || block instanceof BlockWithEntity || block instanceof CraftingTableBlock || block instanceof DoorBlock || block instanceof FenceGateBlock || block instanceof NoteBlock || block instanceof TrapdoorBlock;
    }

    public static int countItem(Item item) {
        int count = 0;

        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);

            if (itemStack.getItem() == item) {
                count += itemStack.getCount();
            }
        }

        return count;
    }

    public static int countSets() {
        int helmets = 0;
        int chestplates = 0;
        int leggings = 0;
        int boots = 0;

        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);

            if (itemStack.getItem() instanceof ArmorItem) {
                ArmorItem item = (ArmorItem) itemStack.getItem();
                if (item.getSlotType() == EquipmentSlot.HEAD) {
                    helmets++;
                } else if (item.getSlotType() == EquipmentSlot.CHEST) {
                    chestplates++;
                } else if (item.getSlotType() == EquipmentSlot.LEGS) {
                    leggings++;
                } else if (item.getSlotType() == EquipmentSlot.FEET) {
                    boots++;
                }
            }
        }

        return Math.min(Math.min(helmets, chestplates), Math.min(leggings, boots));
    }

    public static Vec3d getDirectionalSpeed(double speed) {
        double x;
        double z;

        double forward = mc.player.input.movementForward;
        double strafe = mc.player.input.movementSideways;
        float yaw = mc.player.getYaw();

        if (forward == 0.0D && strafe == 0.0D) {
            x = 0D;
            z = 0D;
        } else {
            if (forward != 0.0D) {
                if (strafe > 0.0D) {
                    yaw += (float)(forward > 0.0D ? -45 : 45);
                } else if (strafe < 0.0D) {
                    yaw += (float)(forward > 0.0D ? 45 : -45);
                }

                strafe = 0.0D;

                if (forward > 0.0D) {
                    forward = 1.0D;
                } else if (forward < 0.0D) {
                    forward = -1.0D;
                }
            }

            x = forward * speed * Math.cos(Math.toRadians(yaw + 90.0F)) + strafe * speed * Math.sin(Math.toRadians(yaw + 90.0F));
            z = forward * speed * Math.sin(Math.toRadians(yaw + 90.0F)) - strafe * speed * Math.cos(Math.toRadians(yaw + 90.0F));
        }

        return new Vec3d(x, 0, z);
    }

    public static double getBaseMotionSpeed() {
        double baseSpeed = 0.2873D;
        if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            int amplifier = mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier();
            baseSpeed *= 1.0 + 0.2 * (amplifier + 1);
        }
        return baseSpeed;
    }

    public static boolean isPlayerMoving() {
        return mc.player.input.pressingForward || mc.player.input.pressingBack || mc.player.input.pressingRight || mc.player.input.pressingLeft;
    }

    public static boolean checkIfBlockInBB(Class<? extends Block> blockClass) {
        return checkIfBlockInBB(blockClass, (int) Math.floor(mc.player.getBoundingBox(mc.player.getPose()).minY));
    }

    public static boolean checkIfBlockInBB(Class<? extends Block> blockClass, int minY) {
        for(int iX = MathHelper.floor(mc.player.getBoundingBox(mc.player.getPose()).minX); iX < MathHelper.ceil(mc.player.getBoundingBox(mc.player.getPose()).maxX); iX++) {
            for(int iZ = MathHelper.floor(mc.player.getBoundingBox(mc.player.getPose()).minZ); iZ < MathHelper.ceil(mc.player.getBoundingBox(mc.player.getPose()).maxZ); iZ++) {
                BlockState state = mc.world.getBlockState(new BlockPos(iX, minY, iZ));
                if (state != null && blockClass.isInstance(state.getBlock())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void centerPlayerHorizontally() {
        double centerX = MathHelper.floor(mc.player.getX()) + 0.5;
        double centerZ = MathHelper.floor(mc.player.getZ()) + 0.5;
        mc.player.updatePosition(centerX, mc.player.getY(), centerZ);
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.isOnGround()));
    }

    public static double movementYaw() {
        float yaw = mc.player.getYaw();

        if (mc.player.forwardSpeed < 0f) yaw += 180f;

        float forward = 1f;

        if (mc.player.forwardSpeed < 0f) forward = -0.5f;
        else if (mc.player.forwardSpeed > 0f) forward = 0.5f;

        if (mc.player.sidewaysSpeed > 0f) yaw -= 90f * forward;
        if (mc.player.sidewaysSpeed < 0f) yaw += 90f * forward;

        return (double) yaw * 0.017453292;
    }
}
