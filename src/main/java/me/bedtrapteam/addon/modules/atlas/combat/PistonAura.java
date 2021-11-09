package me.bedtrapteam.addon.modules.atlas.combat;

import me.bedtrapteam.addon.Atlas;
import me.bedtrapteam.addon.utils.enchansed.Block2Utils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.AbstractButtonBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;

public class PistonAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("range")
        .description("Range for working module.")
        .defaultValue(5)
        .min(0)
        .sliderMax(7)
        .build()
    );

    private final Setting<Integer> piston_place_delay = sgGeneral.add(new IntSetting.Builder()
        .name("piston-place-delay")
        .description("Delay for placing piston.")
        .defaultValue(1)
        .min(0)
        .sliderMax(10)
        .build()
    );

    private final Setting<Integer> crystal_place_delay = sgGeneral.add(new IntSetting.Builder()
        .name("crystal-place-delay")
        .description("Delay for placing crystal.")
        .defaultValue(1)
        .min(0)
        .sliderMax(10)
        .build()
    );

    private final Setting<Integer> crystal_attack_delay = sgGeneral.add(new IntSetting.Builder()
        .name("crystal-attack-delay")
        .description("Delay for attacking crystals.")
        .defaultValue(1)
        .min(0)
        .sliderMax(10)
        .build()
    );

    private final Setting<Boolean> fast_mode = sgGeneral.add(new BoolSetting.Builder()
        .name("fast")
        .description("Tries to speed up module.")
        .defaultValue(true)
        .build()
    );

    public PistonAura() {
        super(Atlas.Combat, "piston-aura", "Automatically pushing crystals into enemy by piston.");
    }

    private PlayerEntity target;
    private BlockPos targetPos = null;
    private BlockPos crystalPos = null;
    private BlockPos itemPos = null;
    private BlockPos redstonePos = null;
    private FindItemResult obsidian, piston, crystal, button;
    private int timer1, timer2, timer3, timer4, timer5, timer6, timer7, timer8, timer9, timer10;
    private final int[][] timer = {
        {2, 3, 4, 5, 6, 7, 8, 10, 11, 12},
        {5, 9, 10, 14, 15, 20, 21, 23, 25, 26}
    };

    private int tick = 0;

    @Override
    public void onActivate() {
        target = TargetUtils.getPlayerTarget(5, SortPriority.LowestDistance);
        tick = 0;
        if (fast_mode.get()) {
            timer1 = timer[0][0];
            timer2 = timer[0][1];
            timer3 = piston_place_delay.get() + timer[0][2];
            timer4 = piston_place_delay.get() + timer[0][3];
            timer5 = piston_place_delay.get() + crystal_place_delay.get() + timer[0][4];
            timer6 = piston_place_delay.get() + crystal_place_delay.get() + timer[0][5];
            timer7 = piston_place_delay.get() + crystal_place_delay.get() + timer[0][6];
            timer8 = piston_place_delay.get() + crystal_place_delay.get() + crystal_attack_delay.get() + timer[0][7];
            timer9 = piston_place_delay.get() + crystal_place_delay.get() + crystal_attack_delay.get() + timer[0][8];
            timer10 = piston_place_delay.get() + crystal_place_delay.get() + crystal_attack_delay.get() + timer[0][9];
        } else {
            timer1 = timer[1][0];
            timer2 = timer[1][1];
            timer3 = piston_place_delay.get() + timer[1][2];
            timer4 = piston_place_delay.get() + timer[1][3];
            timer5 = piston_place_delay.get() + crystal_place_delay.get() + timer[1][4];
            timer6 = piston_place_delay.get() + crystal_place_delay.get() + timer[1][5];
            timer7 = piston_place_delay.get() + crystal_place_delay.get() + timer[1][6];
            timer8 = piston_place_delay.get() + crystal_place_delay.get() + crystal_attack_delay.get() + timer[1][7];
            timer9 = piston_place_delay.get() + crystal_place_delay.get() + crystal_attack_delay.get() + timer[1][8];
            timer10 = piston_place_delay.get() + crystal_place_delay.get() + crystal_attack_delay.get() + timer[1][9];
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        obsidian = InvUtils.findInHotbar(Items.OBSIDIAN);
        piston = InvUtils.findInHotbar(Items.PISTON, Items.STICKY_PISTON);
        crystal = InvUtils.findInHotbar(Items.END_CRYSTAL);
        button = getButton();
        checkItems();
        if (mc.player == null && mc.world == null) return;
        if (target == null) {
            ChatUtils.info("Target is null.");
            toggle();
        } else if (obsidian.found() && button.found() && piston.found() && crystal.found()) {
            targetPos = target.getBlockPos();
            crystalPos = targetPos.up();
            redstonePos = findButton(range.get() + 2);
            // Check
            if (mc.player.distanceTo(target) > range.get()) {
                ChatUtils.info("Target is out of reach distance.");
                toggle();
            }
            if (target.isDead()) {
                ChatUtils.info("Target got naed.");
                toggle();
            }
            // Tick update
            tick++;
            if (checkSide(targetPos.west(), Direction.WEST)) {
                itemPos = targetPos.up().west();
                // Rotate
                Rotations.rotate(90, 0);
                // Obsidian
                if (tick == timer1) {
                    if (canBePlaced(targetPos.west())) {
                        BlockUtils.place(targetPos.west(), obsidian, 100);
                    } else tick = timer2;
                }
                // Piston
                if (tick == timer3) {
                    if (canBePlaced(targetPos.up().west().west())) {
                        BlockHitResult result = new BlockHitResult(mc.player.getPos(), Direction.UP, targetPos.up().west().west(), false);
                        InvUtils.swap(piston.getSlot(), true);
                        mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result));
                    } else tick = timer4;
                }
                // Crystal
                if (tick == timer5) {
                    placeCrystal(targetPos.west());
                }
                // Redstone
                if (tick == timer6) {
                    for (Entity item : mc.world.getEntities()) {
                        if (item instanceof ItemEntity && (item.getBlockPos().equals(itemPos))) {
                            return;
                        }
                    }
                    if (canBePlaced(targetPos.up().west().west().west())) {
                        BlockUtils.place(targetPos.up().west().west().west(), button, 100);
                    }
                }
                // Click
                if (tick == timer7) {
                    if (redstonePos != null) clickButton(redstonePos);
                }
                // Attack Crystal
                if (tick >= timer8 && tick <= timer9) {
                    attackCrystal();
                    if (mc.world.getBlockState(targetPos.up().west().west().west()).getBlock() == Blocks.STONE_BUTTON) {
                        mc.interactionManager.updateBlockBreakingProgress(targetPos.up().west().west().west(), Direction.UP);
                    }
                }
                if (tick >= timer10) {
                    tick = timer1;
                }
            } else if (checkSide(targetPos.north(), Direction.NORTH)) {
                itemPos = targetPos.up().north();
                // Rotate
                Rotations.rotate(180, 0);
                // Obsidian
                if (tick == timer1) {
                    if (canBePlaced(targetPos.north())) {
                        BlockUtils.place(targetPos.north(), obsidian, 100);
                    } else tick = timer2;
                }
                // Piston
                if (tick == timer3) {
                    if (canBePlaced(targetPos.up().north().north())) {
                        BlockHitResult result = new BlockHitResult(mc.player.getPos(), Direction.UP, targetPos.up().north().north(), false);
                        InvUtils.swap(piston.getSlot(), true);
                        mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result));
                    } else tick = timer4;
                }
                // Crystal
                if (tick == timer5) {
                    placeCrystal(targetPos.north());
                }
                // Redstone
                if (tick == timer6) {
                    for (Entity item : mc.world.getEntities()) {
                        if (item instanceof ItemEntity && (item.getBlockPos().equals(itemPos))) {
                            return;
                        }
                    }
                    if (canBePlaced(targetPos.up().north().north().north())) {
                        BlockUtils.place(targetPos.up().north().north().north(), button, 100);
                    }
                }
                // Click
                if (tick == timer7) {
                    if (redstonePos != null) clickButton(redstonePos);
                }
                // Attack Crystal
                if (tick >= timer8 && tick <= timer9) {
                    attackCrystal();
                    if (mc.world.getBlockState(targetPos.up().north().north().north()).getBlock() == Blocks.STONE_BUTTON) {
                        mc.interactionManager.updateBlockBreakingProgress(targetPos.up().north().north().north(), Direction.UP);
                    }
                }
                if (tick >= timer10) {
                    tick = timer1;
                }
            } else if (checkSide(targetPos.east(), Direction.EAST)) {
                itemPos = targetPos.up().east();
                // Rotate
                Rotations.rotate(270, 0);
                // Obsidian
                if (tick == timer1) {
                    if (canBePlaced(targetPos.east())) {
                        BlockUtils.place(targetPos.east(), obsidian, 100);
                    } else tick = timer2;
                }
                // Piston
                if (tick == timer3) {
                    if (canBePlaced(targetPos.up().east().east())) {
                        BlockHitResult result = new BlockHitResult(mc.player.getPos(), Direction.UP, targetPos.up().east().east(), false);
                        InvUtils.swap(piston.getSlot(), true);
                        mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result));
                    } else tick = timer4;
                }
                // Crystal
                if (tick == timer5) {
                    placeCrystal(targetPos.east());
                }
                // Redstone
                if (tick == timer6) {
                    for (Entity item : mc.world.getEntities()) {
                        if (item instanceof ItemEntity && (item.getBlockPos().equals(itemPos))) {
                            return;
                        }
                    }
                    if (canBePlaced(targetPos.up().east().east().east())) {
                        BlockUtils.place(targetPos.up().east().east().east(), button, 100);
                    }
                }
                // Click
                if (tick == timer7) {
                    if (redstonePos != null) clickButton(redstonePos);
                }
                // Attack Crystal
                if (tick >= timer8 && tick <= timer9) {
                    attackCrystal();
                    if (mc.world.getBlockState(targetPos.up().east().east().east()).getBlock() == Blocks.STONE_BUTTON) {
                        mc.interactionManager.updateBlockBreakingProgress(targetPos.up().east().east().east(), Direction.UP);
                    }
                }
                if (tick >= timer10) {
                    tick = timer1;
                }
            } else if (checkSide(targetPos.south(), Direction.SOUTH)) {
                itemPos = targetPos.up().south();
                // Rotate
                Rotations.rotate(0, 0);
                // Obsidian
                if (tick == timer1) {
                    if (canBePlaced(targetPos.south())) {
                        BlockUtils.place(targetPos.south(), obsidian, 100);
                    } else tick = timer2;
                }
                // Piston
                if (tick == timer3) {
                    if (canBePlaced(targetPos.up().south().south())) {
                        BlockHitResult result = new BlockHitResult(mc.player.getPos(), Direction.UP, targetPos.up().south().south(), false);
                        InvUtils.swap(piston.getSlot(), true);
                        mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result));
                    } else tick = timer4;
                }
                // Crystal
                if (tick == timer5) {
                    placeCrystal(targetPos.south());
                }
                // Redstone
                if (tick == timer6) {
                    for (Entity item : mc.world.getEntities()) {
                        if (item instanceof ItemEntity && (item.getBlockPos().equals(itemPos))) {
                            return;
                        }
                    }
                    if (canBePlaced(targetPos.up().south().south().south())) {
                        BlockUtils.place(targetPos.up().south().south().south(), button, 100);
                    }
                }
                // Click
                if (tick == timer7) {
                    if (redstonePos != null) clickButton(redstonePos);
                }
                // Attack Crystal
                if (tick >= timer8 && tick <= timer9) {
                    attackCrystal();
                    if (mc.world.getBlockState(targetPos.up().south().south().south()).getBlock() == Blocks.STONE_BUTTON) {
                        mc.interactionManager.updateBlockBreakingProgress(targetPos.up().south().south().south(), Direction.UP);
                    }
                }
                if (tick >= timer10) {
                    tick = timer1;
                }
            }
        }
    }

    private boolean checkSide(BlockPos pos, Direction direction) {
        int sideInt = 0;
        BlockPos p = pos.up();

        if (bs(p, Blocks.AIR) ||
            bs(p, Blocks.MOVING_PISTON) ||
            bs(p, Blocks.PISTON_HEAD) ||
            bs(p, Blocks.PISTON) ||
            bs(p, Blocks.STICKY_PISTON)) sideInt += 1;

        for (Direction dir : Direction.values()) {
            if (dir != direction) return false;

            if (bs(p, Blocks.OBSIDIAN) ||
                bs(p, Blocks.BEDROCK)) return false;
            if ((bs(p.offset(dir), Blocks.AIR) ||
                bs(p.offset(dir), Blocks.PISTON) ||
                bs(p.offset(dir), Blocks.STICKY_PISTON))) sideInt += 1;
            if (bs(p.offset(dir).offset(dir), Blocks.AIR) ||
                bs(p.offset(dir).offset(dir),
                    Blocks.STONE_BUTTON,
                    Blocks.ACACIA_BUTTON,
                    Blocks.OAK_BUTTON,
                    Blocks.SPRUCE_BUTTON,
                    Blocks.BIRCH_BUTTON,
                    Blocks.JUNGLE_BUTTON,
                    Blocks.DARK_OAK_BUTTON,
                    Blocks.CRIMSON_BUTTON,
                    Blocks.WARPED_BUTTON)) sideInt += 1;
        }
        return sideInt == 3;
    }

