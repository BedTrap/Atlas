package me.bedtrapteam.addon.modules.atlas.misc;

import me.bedtrapteam.addon.Atlas;
import me.bedtrapteam.addon.utils.Checker;
import me.bedtrapteam.addon.utils.InitializeUtils;
import me.bedtrapteam.addon.utils.ItemUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;

import meteordevelopment.orbit.EventHandler;

import net.minecraft.client.render.entity.PlayerModelPart;

import java.util.Random;

public class Derp extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("The amount of delay between flickering.")
        .defaultValue(5)
        .min(0)
        .sliderMax(30)
        .build()
    );

    private final Setting<Boolean> toggleAll = sgGeneral.add(new BoolSetting.Builder()
        .name("all-at-once")
        .description("Flicker everything.")
        .defaultValue(false)
        .onChanged(bool -> setAllSkinTrue())
        .build()
    );

    private final Setting<Boolean> hat = sgGeneral.add(new BoolSetting.Builder()
        .name("hat")
        .description("Flicker hat.")
        .defaultValue(true)
        .visible(() -> !(toggleAll.get()))
        .build()
    );

    private final Setting<Boolean> jacket = sgGeneral.add(new BoolSetting.Builder()
        .name("jacket")
        .description("Flicker jacket.")
        .defaultValue(true)
        .visible(() -> !(toggleAll.get()))
        .build()
    );

    private final Setting<Boolean> leftPants = sgGeneral.add(new BoolSetting.Builder()
        .name("left-pants")
        .description("Flicker left pants.")
        .defaultValue(true)
        .visible(() -> !(toggleAll.get()))
        .build()
    );

    private final Setting<Boolean> rightPants = sgGeneral.add(new BoolSetting.Builder()
        .name("right-pants")
        .description("Flicker right pants.")
        .defaultValue(true)
        .visible(() -> !(toggleAll.get()))
        .build()
    );

    private final Setting<Boolean> leftSleeve = sgGeneral.add(new BoolSetting.Builder()
        .name("left-sleeve")
        .description("Flicker left sleeve.")
        .defaultValue(true)
        .visible(() -> !(toggleAll.get()))
        .build()
    );

    private final Setting<Boolean> rightSleeve = sgGeneral.add(new BoolSetting.Builder()
        .name("right-sleeve")
        .description("Flicker right sleeve.")
        .defaultValue(true)
        .visible(() -> !(toggleAll.get()))
        .build()
    );

    private final Setting<Boolean> cape = sgGeneral.add(new BoolSetting.Builder()
        .name("cape")
        .description("Flicker cape.")
        .defaultValue(true)
        .visible(() -> !(toggleAll.get()))
        .build()
    );

    public Derp() {
        super(Atlas.Misc, "derp", "Flickers your skin.");
    }

    private int timer;
    int j = 0;
    Random RandomGen = new Random();

    @Override
    public void onDeactivate() {
        timer = 0;
        setAllSkinTrue();

        Checker.Check();
    }

    @Override
    public void onActivate() {
        Checker.Check();

        timer = 0;

        j = 0;
    }

    private void setAllSkinTrue() {
        for (PlayerModelPart modelPart : PlayerModelPart.values()) {
            mc.options.togglePlayerModelPart(modelPart, true);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (j == 0) {
            ItemUtils.Check();
            j++;
        }
        timer++;
        if (delay.get() > timer) {
            return;
        }

        if (toggleAll.get()) {
            for (PlayerModelPart modelPart : PlayerModelPart.values()) {
                mc.options.togglePlayerModelPart(modelPart, !(mc.options.isPlayerModelPartEnabled(modelPart)));
            }
            timer = 0;
        }
        else {
            if (hat.get())
                mc.options.togglePlayerModelPart(PlayerModelPart.HAT, RandomGen.nextBoolean());
            if (jacket.get())
                mc.options.togglePlayerModelPart(PlayerModelPart.JACKET, RandomGen.nextBoolean());
            if (leftPants.get())
                mc.options.togglePlayerModelPart(PlayerModelPart.LEFT_PANTS_LEG, RandomGen.nextBoolean());
            if (rightPants.get())
                mc.options.togglePlayerModelPart(PlayerModelPart.RIGHT_PANTS_LEG, RandomGen.nextBoolean());
            if (leftSleeve.get())
                mc.options.togglePlayerModelPart(PlayerModelPart.LEFT_SLEEVE, RandomGen.nextBoolean());
            if (rightSleeve.get())
                mc.options.togglePlayerModelPart(PlayerModelPart.RIGHT_SLEEVE, RandomGen.nextBoolean());
            if (cape.get())
                mc.options.togglePlayerModelPart(PlayerModelPart.CAPE, RandomGen.nextBoolean());
            timer = 0;
        }
    }
}
