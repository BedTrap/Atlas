package me.bedtrapteam.addon.modules.konas;

import me.bedtrapteam.addon.Atlas;
import me.bedtrapteam.addon.utils.InteractionUtil;
import me.bedtrapteam.addon.utils.Timer;
import me.bedtrapteam.addon.utils.enchansed.Block2Utils;
import me.bedtrapteam.addon.utils.enchansed.Inv2Utils;
import me.bedtrapteam.addon.utils.enchansed.Player2Utils;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.mixin.ClientPlayerEntityAccessor;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.ConcurrentHashMap;

public class KSurround extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAutoDisable = settings.createGroup("Auto Disable");

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").defaultValue(true).build());
    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder().name("swing").defaultValue(true).build());
    private final Setting<Integer> actionShift = sgGeneral.add(new IntSetting.Builder().name("action-shift").defaultValue(3).min(0).max(4).build());
    private final Setting<Integer> tickDelay = sgGeneral.add(new IntSetting.Builder().name("action-interval").defaultValue(0).min(0).max(5).build());
    private final Setting<Boolean> predict = sgGeneral.add(new BoolSetting.Builder().name("predict").defaultValue(true).build());
    private final Setting<Boolean> full = sgGeneral.add(new BoolSetting.Builder().name("full").defaultValue(false).build());
    private final Setting<Boolean> eChest = sgGeneral.add(new BoolSetting.Builder().name("EChests").defaultValue(false).build());
    private final Setting<Boolean> autoCenter = sgGeneral.add(new BoolSetting.Builder().name("center").defaultValue(true).build());
    private final Setting<Boolean> onlyWhileSneaking = sgAutoDisable.add(new BoolSetting.Builder().name("only-while-sneaking").defaultValue(false).build());
    private final Setting<Boolean> disableOnJump = sgAutoDisable.add(new BoolSetting.Builder().name("disable-on-jump").defaultValue(true).build());
    private final Setting<Boolean> disableOnTP = sgAutoDisable.add(new BoolSetting.Builder().name("disable-on-tp").defaultValue(true).build());
    private final Setting<Boolean> disableWhenDone = sgAutoDisable.add(new BoolSetting.Builder().name("disable-when-done").defaultValue(true).build());


    public KSurround() {
        super(Atlas.Konas,"k-surround",  "Places obsidian around you");
    }

    private static final Vec3d[] STRICT = {
        new Vec3d(1, 0, 0),
        new Vec3d(0, 0, 1),
        new Vec3d(-1, 0, 0),
        new Vec3d(0, 0, -1)
    };

    private static final Vec3d[] NORMAL = {
        new Vec3d(1, 0, 0),
        new Vec3d(0, 0, 1),
        new Vec3d(-1, 0, 0),
        new Vec3d(0, 0, -1),
        new Vec3d(1, -1, 0),
        new Vec3d(0, -1, 1),
        new Vec3d(-1, -1, 0),
        new Vec3d(0, -1, -1),
        new Vec3d(0, -1, 0)
    };

    private int offsetStep = 0;
    private int delayStep = 0;

    private Timer inactivityTimer = new Timer();

    private ConcurrentHashMap<BlockPos, Long> renderPoses = new ConcurrentHashMap<>();

    @Override
    public void onActivate() {
        if (mc.player == null || mc.world == null) {
            this.toggle();
            return;
        }

        if (autoCenter.get()) {
            Player2Utils.centerPlayerHorizontally();
        }
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (!mc.isOnThread()) return;
        renderPoses.forEach((pos, time) -> {
            if (System.currentTimeMillis() - time > 500) {
                renderPoses.remove(pos);
            }
        });
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket && disableOnTP.get()) {
            toggle();
        }
    }

    @EventHandler(priority = 70)
    public void onUpdateWalkingPlayer(PlayerMoveEvent event) {
        if (mc.player == null || mc.world == null) {
            toggle();
            return;
        }

        if (disableOnJump.get() && mc.options.keyJump.isPressed()) {
            toggle();
            return;
        }

        if (disableWhenDone.get() && inactivityTimer.hasPassed(650)) {
            toggle();
            return;
        }

        if (onlyWhileSneaking.get() && !mc.player.input.sneaking) return;

        if (delayStep < tickDelay.get()) {
            delayStep++;
            return;
        } else {
            delayStep = 0;
        }

        Vec3d[] offsetPattern = new Vec3d[0];
        int maxSteps = 0;

        if (full.get()) {
            offsetPattern = NORMAL;
            maxSteps = NORMAL.length;
        } else {
            offsetPattern = STRICT;
            maxSteps = STRICT.length;
        }

        int blocksPlaced = 0;

        while (blocksPlaced < actionShift.get()) {
            if (offsetStep >= maxSteps) {
                offsetStep = 0;
                break;
            }

            BlockPos offsetPos = new BlockPos(offsetPattern[offsetStep]);
            BlockPos targetPos = new BlockPos(mc.player.getPos()).add(offsetPos.getX(), offsetPos.getY(), offsetPos.getZ());

            FindItemResult slot;
            slot = getSlot();

            if (!slot.found()) {
                toggle();
                ChatUtils.info(title, "No Blocks Found, disabling surround!");
                return;
            }

            if (Block2Utils.place(targetPos, slot, rotate.get(),50, false)) {
                renderPoses.put(targetPos, System.currentTimeMillis());
                blocksPlaced++;
                inactivityTimer.reset();
                if (predict.get()) {
                    InteractionUtil.ghostBlocks.put(targetPos, System.currentTimeMillis());
                }
            }

            offsetStep++;
        }
    }

    private FindItemResult getSlot() {
        FindItemResult slot2 = InvUtils.findInHotbar(Items.OBSIDIAN);;

        if (eChest.get() && !slot2.found()) {
            slot2 = InvUtils.findInHotbar(Items.ENDER_CHEST);
        }

        return slot2;
    }
}
