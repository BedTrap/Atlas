package me.bedtrapteam.addon.modules;

import me.bedtrapteam.addon.Nigger;
import me.bedtrapteam.addon.utils.enchansed.Block2Utils;
import me.bedtrapteam.addon.utils.enchansed.Render2Utils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BedItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;

public class BedBomb extends Module {
    public enum HitSwing {
        OFF_HAND,
        MAIN_HAND
    }

    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final SettingGroup sgSwitch = settings.createGroup("Switch");
    private final SettingGroup sgMisc = settings.createGroup("Misc");
    private final SettingGroup sgRage = settings.createGroup("Rage");
    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> place_delay = sgPlace.add(new IntSetting.Builder().name("place-delay").defaultValue(9).min(0).sliderMax(20).build());
    private final Setting<Double> place_range = sgPlace.add(new DoubleSetting.Builder().name("place-range").defaultValue(4).min(0).sliderMax(5).build());
    private final Setting<Double> break_range = sgPlace.add(new DoubleSetting.Builder().name("break-range").defaultValue(4).min(0).sliderMax(5).build());
    private final Setting<Integer> place_down = sgPlace.add(new IntSetting.Builder().name("place-down").defaultValue(3).min(0).sliderMax(6).build());
    private final Setting<Integer> place_up = sgPlace.add(new IntSetting.Builder().name("place-up").defaultValue(3).min(0).sliderMax(6).build());

    private final Setting<Boolean> auto_switch = sgSwitch.add(new BoolSetting.Builder().name("auto-switch").defaultValue(true).build());
    private final Setting<Boolean> auto_move = sgSwitch.add(new BoolSetting.Builder().name("auto-move").defaultValue(false).build());
    private final Setting<Integer> move_slot = sgSwitch.add(new IntSetting.Builder().name("auto-move-slot").defaultValue(9).min(1).sliderMin(1).max(9).sliderMax(9).build());

    private final Setting<Boolean> disable_on_no_beds = sgMisc.add(new BoolSetting.Builder().name("disable-on-no-beds").defaultValue(true).build());
    private final Setting<Boolean> anti_pop = sgMisc.add(new BoolSetting.Builder().name("anti-self-pop").defaultValue(true).build());
    private final Setting<Boolean> burrow_breaker = sgMisc.add(new BoolSetting.Builder().name("burrow-breaker").defaultValue(true).build());
    private final Setting<Boolean> place_obsidian = sgMisc.add(new BoolSetting.Builder().name("place-obsidian").defaultValue(false).build());
    private final Setting<Boolean> place_swing = sgMisc.add(new BoolSetting.Builder().name("place-swing").defaultValue(true).build());
    private final Setting<HitSwing> break_swing = sgMisc.add(new EnumSetting.Builder<HitSwing>().name("break-swing").defaultValue(HitSwing.OFF_HAND).build());

    private final Setting<Boolean> pause_on_burrow = sgPause.add(new BoolSetting.Builder().name("pause-on-burrow").defaultValue(true).build());

    private final Setting<Boolean> smart = sgRage.add(new BoolSetting.Builder().name("smart").defaultValue(false).build());
    private final Setting<Double> range = sgRage.add(new DoubleSetting.Builder().name("default-range").defaultValue(4.35).min(0.0).sliderMax(7.0).visible(smart::get).build());
    private final Setting<Double> down_range = sgRage.add(new DoubleSetting.Builder().name("down-range").defaultValue(6.0).min(0.0).sliderMax(7.0).visible(smart::get).build());
    private final Setting<Double> up_range = sgRage.add(new DoubleSetting.Builder().name("up-range").defaultValue(7.0).min(0.0).sliderMax(7.0).visible(smart::get).build());
    private final Setting<Boolean> return_range = sgRage.add(new BoolSetting.Builder().name("return-range").defaultValue(false).visible(smart::get).build());

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").defaultValue(true).build());
    private final Setting<Boolean> thick = sgRender.add(new BoolSetting.Builder().name("thick").defaultValue(true).visible(render::get).build());
    private final Setting<SettingColor> side_color = sgRender.add(new ColorSetting.Builder().name("place-side-color").defaultValue(new SettingColor(0, 0, 0, 75)).visible(render::get).build());
    private final Setting<SettingColor> line_color = sgRender.add(new ColorSetting.Builder().name("place-line-color").defaultValue(new SettingColor(15, 255, 211, 255)).visible(render::get).build());
    private final Setting<ShapeMode> shape_mode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").defaultValue(ShapeMode.Both).visible(render::get).build());

    private Direction direction;
    public PlayerEntity target;
    private BlockPos bestPos;
    private BlockPos burrowPos;
    private boolean sendNotif = true;