//    private boolean checkSide(BlockPos pos, Direction direction) {
//        int sideInt = 0;
//        if (bs(pos.up(), Blocks.AIR) ||
//            bs(pos.up(), Blocks.MOVING_PISTON) ||
//            bs(pos.up(), Blocks.PISTON_HEAD) ||
//            bs(pos.up(), Blocks.PISTON) ||
//            bs(pos.up(), Blocks.STICKY_PISTON)) sideInt += 1;
//        switch (direction) {
//            case WEST -> {
//                if (bs(pos.up(), Blocks.OBSIDIAN) || bs(pos.up(), Blocks.BEDROCK)) return false;
//                if ((bs(pos.up().west(), Blocks.AIR) ||
//                    bs(pos.up().west(), Blocks.PISTON) ||
//                    bs(pos.up().west(), Blocks.STICKY_PISTON))) sideInt += 1;
//                if (bs(pos.up().west().west(), Blocks.AIR) ||
//                    bs(pos.up().west().west(), Blocks.STONE_BUTTON)) sideInt += 1;
//            }
//            case NORTH -> {
//                if (bs(pos.up(), Blocks.OBSIDIAN) || bs(pos.up(), Blocks.BEDROCK)) return false;
//                if ((bs(pos.up().north(), Blocks.AIR) ||
//                    bs(pos.up().north(), Blocks.PISTON) ||
//                    bs(pos.up().north(), Blocks.STICKY_PISTON))) sideInt += 1;
//                if (bs(pos.up().north().north(), Blocks.AIR) ||
//                    bs(pos.up().north().north(), Blocks.STONE_BUTTON)) sideInt += 1;
//            }
//            case EAST -> {
//                if (bs(pos.up(), Blocks.OBSIDIAN) || bs(pos.up(), Blocks.BEDROCK)) return false;
//                if ((bs(pos.up().east(), Blocks.AIR) ||
//                    bs(pos.up().east(), Blocks.PISTON) ||
//                    bs(pos.up().east(), Blocks.STICKY_PISTON))) sideInt += 1;
//                if (bs(pos.up().east().east(), Blocks.AIR) ||
//                    bs(pos.up().east().east(), Blocks.STONE_BUTTON)) sideInt += 1;
//            }
//            case SOUTH -> {
//                if (bs(pos.up(), Blocks.OBSIDIAN) || bs(pos.up(), Blocks.BEDROCK)) return false;
//                if ((bs(pos.up().south(), Blocks.AIR) ||
//                    bs(pos.up().south(), Blocks.PISTON) ||
//                    bs(pos.up().south(), Blocks.STICKY_PISTON))) sideInt += 1;
//                if (bs(pos.up().south().south(), Blocks.AIR) ||
//                    bs(pos.up().south().south(), Blocks.STONE_BUTTON)) sideInt += 1;
//            }
//        }
//        return sideInt == 3;
//    }

    private boolean bs(BlockPos blockPos, Block... block) {
        for (Block b : block) {
            return mc.world.getBlockState(blockPos).getBlock() == b;
        }
        return false;
    }

    private boolean canBePlaced(BlockPos blockPos) {
        return mc.world.getBlockState(blockPos).isAir();
    }

    private void placeCrystal(BlockPos pos) {
        InvUtils.swap(crystal.getSlot(), true);
        if (mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL) {
            mc.interactionManager.interactBlock(mc.player, mc.world, Hand.OFF_HAND, new BlockHitResult(mc.player.getPos(), Direction.DOWN, pos, true));
        } else {
            mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.DOWN, pos, true));
        }
    }

    private void attackCrystal() {
        for (Entity crystal : mc.world.getEntities()) {
            if (crystal instanceof EndCrystalEntity &&
                (crystal.getBlockPos().equals(crystalPos) ||
                    crystal.getBlockPos().equals(crystalPos.west()) ||
                    crystal.getBlockPos().equals(crystalPos.east()) ||
                    crystal.getBlockPos().equals(crystalPos.south()) ||
                    crystal.getBlockPos().equals(crystalPos.north()))) {
                mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
            }
        }
    }

    private void checkItems() {
        if (!obsidian.found() || !button.found() || !piston.found() || !crystal.found()) {
            cantFind();
            toggle();
        }
    }

    private FindItemResult getButton() {
        FindItemResult button = InvUtils.findInHotbar(Blocks.ACACIA_BUTTON.asItem());
        if (!button.found()) button = InvUtils.findInHotbar(Blocks.STONE_BUTTON.asItem());
        if (!button.found()) button = InvUtils.findInHotbar(Blocks.OAK_BUTTON.asItem());
        if (!button.found()) button = InvUtils.findInHotbar(Blocks.SPRUCE_BUTTON.asItem());
        if (!button.found()) button = InvUtils.findInHotbar(Blocks.BIRCH_BUTTON.asItem());
        if (!button.found()) button = InvUtils.findInHotbar(Blocks.JUNGLE_BUTTON.asItem());
        if (!button.found()) button = InvUtils.findInHotbar(Blocks.DARK_OAK_BUTTON.asItem());
        if (!button.found()) button = InvUtils.findInHotbar(Blocks.CRIMSON_BUTTON.asItem());
        if (!button.found()) button = InvUtils.findInHotbar(Blocks.WARPED_BUTTON.asItem());
        return button;
    }

    private void cantFind() {
        ArrayList<String> items = new ArrayList<>();
        if (!obsidian.found()) items.add("obsidian");
        if (!piston.found()) items.add("piston");
        if (!crystal.found()) items.add("crystal");
        if (!button.found()) items.add("button");
        ChatUtils.info("Can't find items in hotbar: " + items.toString()
            .replace("[", "")
            .replace("]", ""));
        items.clear();
    }

    // Taken from GhostTypes github
    public void clickButton(BlockPos buttonPos) {
        Vec3d buttonVec = new Vec3d(buttonPos.getX(), buttonPos.getY(), buttonPos.getZ());
        BlockHitResult button = new BlockHitResult(buttonVec, Direction.UP, buttonPos, false);
        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, button);
    }

    // Taken from GhostTypes github
    public BlockPos findButton(int range) {
        for (BlockPos blockPos : Block2Utils.getSphere(mc.player.getBlockPos(), range, range))
            if (mc.world.getBlockState(blockPos).getBlock() instanceof AbstractButtonBlock) {
                return blockPos;
            }
        return null;
    }
}
