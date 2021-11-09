package me.bedtrapteam.addon.modules.konas;

import me.bedtrapteam.addon.Atlas;
import me.bedtrapteam.addon.utils.*;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.AirBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Material;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class AntiSurround extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").defaultValue(false).build());
    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder().name("swing").defaultValue(true).build());
    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder().name("range").defaultValue(4).min(0).sliderMax(6).build());
    private final Setting<Double> delay = sgGeneral.add(new DoubleSetting.Builder().name("delay").defaultValue(2).min(0).sliderMax(10).build());
    private final Setting<Boolean> strictDirection = sgGeneral.add(new BoolSetting.Builder().name("strict-direction").defaultValue(false).build());
    private final Setting<SwapMode> swap = sgGeneral.add(new EnumSetting.Builder<SwapMode>().name("anti-friend-pop").defaultValue(SwapMode.Normal).build());
    private final Setting<Boolean> instant = sgGeneral.add(new BoolSetting.Builder().name("instant").defaultValue(false).build());
    private final Setting<Boolean> limit = sgGeneral.add(new BoolSetting.Builder().name("limit").defaultValue(false).visible(instant::get).build());

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").defaultValue(false).build());
    private final Setting<Boolean> showMining = sgRender.add(new BoolSetting.Builder().name("show-mining").defaultValue(false).visible(render::get).build());
    private final Setting<SettingColor> miningColor = sgRender.add(new ColorSetting.Builder().name("mining").defaultValue(new SettingColor(255, 0, 0, 75, true)).visible(render::get).build());
    private final Setting<SettingColor> miningLineColor = sgRender.add(new ColorSetting.Builder().name("mining-outline").defaultValue(new SettingColor(255, 0, 0, 75, true)).visible(render::get).build());
    private final Setting<SettingColor> readyColor = sgRender.add(new ColorSetting.Builder().name("ready").defaultValue(new SettingColor(255, 0, 0, 75, true)).visible(render::get).build());
    private final Setting<SettingColor> readyLineColor = sgRender.add(new ColorSetting.Builder().name("ready-outline").defaultValue(new SettingColor(255, 0, 0, 75, true)).visible(render::get).build());

    public enum SwapMode {
        Off,
        Normal,
        Silent
    }

    public AntiSurround() {
        super(Atlas.Konas, "anti-surround", "Mines enemies surrounds");
    }

    private Timer silentTimer = new Timer();

    private BlockPos prevPos;

    int i = 0;
    private BlockPos currentPos;
    private Direction currentFacing;

    private float curBlockDamage;
    private Timer mineTimer = new Timer();
    private boolean stopped;

    private Timer delayTimer = new Timer();

    private int priorSlot = -1;

    @Override
    public void onActivate() {
        Checker.Check();

        prevPos = null;
        currentPos = null;
        currentFacing = null;
        curBlockDamage = 0F;
        stopped = false;
        priorSlot = -1;

        i = 0;
    }

    @EventHandler
    public void onPlayerUpdate(TickEvent.Pre event) {
        if (i == 0) {
            Timer.Check();
            i++;
        }
        if (currentPos != null) {
            if (curBlockDamage >= 1F) {
                if (stopped) {
                    if (mineTimer.hasPassed(1500)) {
                        currentPos = null;
                        currentFacing = null;
                    }
                } else {
                    stopped = true;
                    if (swap.get() != SwapMode.Off) {
                        int bestSlot = findBestTool(currentPos);
                        if (bestSlot != -1 && bestSlot != mc.player.getInventory().selectedSlot) {
                            if (swap.get() == SwapMode.Silent) {
                                priorSlot = mc.player.getInventory().selectedSlot;
                                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(bestSlot));
                                silentTimer.reset();
                            } else {
                                mc.player.getInventory().selectedSlot = bestSlot;
                                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(bestSlot));
                            }
                        }
                    }
                }
            }
        }
        if (currentPos != null && curBlockDamage < 1F) {
            BlockState iblockstate = mc.world.getBlockState(currentPos);

            if (iblockstate.getMaterial() == Material.AIR) {
                prevPos = currentPos;
                currentPos = null;
                return;
            }

            int bestSlot = findBestTool(currentPos);
            if (bestSlot == -1) bestSlot = mc.player.getInventory().selectedSlot;
            int prevItem = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = bestSlot;
            curBlockDamage += iblockstate.calcBlockBreakingDelta(mc.player, mc.player.world, currentPos);
            mc.player.getInventory().selectedSlot = prevItem;
            mineTimer.reset();
        }
    }

    @EventHandler
    public void onUpdatePost(TickEvent.Post event) {
        if (priorSlot != -1 && silentTimer.hasPassed(350)) {
            mc.player.getInventory().selectedSlot = priorSlot;
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(priorSlot));
            priorSlot = -1;
        }
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof BlockUpdateS2CPacket && currentPos != null) {
            if (((BlockUpdateS2CPacket) event.packet).getPos().equals(currentPos) && ((BlockUpdateS2CPacket) event.packet).getState().getBlock() instanceof AirBlock) {
                prevPos = currentPos;
                currentPos = null;
                currentFacing = null;
            }
        }
    }

    @EventHandler(priority = 90)
    public void onUpdateWalkingPlayerPre(PlayerMoveEvent event) {
        if (currentPos == null && delayTimer.hasPassed(delay.get() * 1000)) {
            PlayerEntity target = getNearestTarget();

            if (target != null) {
                ArrayList<BlockPos> vulnerablePos = getVulnerablePositions(target.getBlockPos());
                BlockPos bestPos = vulnerablePos.stream().min(Comparator.comparing(pos -> mc.player.getBlockPos().getSquaredDistance(pos))).orElse(null);
                if (bestPos != null) {
                    Direction bestFacing = getFacing(bestPos, strictDirection.get());
                    if (bestFacing != null) {

                        currentPos = bestPos;
                        currentFacing = bestFacing;
                        curBlockDamage = 0F;
                        stopped = false;
                        delayTimer.reset();

                        Vec3d hitVec = new Vec3d(currentPos.getX() + 0.5, currentPos.getY() + 0.5, currentPos.getZ() + 0.5)
                            .add(new Vec3d(currentFacing.getUnitVector()).multiply(0.5));

                        if (instant.get() && currentPos.equals(prevPos)) {
                            curBlockDamage = 1F;
                            mineTimer.reset();
                            if (limit.get()) {
                                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, currentPos, currentFacing.getOpposite()));
                            }
                            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, currentPos, currentFacing));
                            if (swing.get()) {
                                mc.player.swingHand(Hand.MAIN_HAND);
                            }
                        } else {
                            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, currentPos, currentFacing));
                            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, currentPos, currentFacing));
                            if (swing.get()) {
                                mc.player.swingHand(Hand.MAIN_HAND);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onDeactivate() {
        if (priorSlot != -1 && mc.player != null) {
            mc.player.getInventory().selectedSlot = priorSlot;
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(priorSlot));
            priorSlot = -1;
        }

        Checker.Check();
    }

    private PlayerEntity getNearestTarget() {
        return mc.world.getPlayers()
            .stream()
            .filter(e -> e != mc.player)
            .filter(e -> !e.isDead())
            .filter(e -> !Friends.get().isFriend(e))
            .filter(e -> e.getHealth() > 0)
            .filter(e -> mc.player.distanceTo(e) <= range.get())
            .filter(AntiSurround::isVulnerable)
            .min(Comparator.comparing(e -> mc.player.distanceTo(e)))
            .orElse(null);
    }

    public static boolean isVulnerable(Entity entity) {
        BlockPos root = entity.getBlockPos();
        return !getVulnerablePositions(root).isEmpty();
    }

    private Direction getFacing(BlockPos pos, boolean strictDirection) {
        List<Direction> validAxis = new ArrayList<>();
        Vec3d eyePos = getEyesPos(mc.player);
        if (strictDirection) {
            Vec3d blockCenter = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            BlockState blockState = mc.world.getBlockState(pos);
            boolean isFullBox = blockState.getBlock() == Blocks.AIR || blockState.isFullCube(mc.world, pos);
            validAxis.addAll(checkAxis(eyePos.x - blockCenter.x, Direction.WEST, Direction.EAST, !isFullBox));
            validAxis.addAll(checkAxis(eyePos.y - blockCenter.y, Direction.DOWN, Direction.UP, true));
            validAxis.addAll(checkAxis(eyePos.z - blockCenter.z, Direction.NORTH, Direction.SOUTH, !isFullBox));
        } else {
            validAxis = Arrays.asList(Direction.values());
        }
        return validAxis.stream().min(Comparator.comparing(enumFacing -> new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
            .add(new Vec3d(enumFacing.getUnitVector()).multiply(0.5)).distanceTo(eyePos))).orElse(null);
    }


    private int findBestTool(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        int bestSlot = -1;
        double bestSpeed = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty() || stack.getItem() == Items.AIR) continue;
            float speed = stack.getMiningSpeedMultiplier(state);
            int eff;
            if (speed > 1) {
                speed += ((eff = EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, stack)) > 0 ? (Math.pow(eff, 2) + 1) : 0);
                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    bestSlot = i;
                }
            }
        }
        return bestSlot;
    }

    public static ArrayList<BlockPos> getVulnerablePositions(BlockPos root) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ArrayList<BlockPos> vP = new ArrayList<>();
        if (!(mc.world.getBlockState(root).getBlock() instanceof AirBlock)) {
            return vP;
        }
        for (Direction facing : Direction.values()) {
            if (facing == Direction.UP || facing == Direction.DOWN) continue;
            if (mc.world.getBlockState(root.offset(facing)).getBlock() instanceof AirBlock)
                return new ArrayList<BlockPos>();
            if (!(mc.world.getBlockState(root.offset(facing)).getBlock() == Blocks.OBSIDIAN)) continue;
            if (canPlaceCrystal(root.offset(facing, 2).down()) && mc.world.getBlockState(root.offset(facing)).getBlock() != Blocks.AIR) {
                vP.add(root.offset(facing));
            } else if (canPlaceCrystal(root.offset(facing)) && mc.world.getBlockState(root.offset(facing)).getBlock() != Blocks.AIR && (
                mc.world.getBlockState(root.offset(facing).down()).getBlock() == Blocks.BEDROCK || mc.world.getBlockState(root.offset(facing).down()).getBlock() == Blocks.OBSIDIAN
            )) {
                vP.add(root.offset(facing));
            }
        }
        return vP;
    }

    public static boolean canPlaceCrystal(BlockPos blockPos) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!(mc.world.getBlockState(blockPos).getBlock() == Blocks.BEDROCK
            || mc.world.getBlockState(blockPos).getBlock() == Blocks.OBSIDIAN)) return false;

        BlockPos boost = blockPos.add(0, 1, 0);

        if (!(mc.world.getBlockState(boost).getBlock() == Blocks.AIR)) return false;

        BlockPos boost2 = blockPos.add(0, 2, 0);

        if (AutoCrystal.getProtocol) {
            if (!(mc.world.getBlockState(boost2).getBlock() == Blocks.AIR)) {
                return false;
            }
        }

        if (!RayTraceUtils.canSee(new Vec3d(blockPos.getX() + 0.5, blockPos.getY() + 1.7, blockPos.getZ() + 0.5), new Vec3d(blockPos.getX() + 0.5, blockPos.getY() + 1.0, blockPos.getZ() + 0.5))) {
            if (LookCalculator.getEyesPos(mc.player).distanceTo(new Vec3d(blockPos.getX() + 0.5, blockPos.getY() + 1.0,blockPos.getZ() + 0.5)) > AutoCrystal.getPlaceWallRange) {
                return false;
            }
        }

        Vec3d playerEyes = LookCalculator.getEyesPos(mc.player);
        boolean canPlace = false;

        if (AutoCrystal.getStrictDirection) {
            for (Vec3d point : AutoCrystal.fastMultiPoint) {
                Vec3d p = new Vec3d(blockPos.getX() + point.getX(), blockPos.getY() + point.getY(), blockPos.getZ() + point.getZ());
                double distanceTo = playerEyes.distanceTo(p);
                if (distanceTo > AutoCrystal.getPlaceRange) {
                    continue;
                }
                if (distanceTo > AutoCrystal.getPlaceWallRange) {
                    if (AutoCrystal.getStrictDirection) {
                        BlockHitResult result = mc.world.raycast(new RaycastContext(playerEyes, p, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
                        if (result != null && result.getType() == HitResult.Type.BLOCK && result.getBlockPos().equals(blockPos)) {
                            canPlace = true;
                            break;
                        }
                    }
                } else {
                    canPlace = true;
                    break;
                }
            }
        } else {
            for (Direction dir : Direction.values()) {
                Vec3d p = new Vec3d(blockPos.getX() + 0.5 + dir.getVector().getX() * 0.5,
                    blockPos.getY() + 0.5 + dir.getVector().getY() * 0.5,
                    blockPos.getZ() + 0.5 + dir.getVector().getZ() * 0.5);
                double distanceTo = playerEyes.distanceTo(p);
                if (distanceTo > AutoCrystal.getPlaceRange) {
                    continue;
                }
                if (distanceTo > AutoCrystal.getPlaceWallRange) {
                    if (AutoCrystal.getStrictDirection) {
                        BlockHitResult result = mc.world.raycast(new RaycastContext(playerEyes, p, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
                        if (result != null && result.getType() == HitResult.Type.BLOCK && result.getBlockPos().equals(blockPos)) {
                            canPlace = true;
                            break;
                        }
                    }
                } else {
                    canPlace = true;
                    break;
                }
            }
        }

        if (!canPlace) {
            return false;
        }

        return mc.world.getOtherEntities((Entity) null, new Box(blockPos).stretch(0, AutoCrystal.getProtocol ? 2 : 1, 0)).stream()
            .filter(entity -> !AutoCrystal.silentMap.containsKey(entity.getId()) && (!(entity instanceof EndCrystalEntity) || entity.age > 20)).count() == 0;
    }

    public Vec3d getEyesPos(Entity entity) {
        return entity.getPos().add(0, entity.getEyeHeight(entity.getPose()), 0);
    }

    public ArrayList<Direction> checkAxis(double diff, Direction negativeSide, Direction positiveSide, boolean bothIfInRange) {
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

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (currentPos != null && showMining.get()) {
            if (curBlockDamage >= 1F) {
                event.renderer.box(currentPos, readyColor.get(), readyLineColor.get(), ShapeMode.Both, 0);
            } else {
                event.renderer.box(currentPos, miningColor.get(), miningLineColor.get(), ShapeMode.Both, 0);
            }
        }
    }
}