    private int placeDelayLeft;

    public BedBomb() {
        super(Nigger.Category, "bed-bomb", "Automatically places and explodes beds in the Nether and End.");
    }

    @Override
    public void onActivate() {
        sendNotif = true;
        bestPos = null;
        burrowPos = null;

        direction = Direction.EAST;

        placeDelayLeft = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        double defaultPlace = range.get();
        double upPlace = down_range.get();
        double downPlace = up_range.get();
        sendNotif = true;
        if (mc.world.getDimension().isBedWorking()) {
            ChatUtils.error("Overworld moment, disabling.");
            toggle();
            return;
        }

        SortPriority lowDist = SortPriority.LowestDistance;
        target = TargetUtils.getPlayerTarget(place_range.get(), lowDist);

        if (target == null) {
            bestPos = null;
            return;
        }

        if (burrow_breaker.get() && mc.world.getBlockState(target.getBlockPos()).getBlock() instanceof AnvilBlock) {
            assert mc.player != null;
            burrowPos = target.getBlockPos();
            int prevSlot = mc.player.getInventory().selectedSlot;
            FindItemResult pickaxeSlot = InvUtils.find(itemStack -> itemStack.getItem() == Items.DIAMOND_PICKAXE || itemStack.getItem() == Items.NETHERITE_PICKAXE);
            FindItemResult obsidianSlot = InvUtils.findInHotbar(Items.OBSIDIAN);

            if (sendNotif) {
                ChatUtils.info("Burrow Breaker triggered.");
            }
            sendNotif = false;
            if (place_obsidian.get()) {
                InvUtils.swap(obsidianSlot.getSlot(), false);
                BlockUtils.place(target.getBlockPos().up().up(), obsidianSlot, false, 0, false);
            }
            InvUtils.swap(pickaxeSlot.getSlot(), false);
            mine(burrowPos);
            mc.player.getInventory().selectedSlot = prevSlot;
        }
        sendNotif = true;

        if (InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem).found()) {
            assert mc.player != null;
            if (anti_pop.get() && mc.player.getBlockPos().equals(target.getBlockPos()) || mc.player.getBlockPos().up().equals(target.getBlockPos()))
                return;
            if (pause_on_burrow.get() && !pauseOnBurrow(mc.world.getBlockState(target.getBlockPos()).getBlock()))
                return;

            //Check for the best UP pos
            if ((Block2Utils.obbyDoubleSurrounded(target) && mc.world.getBlockState(target.getBlockPos().up()).getBlock() == Blocks.AIR) ||
                (!Block2Utils.isSurrounded(target) && place_up.get() != 0 && mc.player.getPos().getY() - place_up.get() > target.getPos().getY())) {
                if (smart.get() && rageModeDown()) {
                    place_range.set(upPlace);
                } else {
                    if (return_range.get()) {
                        place_range.set(defaultPlace);
                    }
                }
                bestPos = doPlaceUp(target);
                //Check for the best DOWN pos
            } else if (!Block2Utils.isSurrounded(target) && place_down.get() != 0 && mc.player.getPos().getY() + place_down.get() < target.getPos().getY()) {
                if (smart.get() && rageModeUp()) {
                    place_range.set(downPlace);
                } else {
                    if (return_range.get()) {
                        place_range.set(defaultPlace);
                    }
                }
                bestPos = doPlaceDown(target);
            } else {
                //Check for the best pos
                if (smart.get()) {
                    place_range.set(defaultPlace);
                }
                bestPos = doPlace(target);
            }
            if (placeDelayLeft > 0) {
                placeDelayLeft--;
            } else {
                placeBed(bestPos);
                placeDelayLeft = place_delay.get();
            }
        } else if (disable_on_no_beds.get()) {
            ChatUtils.info("You dont have beds, disabling.");
            toggle();
        }
    }

    @EventHandler
    private void breakBed(TickEvent.Post event) {
        double breakRangeLeft = break_range.get() * 8;
        if (target == null) {
            bestPos = null;
            return;
        }

        if (Block2Utils.isRetard(target)) return;
        assert mc.player != null;
        if (!mc.player.isSneaking()) {
            assert mc.world != null;
            for (BlockEntity e : Utils.blockEntities()) {
                if (e instanceof BedBlockEntity && e.getPos().getSquaredDistance(mc.player.getPos(), true) < breakRangeLeft) {
                    BlockPos pos = e.getPos();
                    Vec3d posv3d = new Vec3d(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
                    assert mc.interactionManager != null;
                    if (break_swing.get() == HitSwing.OFF_HAND) {
                        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.OFF_HAND, new BlockHitResult(posv3d, Direction.UP, pos, false));
                    } else {
                        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(posv3d, Direction.UP, pos, false));
                    }
                }
            }
        }
    }

    private void placeBed(BlockPos pos) {
        FindItemResult result = InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem);

        if (result.isMain() && auto_move.get()) doAutoMove();

        result = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BedItem);
        if (!result.found()) return;
        if (result.getHand() == null && !auto_switch.get()) return;

        FindItemResult finalRes = result;

        Rotations.rotate(yawFromDir(direction), mc.player.getPitch(), () -> BlockUtils.place(pos, finalRes, false, 0, place_swing.get(), true, auto_switch.get()));
    }

    private float yawFromDir(Direction direction) {
        switch (direction) {
            case EAST:
                return 90;
            case NORTH:
                return 0;
            case SOUTH:
                return 180;
            case WEST:
                return -90;
        }
        return 0;
    }

    private void doAutoMove() {
        FindItemResult bed = InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem);

        if (bed.found() && bed.getSlot() != move_slot.get() - 1) {
            InvUtils.move().from(bed.getSlot()).toHotbar(move_slot.get() - 1);
        }
    }

    private void mine(BlockPos blockPos) {
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.UP));
        mc.player.swingHand(Hand.MAIN_HAND);
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP));
    }

    public BlockPos doPlaceUp(PlayerEntity target) {
        BlockPos targetPos = target.getBlockPos();

        if (checkPlaceUp(Direction.NORTH, target, true)) return targetPos.up().up().north();
        if (checkPlaceUp(Direction.SOUTH, target, true)) return targetPos.up().up().south();
        if (checkPlaceUp(Direction.EAST, target, true)) return targetPos.up().up().east();
        if (checkPlaceUp(Direction.WEST, target, true)) return targetPos.up().up().west();

        return null;
    }

    public BlockPos doPlace(PlayerEntity target) {
        BlockPos targetPos = target.getBlockPos();

        if (checkPlace(Direction.NORTH, target, true)) return targetPos.up().north();
        if (checkPlace(Direction.SOUTH, target, true)) return targetPos.up().south();
        if (checkPlace(Direction.EAST, target, true)) return targetPos.up().east();
        if (checkPlace(Direction.WEST, target, true)) return targetPos.up().west();

        if (checkPlace(Direction.NORTH, target, false)) return targetPos.north();
        if (checkPlace(Direction.SOUTH, target, false)) return targetPos.south();
        if (checkPlace(Direction.EAST, target, false)) return targetPos.east();
        if (checkPlace(Direction.WEST, target, false)) return targetPos.west();

        return null;
    }

    public BlockPos doPlaceDown(PlayerEntity target) {
        BlockPos targetPos = target.getBlockPos();

        if (checkPlaceDown(Direction.NORTH, target, false)) return targetPos.north();
        if (checkPlaceDown(Direction.SOUTH, target, false)) return targetPos.south();
        if (checkPlaceDown(Direction.EAST, target, false)) return targetPos.east();
        if (checkPlaceDown(Direction.WEST, target, false)) return targetPos.west();

        return null;
    }


    public boolean checkPlaceUp(Direction direction, PlayerEntity target, boolean up) {
        BlockPos headPos = up ? target.getBlockPos().up().up() : target.getBlockPos().up();

        if (mc.world.getBlockState(headPos).getMaterial().isReplaceable() && BlockUtils.canPlace(headPos.offset(direction)) || mc.world.getBlockState(headPos).getBlock() instanceof BedBlock) {
            this.direction = direction;
            return true;
        }

        return false;
    }

    public boolean checkPlace(Direction direction, PlayerEntity target, boolean up) {
        BlockPos headPos = up ? target.getBlockPos().up() : target.getBlockPos();

        if (mc.world.getBlockState(headPos).getMaterial().isReplaceable() && BlockUtils.canPlace(headPos.offset(direction)) || mc.world.getBlockState(headPos).getBlock() instanceof BedBlock) {
            this.direction = direction;
            return true;
        }

        return false;
    }

    public boolean checkPlaceDown(Direction direction, PlayerEntity target, boolean up) {
        BlockPos headPos = up ? target.getBlockPos().up() : target.getBlockPos();

        if (mc.world.getBlockState(headPos).getMaterial().isReplaceable() && BlockUtils.canPlace(headPos.offset(direction)) || mc.world.getBlockState(headPos).getBlock() instanceof BedBlock) {
            this.direction = direction;
            return true;
        }

        return false;
    }

    private boolean rageModeDown() {
        return (mc.player.getBlockPos().north().down(3).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().north().down(4).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().north().down(5).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().north().down(6).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().south().down(3).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().south().down(4).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().south().down(5).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().south().down(6).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().west().down(3).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().west().down(4).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().west().down(5).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().west().down(6).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().east().down(3).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().east().down(4).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().east().down(5).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().east().down(6).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().down(3).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().down(4).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().down(5).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().down(6).equals(target.getBlockPos()));
    }

    private boolean rageModeUp() {
        return (mc.player.getBlockPos().north().up(3).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().north().up(4).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().north().up(5).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().north().up(6).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().north().up(7).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().south().up(3).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().south().up(4).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().south().up(5).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().south().up(6).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().south().up(7).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().west().up(3).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().west().up(4).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().west().up(5).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().west().up(6).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().west().up(7).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().east().up(3).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().east().up(4).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().east().up(5).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().east().up(6).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().east().up(7).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().up(3).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().up(4).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().up(5).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().up(6).equals(target.getBlockPos()) ||
            mc.player.getBlockPos().up(7).equals(target.getBlockPos()));
    }

    public boolean pauseOnBurrow(Block block) {
        return (block == Blocks.AIR ||
            block == Blocks.COBWEB ||
            block == Block.getBlockFromItem(Items.STRING) ||
            block == Blocks.ACACIA_BUTTON ||
            block == Blocks.OAK_BUTTON ||
            block == Blocks.SPRUCE_BUTTON ||
            block == Blocks.BIRCH_BUTTON ||
            block == Blocks.JUNGLE_BUTTON ||
            block == Blocks.DARK_OAK_BUTTON ||
            block == Blocks.CRIMSON_BUTTON ||
            block == Blocks.WARPED_BUTTON ||
            block == Blocks.POLISHED_BLACKSTONE_BUTTON ||
            block == Blocks.WATER ||
            block == Blocks.LAVA ||
            block == Blocks.ACACIA_PRESSURE_PLATE ||
            block == Blocks.BIRCH_PRESSURE_PLATE ||
            block == Blocks.CRIMSON_PRESSURE_PLATE ||
            block == Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE ||
            block == Blocks.DARK_OAK_PRESSURE_PLATE ||
            block == Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE ||
            block == Blocks.JUNGLE_PRESSURE_PLATE ||
            block == Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE ||
            block == Blocks.OAK_PRESSURE_PLATE ||
            block == Blocks.SPRUCE_PRESSURE_PLATE ||
            block == Blocks.STONE_PRESSURE_PLATE ||
            block == Blocks.WARPED_PRESSURE_PLATE ||
            block == Blocks.FIRE);
    }


    @EventHandler
    private void onRender(Render3DEvent event) {
        if (render.get() && bestPos != null) {
            if (anti_pop.get() && mc.player.getBlockPos().equals(target.getBlockPos()) || mc.player.getBlockPos().up().equals(target.getBlockPos()))
                return;
            if (pause_on_burrow.get() && !pauseOnBurrow(mc.world.getBlockState(target.getBlockPos()).getBlock()))
                return;

            int x = bestPos.getX();
            int y = bestPos.getY();
            int z = bestPos.getZ();

            if (thick.get()) {
                switch (direction) {
                    case NORTH -> Render2Utils.render_bed(event, new BlockPos(x,y,z), Direction.NORTH, side_color.get(), line_color.get(), shape_mode.get());
                    case SOUTH -> Render2Utils.render_bed(event, new BlockPos(x,y,z), Direction.SOUTH, side_color.get(), line_color.get(), shape_mode.get());
                    case EAST -> Render2Utils.render_bed(event, new BlockPos(x,y,z), Direction.EAST, side_color.get(), line_color.get(), shape_mode.get());
                    case WEST -> Render2Utils.render_bed(event, new BlockPos(x,y,z), Direction.WEST, side_color.get(), line_color.get(), shape_mode.get());
                }
            } else {
                switch (direction) {
                    case NORTH -> event.renderer.box(x, y, z, x + 1, y + 0.6, z + 2, side_color.get(), line_color.get(), shape_mode.get(), 0);
                    case SOUTH -> event.renderer.box(x, y, z - 1, x + 1, y + 0.6, z + 1, side_color.get(), line_color.get(), shape_mode.get(), 0);
                    case EAST -> event.renderer.box(x - 1, y, z, x + 1, y + 0.6, z + 1, side_color.get(), line_color.get(), shape_mode.get(), 0);
                    case WEST -> event.renderer.box(x, y, z, x + 2, y + 0.6, z + 1, side_color.get(), line_color.get(), shape_mode.get(), 0);
                }
            }
        }
    }

    @Override
    public String getInfoString() {
        if (target != null) return target.getEntityName();
        return null;
    }
}
