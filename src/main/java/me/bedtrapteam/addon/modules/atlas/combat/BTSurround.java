package me.bedtrapteam.addon.modules.atlas.combat;

import me.bedtrapteam.addon.Atlas;
import me.bedtrapteam.addon.utils.enchansed.Block2Utils;
import me.bedtrapteam.addon.utils.enchansed.Render2Utils;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.Burrow;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BTSurround extends Module {
    public enum Mode {
        Default,
        Instant,
        Packet
    }

    public enum BottomMode {
        Default,
        Instant,
        Packet,
        None
    }

    public enum PlaceMode {
        Default,
        Custom,
        None
    }

    public enum CrystalMode {
        Always,
        Legs,
        None
    }

    public enum HitMode {
        Default,
        Packet,
        Both
    }

    public enum TpMode {
        Default,
        Smooth,
        None
    }

    private final SettingGroup sgGeneral = settings.createGroup("General", true);
    private final SettingGroup sgAutoBurrow = settings.createGroup("Auto Burrow", false);
    private final SettingGroup sgPyramid = settings.createGroup("Pyramid", false);
    private final SettingGroup sgCrystalBreaker = settings.createGroup("Crystal Breaker", true);
    private final SettingGroup sgMisc = settings.createGroup("Misc", true);
    private final SettingGroup sgRender = settings.createGroup("Render", true);

    //  General

    private final Setting<Mode> placeMode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("surround-place-mode")
        .description("Place's obby by using choosen method.")
        .defaultValue(Mode.Default)
        .build()
    );

    private final Setting<BottomMode> bottomPlaceMode = sgGeneral.add(new EnumSetting.Builder<BottomMode>()
        .name("bottom-place-mode")
        .description("Place's obby under player by using choosen method.")
        .defaultValue(BottomMode.Default)
        .build()
    );

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("What blocks to use for surround.")
        .defaultValue(Collections.singletonList(Blocks.OBSIDIAN))
        .filter(this::blockFilter)
        .build()
    );

    private final Setting<Boolean> ignoreEntities = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-entities")
        .description("Tries to place on entities, recomended for high ping players.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> antiFall = sgGeneral.add(new BoolSetting.Builder()
        .name("anti-fall")
        .description("Prevent from falling when you bottom block getting destroyed.")
        .defaultValue(false)
        .visible(() -> bottomPlaceMode.get() != BottomMode.None)
        .build()
    );

    private final Setting<Boolean> antiSupport = sgGeneral.add(new BoolSetting.Builder()
        .name("anti-support")
        .description("Automatically places EChest under surround.")
        .defaultValue(true)
        .build()
    );

    //  Auto Burrow

    private final Setting<Boolean> autoBurrow = sgAutoBurrow.add(new BoolSetting.Builder()
        .name("auto-burrow")
        .description("Automatically activates Burrow module.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> autoBurrowDelay = sgAutoBurrow.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay for interact with burrow module.")
        .defaultValue(5)
        .min(0)
        .sliderMax(20)
        .visible(autoBurrow::get)
        .build()
    );

    private final Setting<Boolean> strictBurrow = sgAutoBurrow.add(new BoolSetting.Builder()
        .name("strict")
        .description("Prevent from odd jumps when Auto Burrow is enabled, very effective on strict")
        .defaultValue(false)
        .visible(autoBurrow::get)
        .build()
    );

    //  Pyramid

    private final Setting<PlaceMode> pyramidMode = sgPyramid.add(new EnumSetting.Builder<PlaceMode>()
        .name("pyramid-mode")
        .description("The way to place russian surround.")
        .defaultValue(PlaceMode.None)
        .build()
    );

    private final Setting<Integer> pyramidDelay = sgPyramid.add(new IntSetting.Builder()
        .name("delay")
        .description("The speed at which you rotate.")
        .defaultValue(0)
        .min(0)
        .sliderMax(20)
        .visible(() -> pyramidMode.get() != PlaceMode.None)
        .build()
    );

    //  Crystal Breaker

    private final Setting<CrystalMode> mode = sgCrystalBreaker.add(new EnumSetting.Builder<CrystalMode>()
        .name("crystal-breaker")
        .description("Breaks crystals in range.")
        .defaultValue(CrystalMode.Legs)
        .build()
    );

    private final Setting<CrystalMode> obbyPlaceMode = sgCrystalBreaker.add(new EnumSetting.Builder<CrystalMode>()
        .name("place-mode")
        .description("Places obsidian in crystal position.")
        .defaultValue(CrystalMode.Legs)
        .visible(() -> mode.get() != CrystalMode.None)
        .build()
    );

    private final Setting<HitMode> hitMode = sgCrystalBreaker.add(new EnumSetting.Builder<HitMode>()
        .name("hit-mode")
        .description("The way to interact with crystal.")
        .defaultValue(HitMode.Default)
        .visible(() -> mode.get() != CrystalMode.None)
        .build()
    );

    private final Setting<Boolean> onlyHole = sgCrystalBreaker.add(new BoolSetting.Builder()
        .name("hole-only")
        .description("Woks only if player is in hole.")
        .defaultValue(false)
        .visible(() -> mode.get() != CrystalMode.None)
        .build()
    );

    private final Setting<Double> breakRange = sgCrystalBreaker.add(new DoubleSetting.Builder()
        .name("range")
        .description("The speed at which you rotate.")
        .defaultValue(2.7)
        .min(0)
        .sliderMax(7)
        .visible(() -> mode.get() != CrystalMode.None)
        .build()
    );

    private final Setting<Integer> crystalAge = sgCrystalBreaker.add(new IntSetting.Builder()
        .name("crystal-age")
        .description("The speed at which you rotate.")
        .defaultValue(1)
        .min(0)
        .sliderMax(10)
        .visible(() -> mode.get() != CrystalMode.None)
        .build()
    );

    //  Misc

    private final Setting<TpMode> centerMode = sgMisc.add(new EnumSetting.Builder<TpMode>()
        .name("center-mode")
        .description("Teleports you to the center of the block.")
        .defaultValue(TpMode.Default)
        .build()
    );

    private final Setting<Integer> centerDelay = sgMisc.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay for teleporting to center.")
        .defaultValue(5)
        .min(1)
        .sliderMax(20)
        .visible(() -> centerMode.get() == TpMode.Smooth)
        .build()
    );

    private final Setting<Boolean> rotate = sgMisc.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Automatically faces towards the obsidian being placed.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> onlyOnGround = sgMisc.add(new BoolSetting.Builder()
        .name("only-on-ground")
        .description("Works only when you standing on blocks.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> disableOnJump = sgMisc.add(new BoolSetting.Builder()
        .name("disable-on-jump")
        .description("Automatically disables when you jump.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> disableOnTp = sgMisc.add(new BoolSetting.Builder()
        .name("disable-on-tp")
        .description("Automatically disables when you teleporting (like using chorus or pearl).")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> disableOnYChange = sgMisc.add(new BoolSetting.Builder()
        .name("disable-on-y-change")
        .description("Automatically disables when your y level (step, jumping, atc).")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders an overlay where blocks will be placed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> thick = sgRender.add(new BoolSetting.Builder()
        .name("thick")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color of the target block rendering.")
        .defaultValue(new SettingColor(197, 137, 232, 10))
        .build()
    );
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color of the target block rendering.")
        .defaultValue(new SettingColor(197, 137, 232))
        .build()
    );

    private final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBlocks = new ArrayList<>();
    private final BlockPos.Mutable renderPos = new BlockPos.Mutable();
    private final BlockPos.Mutable blockPos = new BlockPos.Mutable();
    private boolean crystalRemoved = false;
    int pyramidDelayLeft = pyramidDelay.get();
    BlockPos pos;
    private int ticks = 0;
    private boolean return_;
    private int centerDelayLeft;
    private BlockPos prevBreakPos;

    public BTSurround() {
        super(Atlas.Combat, "BT-surround", "Surrounds you in blocks to prevent you from taking lots of damage.");
    }

    @Override
    public void onActivate() {
        centerDelayLeft = 0;
        switch (centerMode.get()) {
            case Default -> PlayerUtils.centerPlayer();
            case Smooth -> {
                centerDelayLeft = centerDelay.get();
                if (inCenter()) {
                    centerDelayLeft = 0;
                }
            }
        }

        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();
    }

    @Override
    public void onDeactivate() {
        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        // Teleport
        if (centerMode.get() == TpMode.Smooth) {
            if (centerDelayLeft > 0) {
                pause();
                assert mc.player != null;
                double decrX = MathHelper.floor(mc.player.getX()) + 0.5 - mc.player.getX();
                double decrZ = MathHelper.floor(mc.player.getZ()) + 0.5 - mc.player.getZ();
                double sqrtPos = Math.sqrt(Math.pow(decrX, 2.0) + Math.pow(decrZ, 2.0));
                double div = Math.sqrt(0.5) / centerDelay.get();
                if (sqrtPos <= div) {
                    centerDelayLeft = 0;
                    double x = MathHelper.floor(mc.player.getX()) + 0.5;
                    double z = MathHelper.floor(mc.player.getZ()) + 0.5;
                    mc.player.setPosition(x, mc.player.getY(), z);
                    return;
                }
                double x = mc.player.getX();
                double z = mc.player.getZ();
                double incX = MathHelper.floor(mc.player.getX()) + 0.5;
                double incZ = MathHelper.floor(mc.player.getZ()) + 0.5;
                double incResult = 0.0;
                double decrResult = 0.0;
                double x_ = mc.player.getX();
                double z_ = mc.player.getZ();
                if (Math.sqrt(Math.pow(decrX, 2.0)) > Math.sqrt(Math.pow(decrZ, 2.0))) {
                    if (decrX > 0.0) {
                        incResult = 0.5 / centerDelay.get();
                    } else if (decrX < 0.0) {
                        incResult = -0.5 / centerDelay.get();
                    }
                    x_ = mc.player.getX() + incResult;
                    z_ = z(x, z, incX, incZ, x_);
                } else if (Math.sqrt(Math.pow(decrX, 2.0)) < Math.sqrt(Math.pow(decrZ, 2.0))) {
                    if (decrZ > 0.0) {
                        decrResult = 0.5 / centerDelay.get();
                    } else if (decrZ < 0.0) {
                        decrResult = -0.5 / centerDelay.get();
                    }
                    z_ = mc.player.getZ() + decrResult;
                    x_ = x(x, z, incX, incZ, z_);
                } else if (Math.sqrt(Math.pow(decrX, 2.0)) == Math.sqrt(Math.pow(decrZ, 2.0))) {
                    if (decrX > 0.0) {
                        incResult = 0.5 / (double) centerDelay.get();
                    } else if (decrX < 0.0) {
                        incResult = -0.5 / (double) centerDelay.get();
                    }
                    x_ = mc.player.getX() + incResult;
                    if (decrZ > 0.0) {
                        decrResult = 0.5 / (double) centerDelay.get();
                    } else if (decrZ < 0.0) {
                        decrResult = -0.5 / (double) centerDelay.get();
                    }
                    z_ = mc.player.getZ() + decrResult;
                }
                pause();
                mc.player.setPosition(x_, mc.player.getY(), z_);
            }
        }

        if (antiFall.get() && bottomPlaceMode.get() != BottomMode.None && Block2Utils.isSurrounded(mc.player)) {
            mc.player.setVelocity(mc.player.getVelocity().x, 0, mc.player.getVelocity().z);
        }

        renderBlocks.forEach(RenderBlock::tick);
        renderBlocks.removeIf(renderBlock -> renderBlock.ticks <= 0);

        if (blockFilter(mc.world.getBlockState(mc.player.getBlockPos().north()).getBlock())) {
            renderPos.set(mc.player.getBlockPos().north());
            renderBlocks.add(renderBlockPool.get().set(renderPos));
        }
        if (blockFilter(mc.world.getBlockState(mc.player.getBlockPos().south()).getBlock())) {
            renderPos.set(mc.player.getBlockPos().south());
            renderBlocks.add(renderBlockPool.get().set(renderPos));
        }
        if (blockFilter(mc.world.getBlockState(mc.player.getBlockPos().east()).getBlock())) {
            renderPos.set(mc.player.getBlockPos().east());
            renderBlocks.add(renderBlockPool.get().set(renderPos));
        }
        if (blockFilter(mc.world.getBlockState(mc.player.getBlockPos().west()).getBlock())) {
            renderPos.set(mc.player.getBlockPos().west());
            renderBlocks.add(renderBlockPool.get().set(renderPos));
        }

        if ((disableOnJump.get() && (mc.options.keyJump.isPressed() || mc.player.input.jumping)) || (disableOnYChange.get() && mc.player.prevY < mc.player.getY())) {
            toggle();
            return;
        }

        if (onlyOnGround.get() && !mc.player.isOnGround()) return;
        if (Modules.get().get(Burrow.class).isActive()) return;
        if (autoBurrow.get() && strictBurrow.get() && !mc.player.isOnGround()) return;

        if (Block2Utils.isSurrounded(mc.player) && autoBurrow.get()) {

            if (ticks < autoBurrowDelay.get()) {
                if (mc.world.getBlockState(mc.player.getBlockPos()).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(mc.player.getBlockPos()).getBlock() == Blocks.ENDER_CHEST)
                    return;
                ticks++;
            } else {
                Modules.get().get(Burrow.class).toggle();
                ticks = 0;
            }
        }

        // Place
        return_ = false;

        switch (bottomPlaceMode.get()) {
            case Default:
                boolean p1 = defaultPlace(0, -1, 0);
                break;
            case Instant:
                instantPlace(0, -1, 0);
                break;
            case Packet:
                packetPlace(0, -1, 0, Direction.UP);
            case None:
                break;
        }
        if (return_) return;

        switch (placeMode.get()) {
            case Default:
                boolean p2 = defaultPlace(1, 0, 0);
                if (return_) return;
                boolean p3 = defaultPlace(-1, 0, 0);
                if (return_) return;
                boolean p4 = defaultPlace(0, 0, 1);
                if (return_) return;
                boolean p5 = defaultPlace(0, 0, -1);
                if (return_) return;
                break;
            case Instant:
                instantPlace(1, 0, 0);
                instantPlace(-1, 0, 0);
                instantPlace(0, 0, 1);
                instantPlace(0, 0, -1);
                break;
            case Packet:
                packetPlace(1, 0, 0, Direction.WEST);
                packetPlace(-1, 0, 0, Direction.EAST);
                packetPlace(0, 0, 1, Direction.NORTH);
                packetPlace(0, 0, -1, Direction.SOUTH);
        }

        switch (pyramidMode.get()) {
            case Default:
                if (bottomPlaceMode.get() == BottomMode.None) {
                    boolean p0 = defaultPlace(0, -1, 0);
                    if (return_) return;
                }

                if (pyramidDelayLeft == 0) {
                    pyramidDelayLeft = pyramidDelay.get();
                } else {
                    pyramidDelayLeft--;
                    return;
                }

                boolean p1 = defaultPlace(0, -2, 0);
                if (return_) return;
                boolean p2 = defaultPlace(1, -1, 0);
                if (return_) return;
                boolean p3 = defaultPlace(-1, -1, 0);
                if (return_) return;
                boolean p4 = defaultPlace(0, -1, 1);
                if (return_) return;
                boolean p5 = defaultPlace(0, -1, -1);
                if (return_) return;
                boolean p6 = defaultPlace(1, 0, 1);
                if (return_) return;
                boolean p7 = defaultPlace(-1, 0, -1);
                if (return_) return;
                boolean p8 = defaultPlace(-1, 0, 1);
                if (return_) return;
                boolean p9 = defaultPlace(1, 0, -1);
                if (return_) return;
                boolean p10 = defaultPlace(2, 0, 0);
                if (return_) return;
                boolean p11 = defaultPlace(-2, 0, 0);
                if (return_) return;
                boolean p12 = defaultPlace(0, 0, 2);
                if (return_) return;
                boolean p13 = defaultPlace(0, 0, -2);
                if (return_) return;
                boolean p14 = defaultPlace(1, 1, 0);
                if (return_) return;
                boolean p15 = defaultPlace(-1, 1, 0);
                if (return_) return;
                boolean p16 = defaultPlace(0, 1, 1);
                if (return_) return;
                boolean p17 = defaultPlace(0, 1, -1);
                if (return_) return;
                boolean p18 = defaultPlace(1, 2, 0);
                if (return_) return;
                boolean p19 = defaultPlace(-1, 2, 0);
                if (return_) return;
                boolean p20 = defaultPlace(0, 2, 1);
                if (return_) return;
                boolean p21 = defaultPlace(0, 2, -1);
                if (return_) return;
                boolean p22 = defaultPlace(0, 2, 0);
                if (return_) return;
                boolean p23 = defaultPlace(0, 3, 0);
                if (return_) return;
                break;
            case Custom:
                break;
            case None:
                break;
        }

        if (antiSupport.get()) {
            boolean p6 = placeDown(1, -1, 0);
            if (return_) return;
            boolean p7 = placeDown(-1, -1, 0);
            if (return_) return;
            boolean p8 = placeDown(0, -1, 1);
            if (return_) return;
            boolean p9 = placeDown(0, -1, -1);
            if (return_) return;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        assert mc.player != null;
        assert mc.world != null;
        if (mode.get() == CrystalMode.None || (onlyHole.get() && !Block2Utils.isSurrounded(mc.player))) return;
        for (Entity crystal : mc.world.getEntities()) {
            if (crystal instanceof EndCrystalEntity && mc.player.distanceTo(crystal) < breakRange.get() && crystal.age >= crystalAge.get()) {
                if (mode.get() == CrystalMode.Legs && crystal.getBlockPos().getY() > mc.player.getBlockPos().getY())
                    return;
                pos = crystal.getBlockPos();
                switch (mode.get()) {
                    case Always -> attack(crystal);
                    case Legs -> {
                        if (pos.getY() <= mc.player.getBlockPos().getY())
                            attack(crystal);
                    }
                }
                crystalRemoved = true;
            } else if (crystalRemoved) {
                switch (obbyPlaceMode.get()) {
                    case Always -> place();
                    case Legs -> {
                        if (pos.getY() <= mc.player.getBlockPos().getY())
                            place();
                    }
                }
                crystalRemoved = false;
            }
        }
    }

    private void attack(Entity target) {
        switch (hitMode.get()) {
            case Default -> mc.interactionManager.attackEntity(mc.player, target);
            case Packet -> mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
            case Both -> {
                mc.interactionManager.attackEntity(mc.player, target);
                mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
            }
        }
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof TeleportConfirmC2SPacket && disableOnTp.get()) toggle();
    }

    private boolean blockFilter(Block block) {
        return block == Blocks.OBSIDIAN ||
            block == Blocks.CRYING_OBSIDIAN ||
            block == Blocks.NETHERITE_BLOCK ||
            block == Blocks.ENDER_CHEST ||
            block == Blocks.RESPAWN_ANCHOR;
    }

    private boolean defaultPlace(int x, int y, int z) {
        setBlockPos(x, y, z);
        BlockState blockState = mc.world.getBlockState(blockPos);

        if (!blockState.getMaterial().isReplaceable()) return true;

        if (ignoreEntities.get()) {
            if (BlockUtils.place(blockPos, InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem()))), rotate.get(), 100, false)) {
                return_ = true;
            }
        } else {
            if (BlockUtils.place(blockPos, InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem()))), rotate.get(), 100, true)) {
                return_ = true;
            }
        }
        return false;
    }

    private void packetPlace(int x, int y, int z, Direction direction) {
        BlockHitResult result = new BlockHitResult(mc.player.getPos(), direction, mc.player.getBlockPos().add(x, y, z), true);
        int prevSlot = mc.player.getInventory().selectedSlot;
        InvUtils.swap(InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem()))).getSlot(), false);
        mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result));
        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), direction, mc.player.getBlockPos().add(x, y, z), true));
        mc.player.getInventory().selectedSlot = prevSlot;
    }

    private void instantPlace(int x, int y, int z) {
        if (ignoreEntities.get()) {
            BlockUtils.place(mc.player.getBlockPos().add(x, y, z), InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem()))), rotate.get(), 100, false);
        } else {
            BlockUtils.place(mc.player.getBlockPos().add(x, y, z), InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem()))), rotate.get(), 100, true);
        }
    }

    private boolean placeDown(int x, int y, int z) {
        setBlockPos(x, y, z);
        BlockState blockState = mc.world.getBlockState(blockPos);

        if (!blockState.getMaterial().isReplaceable()) return true;

        if (BlockUtils.place(blockPos, InvUtils.findInHotbar(Items.ENDER_CHEST), rotate.get(), 100, true)) {
            return_ = true;
        }
        return false;
    }

    private void setBlockPos(int x, int y, int z) {
        blockPos.set(mc.player.getX() + x, mc.player.getY() + y, mc.player.getZ() + z);
    }

    public void place() {
        if (ignoreEntities.get()) {
            BlockUtils.place(pos, InvUtils.find(Items.OBSIDIAN), false, 50, false);
        } else {
            BlockUtils.place(pos, InvUtils.find(Items.OBSIDIAN), false, 50, true);
        }
    }

    private double z(double a, double b, double c, double d, double e) {
        return (e - a) * (d - b) / (c - a) + b;
    }

    private double x(double a, double b, double c, double d, double e) {
        return (e - b) * (c - a) / (d - b) + a;
    }

    private void pause() {
        mc.options.keyJump.setPressed(false);
        mc.options.keySprint.setPressed(false);
        mc.options.keyForward.setPressed(false);
        mc.options.keyBack.setPressed(false);
        mc.options.keyLeft.setPressed(false);
        mc.options.keyRight.setPressed(false);
    }

    private boolean inCenter() {
        if (mc.player == null) {
            return false;
        }
        if (mc.world == null) {
            return false;
        }
        if (mc.interactionManager == null) {
            return false;
        }
        int count = 0;
        if (mc.player.getBlockPos().equals(new BlockPos(mc.player.getX() - (mc.player.getWidth() + 0.1) / 2.0, mc.player.getY(), mc.player.getZ() - (mc.player.getWidth() + 0.1) / 2.0))) {
            count++;
        }
        if (mc.player.getBlockPos().equals(new BlockPos(mc.player.getX() + (mc.player.getWidth() + 0.1) / 2.0, mc.player.getY(), mc.player.getZ() + (mc.player.getWidth() + 0.1) / 2.0))) {
            count++;
        }
        if (mc.player.getBlockPos().equals(new BlockPos(mc.player.getX() - (mc.player.getWidth() + 0.1) / 2.0, mc.player.getY(), mc.player.getZ() + (mc.player.getWidth() + 0.1) / 2.0))) {
            count++;
        }
        if (mc.player.getBlockPos().equals(new BlockPos(mc.player.getX() + (mc.player.getWidth() + 0.1) / 2.0, mc.player.getY(), mc.player.getZ() - (mc.player.getWidth() + 0.1) / 2.0))) {
            count++;
        }
        return count == 4;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (render.get()) {
            renderBlocks.sort(Comparator.comparingInt(o -> -o.ticks));
            renderBlocks.forEach(renderBlock -> renderBlock.render(event, sideColor.get(), lineColor.get(), shapeMode.get(), thick.get()));
        }
    }

    public static class RenderBlock {
        public BlockPos.Mutable pos = new BlockPos.Mutable();
        public int ticks;

        public RenderBlock set(BlockPos blockPos) {
            pos.set(blockPos);
            ticks = 8;

            return this;
        }

        public void tick() {
            ticks--;
        }

        public void render(Render3DEvent event, Color sides, Color lines, ShapeMode shapeMode, boolean thick_block) {
            int preSideA = sides.a;
            int preLineA = lines.a;

            sides.a *= (double) ticks / 8;
            lines.a *= (double) ticks / 8;

            if (thick_block) {
                Render2Utils.thick_box(event, pos, sides, lines, shapeMode);
            } else {
                event.renderer.box(pos, sides, lines, shapeMode, 0);
            }

            sides.a = preSideA;
            lines.a = preLineA;
        }
    }
}
