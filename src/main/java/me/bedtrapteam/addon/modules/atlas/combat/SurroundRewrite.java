package me.bedtrapteam.addon.modules.atlas.combat;

import me.bedtrapteam.addon.Atlas;
import me.bedtrapteam.addon.utils.Timer;
import me.bedtrapteam.addon.utils.enchansed.Player2Utils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Blink;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SurroundRewrite extends Module {
    private static final Timer surroundInstanceDelay = new Timer();
    private BlockPos lastPos = new BlockPos(0, -100, 0);

    private int timeToStart = 0;
    private int ticks = 0;

    private final SettingGroup sgHorizontalExpanding = settings.createGroup("Horizontal Expanding");
    private final SettingGroup sgVerticalExpanding = settings.createGroup("Vertical Expanding");
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // Horizontal Expanding

    private final Setting<Mode> mode = sgHorizontalExpanding.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Determines how big the surround is.")
        .defaultValue(Mode.Normal)
        .build()
    );

    private final Setting<Keybind> bigKeyBind = sgHorizontalExpanding.add(new KeybindSetting.Builder()
        .name("force-big")
        .description("Toggles big surround when held.")
        .build()
    );

    private final Setting<Keybind> giantKeyBind = sgHorizontalExpanding.add(new KeybindSetting.Builder()
        .name("force-giant")
        .description("Toggles giant surround when held.")
        .build()
    );

    // Vertical Expanding

    private final Setting<Boolean> underHeight = sgVerticalExpanding.add(new BoolSetting.Builder()
        .name("under-height")
        .description("Places obsidian under the original surround blocks to prevent surround not placing on some servers.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Keybind> underHeightKeyBind = sgVerticalExpanding.add(new KeybindSetting.Builder()
        .name("force-under-height")
        .description("Toggles under height when held.")
        .build()
    );

    private final Setting<Boolean> doubleHeight = sgVerticalExpanding.add(new BoolSetting.Builder()
        .name("double-height")
        .description("Places obsidian on top of the original surround blocks to prevent people from face-placing you.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Keybind> doubleHeightKeyBind = sgVerticalExpanding.add(new KeybindSetting.Builder()
        .name("force-double-height")
        .description("Toggles double height when held.")
        .build()
    );

    // General

    private final Setting<Primary> primary = sgGeneral.add(new EnumSetting.Builder<Primary>()
        .name("primary-block")
        .description("Primary block to use.")
        .defaultValue(Primary.Obsidian)
        .build()
    );

    private final Setting<Boolean> allBlocks = sgGeneral.add(new BoolSetting.Builder()
        .name("blastproof-blocks-only")
        .description("Places blastproof blocks only.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay between block placements in ticks.")
        .defaultValue(0)
        .sliderMin(0)
        .sliderMax(10)
        .build()
    );

    private final Setting<Boolean> center = sgGeneral.add(new BoolSetting.Builder()
        .name("center")
        .description("Teleports you to the center of the block.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Keybind> centerKeyBind = sgGeneral.add(new KeybindSetting.Builder()
        .name("force-center")
        .description("Toggles center when held.")
        .build()
    );

    private final Setting<Boolean> onlyOnGround = sgGeneral.add(new BoolSetting.Builder()
        .name("only-on-ground")
        .description("Makes surround only work on ground.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> disableOnJump = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-on-jump")
        .description("Automatically disables when you jump.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> disableOnYChange = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-on-y-change")
        .description("Automatically disables when your y level (step, jumping, atc).")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> stayOnBlink = sgGeneral.add(new BoolSetting.Builder()
        .name("blinkers")
        .description("Surround stays on when you are in blink.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> placeOnCrystal = sgGeneral.add(new BoolSetting.Builder()
        .name("place-on-crystal")
        .description("Places the surround on end crystal placement.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Makes you rotate when placing.")
        .defaultValue(false)
        .build()
    );

    public SurroundRewrite() {
        super(Atlas.Combat, "surround-rewrite", "Surrounds you in blocks to prevent you from taking lots of damage.");
    }

    @Override
    public void onActivate() {
        lastPos = (mc.player.isOnGround() ? roundBlockPos(mc.player.getPos()) : mc.player.getBlockPos());
        if (center.get()) PlayerUtils.centerPlayer();
    }

    @Override
    public void onDeactivate() {
        ticks = 0;
        timeToStart = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if ((disableOnJump.get() && (mc.options.keyJump.isPressed() || mc.player.input.jumping)) || (disableOnYChange.get() && mc.player.prevY < mc.player.getY())) {
            ChatUtils.info(title, "You jumped, disabling...");
            toggle();
            return;
        }

        if ((centerKeyBind.get()).isPressed()) PlayerUtils.centerPlayer();

        final BlockPos roundedPos = roundBlockPos(mc.player.getPos());
        if (onlyOnGround.get() && !mc.player.isOnGround() && roundedPos.getY() <= lastPos.getY()) lastPos = roundBlockPos(mc.player.getPos());

        if (surroundInstanceDelay.passedMillis(timeToStart) && (mc.player.isOnGround() || !onlyOnGround.get())) {
            if (delay.get() != 0 && ticks++ % delay.get() != 0) return;
            if (!(Modules.get().get(Blink.class)).isActive() || !stayOnBlink.get()) {
                final AbstractClientPlayerEntity loc = mc.player;
                final BlockPos locRounded = roundBlockPos(loc.getPos());
                if (!lastPos.equals(loc.isOnGround() ? locRounded : loc.getBlockPos())) {
                    if (onlyOnGround.get() || loc.getPos().y > lastPos.getY() + 1.5 || ((Math.floor(loc.getPos().x) != lastPos.getX() || Math.floor(loc.getPos().z) != lastPos.getZ()) && loc.getPos().y > lastPos.getY() + 0.75) || (!mc.world.getBlockState(lastPos).getMaterial().isReplaceable() && loc.getBlockPos() != lastPos)) {
                        toggle();
                        return;
                    }

                    if (!onlyOnGround.get() && locRounded.getY() <= lastPos.getY()) lastPos = locRounded;
                }
            }

            final int obbyIndex = findBlock();
            if (obbyIndex == -1) return;
            final int prevSlot = mc.player.getInventory().selectedSlot;

            if (needsToPlace()) {
                for (final BlockPos pos : getPositions()) {
                    if (mc.world.getBlockState(pos).getMaterial().isReplaceable()) mc.player.getInventory().selectedSlot = obbyIndex;
                    if (Player2Utils.placeBlockMainHand(pos, rotate.get(), !onlyOnGround.get(), placeOnCrystal.get()) && delay.get() != 0) {
                        mc.player.getInventory().selectedSlot = prevSlot;
                        return;
                    }
                }

                mc.player.getInventory().selectedSlot = prevSlot;
            }
        }
    }

    private List<BlockPos> getPositions() {
        final List<BlockPos> positions = new ArrayList<>();
        if (!onlyOnGround.get()) add(positions, lastPos.down());

        if (underHeight.get() || (underHeightKeyBind.get()).isPressed()) {
            add(positions, lastPos.north().down());
            add(positions, lastPos.east().down());
            add(positions, lastPos.south().down());
            add(positions, lastPos.west().down());
        }

        add(positions, lastPos.north());
        add(positions, lastPos.east());
        add(positions, lastPos.south());
        add(positions, lastPos.west());

        if (doubleHeight.get() || (doubleHeightKeyBind.get()).isPressed()) {
            add(positions, lastPos.north().up());
            add(positions, lastPos.east().up());
            add(positions, lastPos.south().up());
            add(positions, lastPos.west().up());
        }

        if (mode.get() != Mode.Normal || (bigKeyBind.get()).isPressed() || (bigKeyBind.get()).isPressed()) {
            if (mc.world.getBlockState(lastPos.north()).getBlock() != Blocks.BEDROCK) add(positions, lastPos.north(2));
            if (mc.world.getBlockState(lastPos.east()).getBlock() != Blocks.BEDROCK) add(positions, lastPos.east(2));
            if (mc.world.getBlockState(lastPos.south()).getBlock() != Blocks.BEDROCK) add(positions, lastPos.south(2));
            if (mc.world.getBlockState(lastPos.west()).getBlock() != Blocks.BEDROCK) add(positions, lastPos.west(2));
        }

        if (mode.get() == Mode.Giant || (giantKeyBind.get()).isPressed()) {
            if (mc.world.getBlockState(lastPos.north()).getBlock() != Blocks.BEDROCK || mc.world.getBlockState(lastPos.east()).getBlock() != Blocks.BEDROCK) add(positions, lastPos.north().east());
            if (mc.world.getBlockState(lastPos.east()).getBlock() != Blocks.BEDROCK || mc.world.getBlockState(lastPos.south()).getBlock() != Blocks.BEDROCK) add(positions, lastPos.east().south());
            if (mc.world.getBlockState(lastPos.south()).getBlock() != Blocks.BEDROCK || mc.world.getBlockState(lastPos.west()).getBlock() != Blocks.BEDROCK) add(positions, lastPos.south().west());
            if (mc.world.getBlockState(lastPos.west()).getBlock() != Blocks.BEDROCK || mc.world.getBlockState(lastPos.north()).getBlock() != Blocks.BEDROCK) add(positions, lastPos.west().north());
        }

        return positions;
    }

    private boolean needsToPlace() {
        return anyAir(lastPos.down(), lastPos.north(), lastPos.east(), lastPos.south(), lastPos.west(), lastPos.north().up(), lastPos.east().up(), lastPos.south().up(), lastPos.west().up(), lastPos.north(2), lastPos.east(2), lastPos.south(2), lastPos.west(2), lastPos.north().east(), lastPos.east().south(), lastPos.south().west(), lastPos.west().north());
    }

    private void add(final List<BlockPos> list, final BlockPos pos) {
        if (mc.world.getBlockState(pos).isAir() && allAir(pos.north(), pos.east(), pos.south(), pos.west(), pos.up(), pos.down()) && onlyOnGround.get()) list.add(pos.down());
        list.add(pos);
    }

    private boolean allAir(final BlockPos... pos) {
        return Arrays.stream(pos).allMatch(blockPos -> mc.world.getBlockState(blockPos).isAir());
    }

    private boolean anyAir(final BlockPos... pos) {
        return Arrays.stream(pos).anyMatch(blockPos -> mc.world.getBlockState(blockPos).isAir());
    }

    private Block primaryBlock() {
        Block index = null;
        if (primary.get() == Primary.Obsidian) index = Blocks.OBSIDIAN;
        else if (primary.get() == Primary.Ender_Chest) index = Blocks.ENDER_CHEST;
        else if (primary.get() == Primary.Crying_Obsidian) index = Blocks.CRYING_OBSIDIAN;
        else if (primary.get() == Primary.Netherite_Block) index = Blocks.NETHERITE_BLOCK;
        else if (primary.get() == Primary.Ancient_Debris) index = Blocks.ANCIENT_DEBRIS;
        else if (primary.get() == Primary.Respawn_Anchor) index = Blocks.RESPAWN_ANCHOR;
        else if (primary.get() == Primary.Anvil) index = Blocks.ANVIL;

        return index;
    }

    private int findBlock() {
        int index = findBlockInHotbar(primaryBlock());
        if (index == -1 && allBlocks.get()) {
            if (index == -1) index = findBlockInHotbar(Blocks.OBSIDIAN);
            if (index == -1) index = findBlockInHotbar(Blocks.ENDER_CHEST);
            if (index == -1) index = findBlockInHotbar(Blocks.CRYING_OBSIDIAN);
            if (index == -1) index = findBlockInHotbar(Blocks.NETHERITE_BLOCK);
            if (index == -1) index = findBlockInHotbar(Blocks.ANCIENT_DEBRIS);
            if (index == -1) index = findBlockInHotbar(Blocks.RESPAWN_ANCHOR);
            if (index == -1) index = findBlockInHotbar(Blocks.ANVIL);
        }

        return index;
    }

    public static int findBlockInHotbar(final Block block) {
        return InvUtils.findInHotbar(new ItemStack(block).getItem()).getSlot();
    }

    public BlockPos roundBlockPos(Vec3d vec) {
        return new BlockPos(vec.x, Math.round(vec.y), vec.z);
    }

    public enum Mode {
        Normal,
        Big,
        Giant
    }

    public enum Primary {
        Obsidian,
        Ender_Chest,
        Crying_Obsidian,
        Netherite_Block,
        Ancient_Debris,
        Respawn_Anchor,
        Anvil;

        @Override
        public String toString() {
            return super.toString().replace("_", " ");
        }
    }
}
