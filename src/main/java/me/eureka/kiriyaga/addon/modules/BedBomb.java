package me.eureka.kiriyaga.addon.modules;

import me.eureka.kiriyaga.addon.Nigger;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BedItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static net.minecraft.entity.effect.StatusEffects.HASTE;

public class BedBomb extends Module {
    private final SettingGroup sg_general = settings.getDefaultGroup();
    private final SettingGroup sg_smart = settings.createGroup("Smart");
    private final SettingGroup sg_automatic = settings.createGroup("Automatic");
    private final SettingGroup sg_bed_refill = settings.createGroup("Bed Re-fill");
    private final SettingGroup sg_pause = settings.createGroup("Pause");
    private final SettingGroup sg_render = settings.createGroup("Render");

    // Ranges
    private final Setting<SortPriority> priority = sg_general.add(new EnumSetting.Builder<SortPriority>().name("priority").defaultValue(SortPriority.LowestDistance).build());
    private final Setting<Double> target_range = sg_general.add(new DoubleSetting.Builder().name("target-range").defaultValue(7).min(0).sliderMax(9).build());
    private final Setting<Double> place_range = sg_general.add(new DoubleSetting.Builder().name("place-range").defaultValue(5.5).min(0).sliderMax(7).build());
    private final Setting<Integer> delay = sg_general.add(new IntSetting.Builder().name("delay").defaultValue(10).min(0).sliderMax(30).build());
    private final Setting<Boolean> anti_self = sg_general.add(new BoolSetting.Builder().name("anti-self").defaultValue(false).build());
    private final Setting<Boolean> anti_sneak = sg_general.add(new BoolSetting.Builder().name("anti-sneak").defaultValue(false).build());
    private final Setting<rotate_mode> rotation = sg_general.add(new EnumSetting.Builder<rotate_mode>().name("rotation").defaultValue(rotate_mode.Default).build());

    // Smart
    private final Setting<Boolean> smart = sg_smart.add(new BoolSetting.Builder().name("smart").defaultValue(false).build());
    private final Setting<Boolean> instant_break = sg_smart.add(new BoolSetting.Builder().name("instant-break").visible(smart::get).defaultValue(true).build());
    private final Setting<Boolean> perfect_break = sg_smart.add(new BoolSetting.Builder().name("perfect-break").visible(smart::get).defaultValue(false).build());
    private final Setting<Boolean> bed_cleaner = sg_smart.add(new BoolSetting.Builder().name("bed-cleaner").visible(smart::get).defaultValue(true).build());
    private final Setting<Double> smart_range = sg_smart.add(new DoubleSetting.Builder().name("smart-range").defaultValue(7).min(1).sliderMin(1).sliderMax(15).visible(smart::get).build());
    private final Setting<Double> extra_range = sg_smart.add(new DoubleSetting.Builder().name("extra-range").defaultValue(9).min(1).sliderMin(1).sliderMax(15).visible(smart::get).build());
    private final Setting<Double> smart_up = sg_smart.add(new DoubleSetting.Builder().name("smart-up").defaultValue(3).min(1).sliderMin(1).sliderMax(10).visible(smart::get).build());
    private final Setting<Double> smart_down = sg_smart.add(new DoubleSetting.Builder().name("smart-down").defaultValue(3).min(1).sliderMin(1).sliderMax(10).visible(smart::get).build());

    // Automatic
    private final Setting<Boolean> place_obsidian = sg_automatic.add(new BoolSetting.Builder().name("place-obsidian").defaultValue(false).build());
    private final Setting<Boolean> trap_breaker = sg_automatic.add(new BoolSetting.Builder().name("trap-breaker").defaultValue(false).build());
    private final Setting<Boolean> burrow_breaker = sg_automatic.add(new BoolSetting.Builder().name("burrow-breaker").defaultValue(false).build());
    private final Setting<Boolean> string_breaker = sg_automatic.add(new BoolSetting.Builder().name("string-breaker").defaultValue(false).build());
    private final Setting<Boolean> speed_mine = sg_automatic.add(new BoolSetting.Builder().name("speed-mine").defaultValue(false).build());

    // Bed Re-fill
    private final Setting<Boolean> bed_refill = sg_bed_refill.add(new BoolSetting.Builder().name("bed-refill").defaultValue(true).build());
    private final Setting<Integer> bed_slot = sg_bed_refill.add(new IntSetting.Builder().name("bed-slot").defaultValue(7).min(1).max(9).sliderMin(1).sliderMax(9).visible(bed_refill::get).build());

