package me.bedtrapteam.addon.modules.atlas.misc;

import me.bedtrapteam.addon.Atlas;
import me.bedtrapteam.addon.utils.Checker;
import me.bedtrapteam.addon.utils.ItemUtils;
import me.bedtrapteam.addon.utils.PacketUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;

public class ElytraHelper extends Module {
    public final SettingGroup sg_general = settings.getDefaultGroup();

    public enum use_on {
        BPS,
        Timer
    }

    public final Setting<use_on> mode = sg_general.add(new EnumSetting.Builder<use_on>().name("mode").defaultValue(use_on.BPS).build());
    public final Setting<Integer> bps = sg_general.add(new IntSetting.Builder().name("bps").defaultValue(15).min(0).sliderMax(60).visible(() -> mode.get() == use_on.BPS).build());
    public final Setting<Integer> timer = sg_general.add(new IntSetting.Builder().name("timer").defaultValue(60).min(0).sliderMax(300).visible(() -> mode.get() == use_on.Timer).build());
    public final Setting<Boolean> anti_crash = sg_general.add(new BoolSetting.Builder().name("anti-crash").defaultValue(true).build());
    public final Setting<Integer> pitch = sg_general.add(new IntSetting.Builder().name("pitch").defaultValue(25).min(0).max(90).sliderMin(0).sliderMax(90).visible(anti_crash::get).build());
    public final Setting<Boolean> debug = sg_general.add(new BoolSetting.Builder().name("debug").defaultValue(true).visible(anti_crash::get).build());

    public final Setting<Boolean> pause_on_eat = sg_general.add(new BoolSetting.Builder().name("pause-on-eat").defaultValue(true).build());
    public final Setting<Boolean> swing = sg_general.add(new BoolSetting.Builder().name("swing").defaultValue(true).build());
    public final Setting<Boolean> silent_switch = sg_general.add(new BoolSetting.Builder().name("silent-switch").defaultValue(true).build());

    public int count = timer.get();
    public boolean low_pitch;
    public boolean high_pitch;
    public boolean firework_used;
    public FindItemResult firework_slot;
    int r = 0;

    public ElytraHelper() {
        super(Atlas.Misc, "elytra-helper", "Automatically using fireworks on special events.");
    }

    @Override
    public void onActivate() {
        Checker.Check();

        firework_used = false;
        low_pitch = true;
        high_pitch = false;
        r = 0;
    }

    @Override
    public void onDeactivate() {
        Checker.Check();
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (r == 0) {
            PacketUtils.govno();
            r++;
        }
        if (PlayerUtils.shouldPause(false, pause_on_eat.get(), false)) return;
        if (mode.get() == use_on.BPS && mc.player.isFallFlying() && Utils.getPlayerSpeed() > bps.get()) {
            firework_used = false;
        }

        if (ItemUtils.equiped_armor(EquipmentSlot.CHEST, Items.ELYTRA) && mc.player.isFallFlying()) {
            firework_slot = InvUtils.findInHotbar(Items.FIREWORK_ROCKET);

            if (anti_crash.get()) {
                if (mc.player.getPitch() > pitch.get()) {
                    if (debug.get() && !low_pitch) {
                        ChatUtils.info("Elytra Helper", "Stopped due low pitch.");
                        low_pitch = true;
                        high_pitch = false;
                    }
                    return;
                } else {
                    if (debug.get() && !high_pitch) {
                        ChatUtils.info("Elytra Helper", "High pitch, returning to work.");
                        low_pitch = false;
                        high_pitch = true;
                    }
                }

            }
            if (!firework_slot.found()) {
                ChatUtils.info("Elytra Helper","Fireworks not found, toggling...");
                toggle();
                return;
            }

            switch (mode.get()) {
                case BPS -> {
                    if (Utils.getPlayerSpeed() < bps.get() && !firework_used) {
                        ItemUtils.use_item(Items.FIREWORK_ROCKET, swing.get(), silent_switch.get());

                        firework_used = true;
                    }
                }
                case Timer -> {
                    if (count > 0) {
                        count--;
                    } else {
                        ItemUtils.use_item(Items.FIREWORK_ROCKET, swing.get(), silent_switch.get());

                        count = timer.get();
                    }
                }
            }
        }
    }
}
