package me.bedtrapteam.addon.modules.konas;

import me.bedtrapteam.addon.Atlas;
import me.bedtrapteam.addon.utils.InteractionUtil;
import me.bedtrapteam.addon.utils.enchansed.Block2Utils;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.block.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.StreamSupport;

public class KHoleFill extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").defaultValue(true).build());
    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder().name("swing").defaultValue(true).build());
    private final Setting<Boolean> strictDirection = sgGeneral.add(new BoolSetting.Builder().name("strict-direction").defaultValue(true).build());
    private final Setting<Integer> rangeXZ = sgGeneral.add(new IntSetting.Builder().name("range").defaultValue(5).min(0).max(6).build());
    private final Setting<Integer> actionShift = sgGeneral.add(new IntSetting.Builder().name("action-shift").defaultValue(1).min(0).max(3).build());
    private final Setting<Integer> actionInterval = sgGeneral.add(new IntSetting.Builder().name("action-interval").defaultValue(0).min(0).max(5).build());
    private final Setting<Boolean> jumpDisable = sgGeneral.add(new BoolSetting.Builder().name("jump-disable").defaultValue(true).build());
    private final Setting<Boolean> onlyWebs = sgGeneral.add(new BoolSetting.Builder().name("only-webs").defaultValue(false).build());
    private final Setting<SmartMode> smartMode = sgGeneral.add(new EnumSetting.Builder<SmartMode>().name("safety").defaultValue(SmartMode.Always).build());
    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder().name("enemy-range").defaultValue(10).min(0).max(15).build());
    private final Setting<Boolean> disableWhenNone = sgGeneral.add(new BoolSetting.Builder().name("disable-when-none").defaultValue(false).build());

    public enum SmartMode {
        None, Always, Target
    }

    public KHoleFill() {
        super(Atlas.Konas, "k-hole-fill", "Automatically fill holes");
    }

    private Map<BlockPos, Long> renderPoses = new ConcurrentHashMap<>();

    private int tickCounter = 0;

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (!mc.isOnThread()) return;
        renderPoses.forEach((pos, time) -> {
            if (System.currentTimeMillis() - time > 500) {
                renderPoses.remove(pos);
            } else {
                event.renderer.box(pos, new Color(225, 255, 255, 70), new Color(233, 255, 136, 230), ShapeMode.Both, 1);
            }
        });
    }

    @EventHandler
    public void onUpdateWalkingPlayer(PlayerMoveEvent event) {
        if (jumpDisable.get() && mc.player.prevY < mc.player.getY()) {
            toggle();
        }

        if (tickCounter < actionInterval.get()) {
            tickCounter++;
        }

        if (tickCounter < actionInterval.get()) {
            return;
        }

        FindItemResult slot = InvUtils.findInHotbar(Items.OBSIDIAN);

        if (!slot.found() && onlyWebs.get()) {
            slot = InvUtils.findInHotbar(Items.COBWEB);
        }

        if (!slot.found()) {
            return;
        }

        List<BlockPos> holes = findHoles();

        if (smartMode.get() == SmartMode.Target && getNearestTarget() == null) return;

        int blocksPlaced = 0;

        while (blocksPlaced < actionShift.get()) {
            BlockPos pos = StreamSupport.stream(holes.spliterator(), false)
                .filter(this::isHole)
                .filter(p -> mc.player.getPos().distanceTo(new Vec3d(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5)) <= rangeXZ.get())
                .filter(p -> InteractionUtil.canPlaceBlock(p, strictDirection.get()))
                .min(Comparator.comparing(p -> mc.player.getPos().distanceTo(new Vec3d(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5))))
                .orElse(null);

            if (pos != null) {
                if (Block2Utils.place(pos, slot, rotate.get(), 50, false)) {
                    blocksPlaced++;
                    renderPoses.put(pos, System.currentTimeMillis());
                    InteractionUtil.ghostBlocks.put(pos, System.currentTimeMillis());
                    tickCounter = 0;
                    if (!mc.player.isOnGround()) return;
                } else {
                    return;
                }
            } else {
                if (disableWhenNone.get()) {
                    toggle();
                }
                return;
            }
        }
    }

    private List<BlockPos> findHoles() {
        List<BlockPos> positions = new ArrayList<>();
        BlockPos centerPos = mc.player.getBlockPos();
        int r = (int) Math.ceil(rangeXZ.get()) + 1;
        int h = rangeXZ.get().intValue();
        for (int i = centerPos.getX() - r; i < centerPos.getX() + r; i++) {
            for (int j = centerPos.getY() - h; j < centerPos.getY() + h; j++) {
                for (int k = centerPos.getZ() - r; k < centerPos.getZ() + r; k++) {
                    BlockPos pos = new BlockPos(i, j, k);
                    if (isHole(pos)) {
                        positions.add(pos);
                    }
                }
            }
        }
        return positions;
    }

    private boolean isValidItem(Item item) {
        if (item instanceof BlockItem) {
            if (onlyWebs.get()) {
                return ((BlockItem) item).getBlock() == Blocks.COBWEB;
            }
            return true;
        }
        return false;
    }

    private PlayerEntity getNearestTarget() {
        return mc.world.getPlayers().stream()
            .filter(e -> e != mc.player)
            .filter(e -> !Friends.get().isFriend(e))
            .filter(e -> mc.player.distanceTo(e) < targetRange.get())
            .min(Comparator.comparing(e -> mc.player.distanceTo(e)))
            .orElse(null);
    }

    public boolean validObi(BlockPos pos) {
        return !validBedrock(pos)
            && (mc.world.getBlockState(pos.add(0, -1, 0)).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(pos.add(0, -1, 0)).getBlock() == Blocks.BEDROCK)
            && (mc.world.getBlockState(pos.add(1, 0, 0)).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(pos.add(1, 0, 0)).getBlock() == Blocks.BEDROCK)
            && (mc.world.getBlockState(pos.add(-1, 0, 0)).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(pos.add(-1, 0, 0)).getBlock() == Blocks.BEDROCK)
            && (mc.world.getBlockState(pos.add(0, 0, 1)).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(pos.add(0, 0, 1)).getBlock() == Blocks.BEDROCK)
            && (mc.world.getBlockState(pos.add(0, 0, -1)).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(pos.add(0, 0, -1)).getBlock() == Blocks.BEDROCK)
            && mc.world.getBlockState(pos).getMaterial() == Material.AIR
            && mc.world.getBlockState(pos.add(0, 1, 0)).getMaterial() == Material.AIR
            && mc.world.getBlockState(pos.add(0, 2, 0)).getMaterial() == Material.AIR;
    }

    public boolean validBedrock(BlockPos pos) {
        return mc.world.getBlockState(pos.add(0, -1, 0)).getBlock() == Blocks.BEDROCK
            && mc.world.getBlockState(pos.add(1, 0, 0)).getBlock() == Blocks.BEDROCK
            && mc.world.getBlockState(pos.add(-1, 0, 0)).getBlock() == Blocks.BEDROCK
            && mc.world.getBlockState(pos.add(0, 0, 1)).getBlock() == Blocks.BEDROCK
            && mc.world.getBlockState(pos.add(0, 0, -1)).getBlock() == Blocks.BEDROCK
            && mc.world.getBlockState(pos).getMaterial() == Material.AIR
            && mc.world.getBlockState(pos.add(0, 1, 0)).getMaterial() == Material.AIR
            && mc.world.getBlockState(pos.add(0, 2, 0)).getMaterial() == Material.AIR;
    }

    public BlockPos validTwoBlockObiXZ(BlockPos pos) {
        if (
            (mc.world.getBlockState(pos.down()).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(pos.down()).getBlock() == Blocks.BEDROCK)
                && (mc.world.getBlockState(pos.west()).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(pos.west()).getBlock() == Blocks.BEDROCK)
                && (mc.world.getBlockState(pos.south()).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(pos.south()).getBlock() == Blocks.BEDROCK)
                && (mc.world.getBlockState(pos.north()).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(pos.north()).getBlock() == Blocks.BEDROCK)
                && mc.world.getBlockState(pos).getMaterial() == Material.AIR
                && mc.world.getBlockState(pos.up()).getMaterial() == Material.AIR
                && mc.world.getBlockState(pos.up(2)).getMaterial() == Material.AIR
                && (mc.world.getBlockState(pos.east().down()).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(pos.east().down()).getBlock() == Blocks.BEDROCK)
                && (mc.world.getBlockState(pos.east(2)).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(pos.east(2)).getBlock() == Blocks.BEDROCK)
                && (mc.world.getBlockState(pos.east().south()).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(pos.east().south()).getBlock() == Blocks.BEDROCK)
                && (mc.world.getBlockState(pos.east().north()).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(pos.east().north()).getBlock() == Blocks.BEDROCK)
                && mc.world.getBlockState(pos.east()).getMaterial() == Material.AIR
                && mc.world.getBlockState(pos.east().up()).getMaterial() == Material.AIR
                && mc.world.getBlockState(pos.east().up(2)).getMaterial() == Material.AIR
        ) {
            return validTwoBlockBedrockXZ(pos) == null ? new BlockPos(1, 0, 0) : null;
        } else if (
            (mc.world.getBlockState(pos.down()).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(pos.down()).getBlock() == Blocks.BEDROCK)
                && (mc.world.getBlockState(pos.west()).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(pos.west()).getBlock() == Blocks.BEDROCK)
                && (mc.world.getBlockState(pos.east()).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(pos.east()).getBlock() == Blocks.BEDROCK)
                && (mc.world.getBlockState(pos.north()).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(pos.north()).getBlock() == Blocks.BEDROCK)
                && mc.world.getBlockState(pos).getMaterial() == Material.AIR
                && mc.world.getBlockState(pos.up()).getMaterial() == Material.AIR
                && mc.world.getBlockState(pos.up(2)).getMaterial() == Material.AIR
                && (mc.world.getBlockState(pos.south().down()).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(pos.south().down()).getBlock() == Blocks.BEDROCK)
                && (mc.world.getBlockState(pos.south(2)).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(pos.south(2)).getBlock() == Blocks.BEDROCK)
                && (mc.world.getBlockState(pos.south().east()).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(pos.south().east()).getBlock() == Blocks.BEDROCK)
                && (mc.world.getBlockState(pos.south().west()).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(pos.south().west()).getBlock() == Blocks.BEDROCK)
                && mc.world.getBlockState(pos.south()).getMaterial() == Material.AIR
                && mc.world.getBlockState(pos.south().up()).getMaterial() == Material.AIR
                && mc.world.getBlockState(pos.south().up(2)).getMaterial() == Material.AIR
        ) {
            return validTwoBlockBedrockXZ(pos) == null ? new BlockPos(0, 0, 1) : null;
        }
        return null;
    }

    public BlockPos validTwoBlockBedrockXZ(BlockPos pos) {
        if (
            (mc.world.getBlockState(pos.down()).getBlock() == Blocks.BEDROCK)
                && (mc.world.getBlockState(pos.west()).getBlock() == Blocks.BEDROCK)
                && (mc.world.getBlockState(pos.south()).getBlock() == Blocks.BEDROCK)
                && (mc.world.getBlockState(pos.north()).getBlock() == Blocks.BEDROCK)
                && mc.world.getBlockState(pos).getMaterial() == Material.AIR
                && mc.world.getBlockState(pos.up()).getMaterial() == Material.AIR
                && mc.world.getBlockState(pos.up(2)).getMaterial() == Material.AIR
                && (mc.world.getBlockState(pos.east().down()).getBlock() == Blocks.BEDROCK)
                && (mc.world.getBlockState(pos.east(2)).getBlock() == Blocks.BEDROCK)
                && (mc.world.getBlockState(pos.east().south()).getBlock() == Blocks.BEDROCK)
                && (mc.world.getBlockState(pos.east().north()).getBlock() == Blocks.BEDROCK)
                && mc.world.getBlockState(pos.east()).getMaterial() == Material.AIR
                && mc.world.getBlockState(pos.east().up()).getMaterial() == Material.AIR
                && mc.world.getBlockState(pos.east().up(2)).getMaterial() == Material.AIR
        ) {
            return new BlockPos(1, 0, 0);
        } else if (
            (mc.world.getBlockState(pos.down()).getBlock() == Blocks.BEDROCK)
                && (mc.world.getBlockState(pos.west()).getBlock() == Blocks.BEDROCK)
                && (mc.world.getBlockState(pos.east()).getBlock() == Blocks.BEDROCK)
                && (mc.world.getBlockState(pos.north()).getBlock() == Blocks.BEDROCK)
                && mc.world.getBlockState(pos).getMaterial() == Material.AIR
                && mc.world.getBlockState(pos.up()).getMaterial() == Material.AIR
                && mc.world.getBlockState(pos.up(2)).getMaterial() == Material.AIR
                && (mc.world.getBlockState(pos.south().down()).getBlock() == Blocks.BEDROCK)
                && (mc.world.getBlockState(pos.south(2)).getBlock() == Blocks.BEDROCK)
                && (mc.world.getBlockState(pos.south().east()).getBlock() == Blocks.BEDROCK)
                && (mc.world.getBlockState(pos.south().west()).getBlock() == Blocks.BEDROCK)
                && mc.world.getBlockState(pos.south()).getMaterial() == Material.AIR
                && mc.world.getBlockState(pos.south().up()).getMaterial() == Material.AIR
                && mc.world.getBlockState(pos.south().up(2)).getMaterial() == Material.AIR
        ) {
            return new BlockPos(0, 0, 1);
        }
        return null;
    }

    public boolean isHole(BlockPos pos) {
        return validObi(pos) || validBedrock(pos);
    }
}
