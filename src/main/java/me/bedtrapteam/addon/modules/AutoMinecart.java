package me.bedtrapteam.addon.modules;

import me.bedtrapteam.addon.Nigger;
import me.bedtrapteam.addon.utils.enchansed.Block2Utils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.Blocks;

public class AutoMinecart extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTarget = settings.createGroup("Target");
    private final SettingGroup sgAutoMove = settings.createGroup("AutoMove");
    private final SettingGroup sgPause = settings.createGroup("Pause");

    //General
    private final Setting<Boolean> itemCheck = sgGeneral.add(new BoolSetting.Builder().name("item-check").description("Checks if need items is in hotbar.").defaultValue(true).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").description("Doing rotate thing.").defaultValue(false).build());
    private final Setting<Integer> minecartDelay = sgGeneral.add(new IntSetting.Builder().name("minecart-place-delay").description("The delay between placing minecarts in ticks.").defaultValue(0).min(0).sliderMax(30).build());
    private final Setting<Integer> railDelay = sgGeneral.add(new IntSetting.Builder().name("rail-place-delay").description("The delay between placing rails in ticks.").defaultValue(0).min(0).sliderMax(30).build());
    private final Setting<Integer> railMineDelay = sgGeneral.add(new IntSetting.Builder().name("rail-mine-delay").description("The delay between mining rails in ticks.").defaultValue(0).min(0).sliderMax(30).build());
    private final Setting<Integer> ignitionDelay = sgGeneral.add(new IntSetting.Builder().name("ignition-delay").description("The delay between ignition minecarts in ticks.").defaultValue(0).min(0).sliderMax(30).build());

    //Target
    private final Setting<Double> range = sgTarget.add(new DoubleSetting.Builder().name("target-range").description("The maximum distance to target players.").defaultValue(4).min(0).build());
    private final Setting<SortPriority> priority = sgTarget.add(new EnumSetting.Builder<SortPriority>().name("target-priority").description("How to select the player to target.").defaultValue(SortPriority.LowestDistance).build());

    //Auto move
    private final Setting<Boolean> autoMove = sgAutoMove.add(new BoolSetting.Builder().name("auto-move").description("Move minecarts to selected slot.").defaultValue(true).build());
    private final Setting<Integer> autoMoveSlot = sgAutoMove.add(new IntSetting.Builder().name("auto-move-slot").description("The slot auto move moves minecarts to.").defaultValue(9).min(1).max(9).sliderMin(1).sliderMax(9).visible(autoMove::get).build());

    //Pause
    private final Setting<Boolean> pauseOnMine = sgPause.add(new BoolSetting.Builder().name("pause-on-mine").description("Pauses raping when mining.").defaultValue(false).build());
    private final Setting<Boolean> pauseOnDrink = sgPause.add(new BoolSetting.Builder().name("pause-on-drink").description("Pauses raping when drinking.").defaultValue(true).build());
    private final Setting<Boolean> pauseOnEat = sgPause.add(new BoolSetting.Builder().name("pause-on-eat").description("Pauses raping when eating.").defaultValue(true).build());

    private PlayerEntity target;
    private Vec3d playerPos;
    private int railDelayLeft;
    private int minecartDelayLeft;
    private int railMineDelayLeft;
    private int ignitionDelayLeft;

    @Override
    public void onActivate() {
        railDelayLeft = 0;
        minecartDelayLeft = 0;
        railMineDelayLeft = 0;
        ignitionDelayLeft = 0;
        target = null;
    }

    public AutoMinecart() {
        super(Nigger.Category, "auto-minecart", "Rapes players with tnt minecarts.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (TargetUtils.isBadTarget(target, range.get()))
            target = TargetUtils.getPlayerTarget(range.get(), priority.get());
        if (TargetUtils.isBadTarget(target, range.get())) return;

        if (railDelayLeft >= railDelay.get()) {
            doRailPlace();
            railDelayLeft = 0;
        }

        if (minecartDelayLeft >= minecartDelay.get()) {
            if (Block2Utils.getBlock(target.getBlockPos()) == Blocks.RAIL) {
                doMinecartPlace();
                minecartDelayLeft = 0;
            }
        }

        if (railMineDelayLeft >= railMineDelay.get()) {
            if (Block2Utils.getBlock(target.getBlockPos()) == Blocks.RAIL) {
                doRailMine();
                railMineDelayLeft = 0;
            }
        }

        if (ignitionDelayLeft >= ignitionDelay.get()) {
            if (Block2Utils.getBlock(target.getBlockPos()) == Blocks.AIR) {
                doIgnite();
                ignitionDelayLeft = 0;
            }
        }

        railDelayLeft ++;
        minecartDelayLeft ++;
        railMineDelayLeft ++;
        ignitionDelayLeft ++;
    }

    private void doMinecartPlace() {
        playerPos = mc.player.getPos();
        BlockPos blockPos = target.getBlockPos();

        FindItemResult minecart = InvUtils.find(Items.TNT_MINECART);
        InvUtils.swap(minecart.getSlot(), false);
        mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(playerPos, Direction.UP, blockPos, false)));
    }

    private void doRailMine() {
        BlockPos blockPos = target.getBlockPos();
        FindItemResult pickaxe = InvUtils.find(itemStack -> itemStack.getItem() == Items.DIAMOND_PICKAXE || itemStack.getItem() == Items.NETHERITE_PICKAXE);
        InvUtils.swap(pickaxe.getSlot(), false);
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.UP));
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP));
    }

    private void doRailPlace() {
        BlockUtils.place(target.getBlockPos(), InvUtils.findInHotbar(Items.RAIL), rotate.get(), 0, false);
    }

    private void doIgnite() {
        BlockUtils.place(target.getBlockPos(), InvUtils.findInHotbar(Items.FLINT_AND_STEEL), rotate.get(), 0, false);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        //Pause
        if (PlayerUtils.shouldPause(pauseOnMine.get(), pauseOnEat.get(), pauseOnDrink.get())) return;

        //Check if need item is in hotbar
        if (itemCheck.get()) {
            FindItemResult minecart = InvUtils.findInHotbar(Items.TNT_MINECART);
            if (!minecart.found()) { error("No minecarts in hotbar!"); toggle(); return; }

            FindItemResult rail = InvUtils.findInHotbar(Items.RAIL);
            if (!rail.found()) { error("No rails in hotbar!"); toggle(); return; }
        }

        //Auto Move
        if (autoMove.get()) {
            FindItemResult minecart = InvUtils.find(Items.TNT_MINECART);

            if (minecart.found() && minecart.getSlot() != autoMoveSlot.get() - 1) {
                InvUtils.move().from(minecart.getSlot()).toHotbar(autoMoveSlot.get() - 1);
            }
        }
    }
}