    // Pause
    private final Setting<Boolean> pause_on_eat = sg_pause.add(new BoolSetting.Builder().name("pause-on-eat").defaultValue(true).build());
    private final Setting<Boolean> pause_on_drink = sg_pause.add(new BoolSetting.Builder().name("pause-on-drink").defaultValue(true).build());
    private final Setting<Boolean> pause_on_mine = sg_pause.add(new BoolSetting.Builder().name("pause-on-mine").defaultValue(false).build());
    private final Setting<Boolean> pause_on_no_beds = sg_pause.add(new BoolSetting.Builder().name("pause-on-no-beds").defaultValue(false).build());
    private final Setting<Boolean> pause_on_burrow = sg_pause.add(new BoolSetting.Builder().name("pause-on-burrow").defaultValue(true).build());
    private final Setting<Boolean> pause_on_health = sg_pause.add(new BoolSetting.Builder().name("pause-on-health").defaultValue(false).build());
    private final Setting<Integer> health = sg_pause.add(new IntSetting.Builder().name("health").defaultValue(12).min(1).sliderMin(1).sliderMax(36).visible(pause_on_health::get).build());

    // Render
    private final Setting<Boolean> render = sg_render.add(new BoolSetting.Builder().name("render").defaultValue(true).build());
    private final Setting<ShapeMode> shape_mode = sg_render.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> side_color = sg_render.add(new ColorSetting.Builder().name("side-color").defaultValue(new SettingColor(30, 35, 122, 75)).build());
    private final Setting<SettingColor> line_color = sg_render.add(new ColorSetting.Builder().name("line-color").defaultValue(new SettingColor(30, 35, 122)).build());

    private PlayerEntity target;
    private int rotate;
    private int ticks;
    private boolean ignore_placement;
    private boolean start_mining = false;

    private boolean rotated = false;

    public BedBomb() {
        super(Nigger.Category, "Bed Bomb", "Automatically placed and blows up beds in nether/end dimension.");
    }

    @Override
    public void onActivate() {
        rotated = false;
        if (smart.get() && instant_break.get()) ticks = delay.get();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        ignore_placement = true;
        rotate = 5;
        ticks++;

        //if (anti_sneak.get()) mc.options.keySneak.setPressed(false);

        if (mc.world.getDimension().isBedWorking()) {
            ChatUtils.error("You can`t blow up beds in this dimension! disabling...");
            toggle();
            return;
        }

        if (PlayerUtils.getTotalHealth() <= health.get() && pause_on_health.get()) return;

        if (pause_on_no_beds.get() && bed_found()) {
            ChatUtils.error("No beds found, disabling...");
            toggle();
            return;
        }

        target = TargetUtils.getPlayerTarget(target_range.get(), priority.get());
        if (target == null) {
            if (smart.get() && instant_break.get()) ticks = delay.get();
            return;
        }

        if (anti_self.get() && (mc.player.getBlockPos().equals(target.getBlockPos()) || mc.player.getBlockPos().equals(target.getBlockPos().up()) || mc.player.getBlockPos().equals(target.getBlockPos().down())))
            return;

        if (PlayerUtils.shouldPause(pause_on_mine.get(), pause_on_eat.get(), pause_on_drink.get())) return;

        if (bed_refill.get()) {
            FindItemResult bedItem = InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem);
            if (bedItem.found() && bedItem.getSlot() != bed_slot.get() - 1) {
                InvUtils.move().from(bedItem.getSlot()).toHotbar(bed_slot.get() - 1);
            }
        }

        //if (anti_sneak.get()) mc.options.keySneak.setPressed(false);
        if (speed_mine.get()) get_speedmine();
        if (string_breaker.get()) break_strings(target);
        if (burrow_breaker.get()) break_burrow(target);
        if (trap_breaker.get()) break_trap(target);

        if (closest_block(target) == null) return;

        if (pause_on_burrow.get()) {
            BlockPos fillBlock = target.getBlockPos();
            if (mc.world.getBlockState(fillBlock).getBlock().getBlastResistance() > 600 || mc.world.getBlockState(fillBlock.up()).getBlock().getBlastResistance() > 600)
                return;
        }

        if (!within_range(closest_block(target), place_range.get())) {
            if (smart.get() && instant_break.get()) ticks = delay.get();
            return;
        }

        if (closest_block(target) == null) return;

        best_direction(target);

        double yaw = mc.player.getYaw();
        double pitch = mc.player.getPitch();

        if (ticks == delay.get() - 1 && rotation.get() == rotate_mode.Strict) {
            Rotations.rotate(Rotations.getYaw(closest_block(target)), Rotations.getPitch(closest_block(target)));
        }

        if (smart.get() && perfect_break.get()) {
            if (target.hurtTime != 0) return;
        }

        if (ticks >= delay.get()) {
            //if (anti_sneak.get()) mc.options.keySneak.setPressed(false);
            start_bombing(closest_block(target));
            if (smart.get() && bed_cleaner.get()) bed_cleaner();

            ticks = 0;
        }

        if (ticks == 0 && rotation.get() == rotate_mode.Strict) {
            Rotations.rotate(yaw, pitch);
        }

        if (place_obsidian.get() && is_surrounded(target) && within_range(closest_block(target), place_range.get()) && ignore_placement) {
            BlockUtils.place(target.getBlockPos().up(2), InvUtils.findInHotbar(Items.OBSIDIAN), false, 0, true);
        }
    }

    private void best_direction(PlayerEntity target) {
        //if (anti_sneak.get()) mc.options.keySneak.setPressed(false);
        if (target != null && target.getBlockPos() != null && closest_block(target) != null) {

            BlockPos rotateWest = target.getBlockPos().west().up();
            BlockPos rotateEast = target.getBlockPos().east().up();
            BlockPos rotateSouth = target.getBlockPos().south().up();
            BlockPos rotateNorth = target.getBlockPos().north().up();
            BlockPos dayn = target.getBlockPos();

            if (rotate_check(target, dayn.west(2).north()) || rotate_check(target, dayn.west(2).south()) || rotate_check(target, rotateWest) || rotate_check(target, rotateWest.down()) || rotate_check(target, rotateWest.down(2)) || rotate_check(target, rotateWest.down().west()) || rotate_check(target, rotateWest.up()) || rotate_check(target, dayn.west(3))) {
                if (rotation.get() == rotate_mode.Default) Rotations.rotate(-90, 0);
                rotate = 4;
            }
            if (rotate_check(target, dayn.east(2).north()) || rotate_check(target, dayn.east(2).south()) || rotate_check(target, rotateEast) || rotate_check(target, rotateEast.down()) || rotate_check(target, rotateEast.down(2)) || rotate_check(target, rotateEast.down().east()) || rotate_check(target, rotateEast.up()) || rotate_check(target, dayn.east(3))) {
                if (rotation.get() == rotate_mode.Default) Rotations.rotate(90, 0);
                rotate = 3;
            }
            if (rotate_check(target, dayn.west().south(2)) || rotate_check(target, dayn.east().south(2)) || rotate_check(target, rotateSouth) || rotate_check(target, rotateSouth.down()) || rotate_check(target, rotateSouth.down(2)) || rotate_check(target, rotateSouth.down().south()) || rotate_check(target, rotateSouth.up()) || rotate_check(target, dayn.south(3))) {
                if (rotation.get() == rotate_mode.Default) Rotations.rotate(180, 0);
                rotate = 2;
            }
            if (rotate_check(target, dayn.east().north(2)) || rotate_check(target, dayn.west().north(2)) || rotate_check(target, rotateNorth) || rotate_check(target, rotateNorth.down()) || rotate_check(target, rotateNorth.down(2)) || rotate_check(target, rotateNorth.down().north()) || rotate_check(target, rotateNorth.up()) || rotate_check(target, dayn.north(3))) {
                if (rotation.get() == rotate_mode.Default) Rotations.rotate(0, 0);
                rotate = 1;
            }
        }
    }

    private List<BlockPos> place_positions(PlayerEntity player) {
        if (player == null) return null;
        List<BlockPos> positions = new ArrayList<>();

        for (Direction side : Direction.values()) {
            if (side == Direction.UP || side == Direction.DOWN) continue;

            BlockPos pos = player.getBlockPos().up().offset(side);
            BlockPos posDown = player.getBlockPos().offset(side);
            BlockPos posUp = player.getBlockPos().up(2).offset(side);
            BlockPos posRageDown = player.getBlockPos().down().offset(side);

            BlockPos horiz = player.getBlockPos();

            if (can_place(pos, true) && has_player(pos) && mc.world.getBlockState(pos).getMaterial().isReplaceable() || mc.world.getBlockState(target.getBlockPos().up()).getBlock() instanceof BedBlock) {
                positions.add(pos);
            }

            if (can_place(posDown, true) && has_player(posDown) && mc.world.getBlockState(posDown).getMaterial().isReplaceable() || mc.world.getBlockState(target.getBlockPos()).getBlock() instanceof BedBlock) {
                positions.add(posDown);
            }

            if (can_place(posUp, true) && should_up(target) && has_player(posUp) && mc.world.getBlockState(posUp).getMaterial().isReplaceable() || ((mc.world.getBlockState(posUp).getBlock() instanceof BedBlock) && should_up(target)) || ((smart.get() && mc.player.getY() - target.getY() >= smart_down.get()) && can_place(posUp, true) && smart.get() && can_place(target.getBlockPos().up(2), true) && has_player(posUp))) {
                ignore_placement = false;
                positions.add(posUp);
            }

            if ((smart.get() && target.getY() - mc.player.getY() >= smart_up.get()) && (mc.world.getBlockState(posRageDown).getMaterial().isReplaceable() && can_place(posRageDown, true)) && has_player(posRageDown) && smart.get() && mc.world.getBlockState(target.getBlockPos().down()).getMaterial().isReplaceable() || mc.world.getBlockState(posRageDown).getBlock() instanceof BedBlock) {
                positions.add(posRageDown);
            }

            if (can_place(horiz.west().up(), true) && smart.get() && (smart.get() && mc.player.distanceTo(target) >= smart_range.get()) && horizontal_place(target, Direction.WEST) && mc.world.getBlockState(horiz.west(2)).getMaterial().isReplaceable() && can_place(horiz.west(2), true) || mc.world.getBlockState(horiz.west(2)).getBlock() instanceof BedBlock) {
                positions.add(horiz.west(2));
            }

            if ((smart.get() && mc.player.distanceTo(target) >= extra_range.get()) && retarded_place(target, Direction.WEST) || mc.world.getBlockState(horiz.west(3)).getBlock() instanceof BedBlock) {
                positions.add(horiz.west(3));
            }

            if (can_place(horiz.east().up(), true) && smart.get() && (smart.get() && mc.player.distanceTo(target) >= smart_range.get()) && horizontal_place(target, Direction.EAST) && mc.world.getBlockState(horiz.east(2)).getMaterial().isReplaceable() && can_place(horiz.east(2), true) || mc.world.getBlockState(horiz.east(2)).getBlock() instanceof BedBlock) {
                positions.add(horiz.east(2));
            }

            if ((smart.get() && mc.player.distanceTo(target) >= extra_range.get()) && retarded_place(target, Direction.EAST) || mc.world.getBlockState(horiz.east(3)).getBlock() instanceof BedBlock) {
                positions.add(horiz.east(3));
            }

            if (can_place(horiz.south().up(), true) && smart.get() && (smart.get() && mc.player.distanceTo(target) >= smart_range.get()) && horizontal_place(target, Direction.SOUTH) && mc.world.getBlockState(horiz.south(2)).getMaterial().isReplaceable() && can_place(horiz.south(2), true) || mc.world.getBlockState(horiz.south(2)).getBlock() instanceof BedBlock) {
                positions.add(horiz.south(2));
            }

            if ((smart.get() && mc.player.distanceTo(target) >= extra_range.get()) && retarded_place(target, Direction.SOUTH) || mc.world.getBlockState(horiz.south(3)).getBlock() instanceof BedBlock) {
                positions.add(horiz.south(3));
            }

            if (can_place(horiz.north().up(), true) && smart.get() && (smart.get() && mc.player.distanceTo(target) >= smart_range.get()) && horizontal_place(target, Direction.NORTH) && mc.world.getBlockState(horiz.north(2)).getMaterial().isReplaceable() && can_place(horiz.north(2), true) || mc.world.getBlockState(horiz.north(2)).getBlock() instanceof BedBlock) {
                positions.add(horiz.north(2));
            }

            if ((smart.get() && mc.player.distanceTo(target) >= extra_range.get()) && retarded_place(target, Direction.NORTH) || mc.world.getBlockState(horiz.north(3)).getBlock() instanceof BedBlock) {
                positions.add(horiz.north(3));
            }

            if (smart.get() && (smart.get() && mc.player.distanceTo(target) >= smart_range.get()) && can_place(horiz.north(), true) && can_place(horiz.north().up(), true) && can_place(horiz.west(), true) && can_place(horiz.west().up(), true) && can_place(horiz.north().west(), true) && can_place(horiz.west().north(2), true) || mc.world.getBlockState(horiz.north(2).west()).getBlock() instanceof BedBlock) {
                positions.add(horiz.west().north(2));
            }

            if (smart.get() && (smart.get() && mc.player.distanceTo(target) >= smart_range.get()) && can_place(horiz.north(), true) && can_place(horiz.north().up(), true) && can_place(horiz.east(), true) && can_place(horiz.east().up(), true) && can_place(horiz.north().east(), true) && can_place(horiz.east().north(2), true) || mc.world.getBlockState(horiz.north(2).east()).getBlock() instanceof BedBlock) {
                positions.add(horiz.east().north(2));
            }

            if (smart.get() && (smart.get() && mc.player.distanceTo(target) >= smart_range.get()) && can_place(horiz.south(), true) && can_place(horiz.south().up(), true) && can_place(horiz.east(), true) && can_place(horiz.east().up(), true) && can_place(horiz.south().east(), true) && can_place(horiz.east().south(2), true) || mc.world.getBlockState(horiz.south(2).east()).getBlock() instanceof BedBlock) {
                positions.add(horiz.east().south(2));
            }

            if (smart.get() && (smart.get() && mc.player.distanceTo(target) >= smart_range.get()) && can_place(horiz.south(), true) && can_place(horiz.south().up(), true) && can_place(horiz.west(), true) && can_place(horiz.west().up(), true) && can_place(horiz.south().west(), true) && can_place(horiz.west().south(2), true) || mc.world.getBlockState(horiz.south(2).west()).getBlock() instanceof BedBlock) {
                positions.add(horiz.west().south(2));
            }

            if (smart.get() && (smart.get() && mc.player.distanceTo(target) >= smart_range.get()) && can_place(horiz.west(), true) && can_place(horiz.west().up(), true) && can_place(horiz.south(), true) && can_place(horiz.south().up(), true) && can_place(horiz.west().south(), true) && can_place(horiz.south().west(2), true) || mc.world.getBlockState(horiz.west(2).south()).getBlock() instanceof BedBlock) {
                positions.add(horiz.south().west(2));
            }

            if (smart.get() && (smart.get() && mc.player.distanceTo(target) >= smart_range.get()) && can_place(horiz.west(), true) && can_place(horiz.west().up(), true) && can_place(horiz.north(), true) && can_place(horiz.north().up(), true) && can_place(horiz.west().north(), true) && can_place(horiz.north().west(2), true) || mc.world.getBlockState(horiz.west(2).north()).getBlock() instanceof BedBlock) {
                positions.add(horiz.north().west(2));
            }

            if (smart.get() && (smart.get() && mc.player.distanceTo(target) >= smart_range.get()) && can_place(horiz.east(), true) && can_place(horiz.east().up(), true) && can_place(horiz.south(), true) && can_place(horiz.south().up(), true) && can_place(horiz.east().south(), true) && can_place(horiz.south().east(2), true) || mc.world.getBlockState(horiz.east(2).south()).getBlock() instanceof BedBlock) {
                positions.add(horiz.south().east(2));
            }
            if (smart.get() && (smart.get() && mc.player.distanceTo(target) >= smart_range.get()) && can_place(horiz.east(), true) && can_place(horiz.east().up(), true) && can_place(horiz.north(), true) && can_place(horiz.north().up(), true) && can_place(horiz.east().north(), true) && can_place(horiz.north().east(2), true) || mc.world.getBlockState(horiz.east(2).north()).getBlock() instanceof BedBlock) {
                positions.add(horiz.north().east(2));
            }
        }
        return positions;
    }

    private BlockPos closest_block(PlayerEntity player) {
        if (player == null) return null;

        List<BlockPos> posList = place_positions(player);
        posList.sort(Comparator.comparingDouble(PlayerUtils::distanceTo));
        return posList.isEmpty() ? null : posList.get(0);
    }

    private boolean rotate_check(PlayerEntity target, BlockPos rotateSide) {
        return rotateSide.getX() == closest_block(target).getX() && rotateSide.getY() == closest_block(target).getY() && rotateSide.getZ() == closest_block(target).getZ();
    }

    private boolean should_up(LivingEntity target) {
        if (target != null && target.getBlockPos() != null) {
            return !mc.world.getBlockState(target.getBlockPos().west()).getMaterial().isReplaceable() && !can_place(target.getBlockPos().west(), true) && !mc.world.getBlockState(target.getBlockPos().north()).getMaterial().isReplaceable() && !can_place(target.getBlockPos().north(), true) && !mc.world.getBlockState(target.getBlockPos().south()).getMaterial().isReplaceable() && !can_place(target.getBlockPos().south(), true) && !mc.world.getBlockState(target.getBlockPos().east()).getMaterial().isReplaceable() && !can_place(target.getBlockPos().east(), true) && !mc.world.getBlockState(target.getBlockPos().west().up()).getMaterial().isReplaceable() && !can_place(target.getBlockPos().west().up(), true) && !mc.world.getBlockState(target.getBlockPos().north().up()).getMaterial().isReplaceable() && !can_place(target.getBlockPos().north().up(), true) && !mc.world.getBlockState(target.getBlockPos().south().up()).getMaterial().isReplaceable() && !can_place(target.getBlockPos().south().up(), true) && !mc.world.getBlockState(target.getBlockPos().east().up()).getMaterial().isReplaceable() && !can_place(target.getBlockPos().east().up(), true) && (mc.world.getBlockState(target.getBlockPos().up(2)).getMaterial().isReplaceable() || (mc.world.getBlockState(target.getBlockPos().up(2)).getBlock() instanceof BedBlock));
        }
        return false;
    }

    private void start_bombing(BlockPos posPlaceBlocks) {
        if (posPlaceBlocks != null) {
            FindItemResult bedItem = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BedItem);

            BlockHitResult place_result;
            BlockHitResult break_result;

            switch (rotate) {
                case 1 -> place_result = new BlockHitResult(best_hitpos(closest_block(target)), Direction.NORTH, closest_block(target), false);
                case 2 -> place_result = new BlockHitResult(best_hitpos(closest_block(target)), Direction.SOUTH, closest_block(target), false);
                case 3 -> place_result = new BlockHitResult(best_hitpos(closest_block(target)), Direction.EAST, closest_block(target), false);
                case 4 -> place_result = new BlockHitResult(best_hitpos(closest_block(target)), Direction.WEST, closest_block(target), false);
                default -> place_result = new BlockHitResult(best_hitpos(closest_block(target)), Direction.DOWN, closest_block(target), false);
            }

            int prevSlot = mc.player.getInventory().selectedSlot;
            InvUtils.swap(bedItem.getSlot(), false);

            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, place_result));

            if (anti_sneak.get()) mc.options.keySneak.setPressed(false);

            switch (rotate) {
                case 1 -> break_result = new BlockHitResult(best_hitpos(closest_block(target)), Direction.NORTH, posPlaceBlocks, false);
                case 2 -> break_result = new BlockHitResult(best_hitpos(closest_block(target)), Direction.SOUTH, posPlaceBlocks, false);
                case 3 -> break_result = new BlockHitResult(best_hitpos(closest_block(target)), Direction.EAST, posPlaceBlocks, false);
                case 4 -> break_result = new BlockHitResult(best_hitpos(closest_block(target)), Direction.WEST, posPlaceBlocks, false);
                default -> break_result = new BlockHitResult(best_hitpos(closest_block(target)), Direction.DOWN, posPlaceBlocks, false);
            }

            mc.interactionManager.interactBlock(mc.player, mc.world, Hand.OFF_HAND, break_result);

            mc.player.getInventory().selectedSlot = prevSlot;
        }
    }

    public Vec3d best_hitpos(BlockPos pos) {
        assert (mc.player != null);
        double x = MathHelper.clamp((double)(mc.player.getX() - (double)pos.getX()), (double)0.0, (double)1.0);
        double y = MathHelper.clamp((double)(mc.player.getY() - (double)pos.getY()), (double)0.0, (double)0.6);
        double z = MathHelper.clamp((double)(mc.player.getZ() - (double)pos.getZ()), (double)0.0, (double)1.0);
        Vec3d hitPos = new Vec3d(0.0, 0.0, 0.0);
        ((IVec3d)hitPos).set((double)pos.getX() + x, (double)pos.getY() + y, (double)pos.getZ() + z);
        return hitPos;
    }

    private boolean within_range(BlockPos placePos, double testRange) {
        return mc.player.getBlockPos().isWithinDistance(placePos, testRange);
    }

    private boolean horizontal_place(LivingEntity target, Direction direction) {
        if (target != null && target.getBlockPos() != null) {
            assert mc.world != null;
            switch (direction) {
                case NORTH -> {
                    if (retarded_check(target.getBlockPos().north()))
                        return true;
                }
                case SOUTH -> {
                    if (retarded_check(target.getBlockPos().south()))
                        return true;
                }
                case EAST -> {
                    if (retarded_check(target.getBlockPos().east()))
                        return true;
                }
                case WEST -> {
                    if (retarded_check(target.getBlockPos().west()))
                        return true;
                }
            }
        }
        return false;
    }

    private boolean retarded_place(LivingEntity target, Direction direction) {
        if (target != null && target.getBlockPos() != null) {
            assert mc.world != null;
            switch (direction) {
                case NORTH -> {
                    if (retarded_check(target.getBlockPos().up().north()) && retarded_check(target.getBlockPos().north()) && retarded_check(target.getBlockPos().north(2)) && retarded_check(target.getBlockPos().north(3)))
                        return true;
                }
                case SOUTH -> {
                    if (retarded_check(target.getBlockPos().up().south()) && retarded_check(target.getBlockPos().south()) && retarded_check(target.getBlockPos().south(2)) && retarded_check(target.getBlockPos().south(3)))
                        return true;
                }
                case EAST -> {
                    if (retarded_check(target.getBlockPos().up().east()) && retarded_check(target.getBlockPos().east()) && retarded_check(target.getBlockPos().east(2)) && retarded_check(target.getBlockPos().east(3)))
                        return true;
                }
                case WEST -> {
                    if (retarded_check(target.getBlockPos().up().west()) && retarded_check(target.getBlockPos().west()) && retarded_check(target.getBlockPos().west(2)) && retarded_check(target.getBlockPos().west(3)))
                        return true;
                }
            }
        }
        return false;
    }


    private boolean retarded_check(BlockPos targetpos) {
        return mc.world.getBlockState(targetpos).getMaterial().isReplaceable() && can_place(targetpos, true);
    }

    private boolean is_surrounded(LivingEntity target) {
        return !mc.world.getBlockState(target.getBlockPos().add(1, 0, 0)).isAir() && !mc.world.getBlockState(target.getBlockPos().add(-1, 0, 0)).isAir() && !mc.world.getBlockState(target.getBlockPos().add(0, 0, 1)).isAir() && !mc.world.getBlockState(target.getBlockPos().add(0, 0, -1)).isAir();
    }

    private boolean is_in_green_hole(BlockPos pos) {
        return !BlockUtils.canBreak(pos.west()) && !BlockUtils.canBreak(pos.south()) && !BlockUtils.canBreak(pos.east()) && !BlockUtils.canBreak(pos.north());
    }

    private boolean is_self_trapped(LivingEntity target) {
        return !mc.world.getBlockState(target.getBlockPos().add(1, 1, 0)).isAir() && !mc.world.getBlockState(target.getBlockPos().add(-1, 1, 0)).isAir() && !mc.world.getBlockState(target.getBlockPos().add(0, 1, 1)).isAir() && !mc.world.getBlockState(target.getBlockPos().add(0, 1, -1)).isAir() && !mc.world.getBlockState(target.getBlockPos().add(0, 2, 0)).isAir();
    }

    private void break_strings(PlayerEntity target) {
        BlockPos pos = target.getBlockPos().up();
        if (mc.world.getBlockState(pos).getBlock() != Block.getBlockFromItem(Items.STRING) || !within_range(pos, place_range.get()))
            return;
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, Direction.UP));
        mc.player.swingHand(Hand.MAIN_HAND);
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP));
    }

    private boolean bed_found() {
        return InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem).getCount() <= 0;
    }

    private void bed_cleaner() {
        double breakRangeLeft = place_range.get() * 8;
        if (target == null) {
            return;
        }

        if (is_retard(target)) return;
        assert mc.player != null;
        if (!mc.player.isSneaking()) {
            assert mc.world != null;
            for (BlockEntity e : Utils.blockEntities()) {
                if (e instanceof BedBlockEntity && e.getPos().getSquaredDistance(mc.player.getPos(), true) < breakRangeLeft) {
                    BlockPos pos = e.getPos();
                    Vec3d posv3d = new Vec3d(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
                    assert mc.interactionManager != null;
                    mc.interactionManager.interactBlock(mc.player, mc.world, Hand.OFF_HAND, new BlockHitResult(posv3d, Direction.UP, pos, false));
                }
            }
        }
    }

    private boolean is_retard(LivingEntity entity) {
        BlockPos entityBlockPos = entity.getBlockPos();
        return is_blast_res(mc.world.getBlockState(entity.getBlockPos().add(1, 0, 0)).getBlock()) && is_blast_res(mc.world.getBlockState(entityBlockPos.add(-1, 0, 0)).getBlock()) && is_blast_res(mc.world.getBlockState(entityBlockPos.add(0, 0, 1)).getBlock()) && is_blast_res(mc.world.getBlockState(entityBlockPos.add(0, 0, -1)).getBlock()) && is_blast_res(mc.world.getBlockState(entity.getBlockPos().add(1, 1, 0)).getBlock()) && is_blast_res(mc.world.getBlockState(entityBlockPos.add(-1, 1, 0)).getBlock()) && is_blast_res(mc.world.getBlockState(entityBlockPos.add(0, 1, 1)).getBlock()) && is_blast_res(mc.world.getBlockState(entityBlockPos.add(0, 1, -1)).getBlock()) && is_blast_res(mc.world.getBlockState(entityBlockPos.add(0, -1, 0)).getBlock()) && is_blast_res(mc.world.getBlockState(entityBlockPos.add(0, 2, 0)).getBlock());
    }

    private boolean is_blast_res(Block block) {
        if (block == Blocks.BEDROCK) {
            return true;
        } else if (block == Blocks.OBSIDIAN) {
            return true;
        } else if (block == Blocks.ENDER_CHEST) {
            return true;
        } else if (block == Blocks.ANCIENT_DEBRIS) {
            return true;
        } else if (block == Blocks.CRYING_OBSIDIAN) {
            return true;
        } else if (block == Blocks.ENCHANTING_TABLE) {
            return true;
        } else if (block == Blocks.NETHERITE_BLOCK) {
            return true;
        } else if (block == Blocks.ANVIL) {
            return true;
        } else if (block == Blocks.CHIPPED_ANVIL) {
            return true;
        } else if (block == Blocks.DAMAGED_ANVIL) {
            return true;
        } else {
            return block == Blocks.RESPAWN_ANCHOR && mc.world.getDimension().isRespawnAnchorWorking();
        }
    }

    private boolean has_player(BlockPos pos) {
        BlockPos pos2 = mc.player.getBlockPos();
        return !pos2.equals(pos) && !pos2.up().equals(pos);
    }

    // Ломание буррова врага.
    private void break_burrow(PlayerEntity target) {
        if (!is_burrowed(target)) return;
        BlockPos pos = target.getBlockPos();
        if (!within_range(pos, place_range.get())) return;
        FindItemResult bestSlot = InvUtils.findFastestTool(mc.world.getBlockState(pos));
        InvUtils.swap(bestSlot.getSlot(), false);
        if (mc.world.getBlockState(pos.up()).getBlock().getBlastResistance() > 600 && BlockUtils.canBreak(pos)) {
            BlockUtils.breakBlock(pos.up(), true);
        } else if (mc.world.getBlockState(pos).getBlock().getBlastResistance() > 600 && BlockUtils.canBreak(pos)) {
            BlockUtils.breakBlock(pos, true);
        }
    }

    // Ломание блоков вокруг врага.
    private void break_trap(PlayerEntity target) {
        if (is_burrowed(target) || get_closest_block(target) == null) return;
        if (!is_surrounded(target) && !is_self_trapped(target)) return;
        if (!within_range(get_closest_block(target), place_range.get())) return;
        FindItemResult bestSlot = InvUtils.findFastestTool(mc.world.getBlockState(get_closest_block(target)));
        InvUtils.swap(bestSlot.getSlot(), false);
        BlockPos pos = get_closest_block(target);
        BlockUtils.breakBlock(pos, true);
    }

    // Проверка в буррове враг или нет.
    private boolean is_burrowed(PlayerEntity target) {
        BlockPos pos = target.getBlockPos();
        if (mc.world.getBlockState(pos).getBlock().getBlastResistance() > 600 && BlockUtils.canBreak(pos) || mc.world.getBlockState(pos.up()).getBlock().getBlastResistance() > 600 && BlockUtils.canBreak(pos))
            return true;
        return false;
    }

    // Выдаёт статус Спешка 2.
    private void get_speedmine() {
        mc.player.addStatusEffect(new StatusEffectInstance(HASTE, 5, 1, false, false, true));
    }

    // Получение блоков сарраунда врага.
    private List<BlockPos> get_city_blocks(PlayerEntity player) {
        if (player == null) return null;
        if (!is_surrounded(player) || !is_self_trapped(player)) return null;

        List<BlockPos> positions = new ArrayList<>();

        for (Direction side : Direction.values()) {
            if (side == Direction.UP || side == Direction.DOWN) continue;

            BlockPos pos = player.getBlockPos().offset(side);
            BlockPos posUp = player.getBlockPos().up().offset(side);

            if (mc.world.getBlockState(pos).getBlock().getBlastResistance() > 600 && BlockUtils.canBreak(pos)) {
                positions.add(pos);
            }
            if (mc.world.getBlockState(posUp).getBlock().getBlastResistance() > 600 && BlockUtils.canBreak(posUp) && is_in_green_hole(target.getBlockPos())) {
                positions.add(posUp);
            }
        }
        return positions;
    }

    // Сортировка и получение ближайшего блока врага.
    private BlockPos get_closest_block(PlayerEntity player) {
        if (player == null || get_city_blocks(player) == null) return null;
        List<BlockPos> posList = get_city_blocks(player);
        posList.sort(Comparator.comparingDouble(PlayerUtils::distanceTo));
        return posList.isEmpty() ? null : posList.get(0);
    }

    // Проверка можно ли поставить блок.
    public boolean can_place(BlockPos blockPos, boolean checkEntities) {
        if (blockPos == null) return false;

        if (blockPos.getY() > 256 || blockPos.getY() < 0) return false;

        if (!mc.world.getBlockState(blockPos).getMaterial().isReplaceable()) return false;

        return !checkEntities || mc.world.canPlace(Blocks.STONE.getDefaultState(), blockPos, ShapeContext.absent());
    }

    // Рендер
    @EventHandler
    private void onRender(Render3DEvent event) {
        if (closest_block(target) != null && !within_range(closest_block(target), place_range.get())) return;

        if (render.get() && closest_block(target) != null && target != null) {
            int x = closest_block(target).getX();
            int y = closest_block(target).getY();
            int z = closest_block(target).getZ();

            switch (rotate) {
                case 1 -> event.renderer.box(x, y, z, x + 1, y + 0.6, z + 2, side_color.get(), line_color.get(), shape_mode.get(), 0);
                case 2 -> event.renderer.box(x, y, z - 1, x + 1, y + 0.6, z + 1, side_color.get(), line_color.get(), shape_mode.get(), 0);
                case 3 -> event.renderer.box(x - 1, y, z, x + 1, y + 0.6, z + 1, side_color.get(), line_color.get(), shape_mode.get(), 0);
                case 4 -> event.renderer.box(x, y, z, x + 2, y + 0.6, z + 1, side_color.get(), line_color.get(), shape_mode.get(), 0);
            }
        }
    }

    // Получение имени врага в ArrayList модулей.
    public String getInfoString() {
        return target != null ? target.getEntityName() : null;
    }

    public enum rotate_mode {
        Default,
        Strict,
        None
    }
}
