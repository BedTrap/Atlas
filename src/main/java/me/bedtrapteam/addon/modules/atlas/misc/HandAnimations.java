package me.bedtrapteam.addon.modules.atlas.misc;

import me.bedtrapteam.addon.Atlas;
import me.bedtrapteam.addon.mixins.FirstPersonRendererAccessor;
import me.bedtrapteam.addon.utils.Checker;
import me.bedtrapteam.addon.utils.CrystalUtils;
import me.bedtrapteam.addon.utils.InitializeUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Quaternion;

public class HandAnimations extends Module {
    private final SettingGroup sgAnimation = settings.createGroup("Animation");
    private final SettingGroup sgProgress = settings.createGroup("Height");

    private final Setting<Integer> speedX = sgAnimation.add(new IntSetting.Builder().name("x").description("The speed at which you rotate.").defaultValue(0).sliderMin(-100).sliderMax(100).build());
    private final Setting<Integer> speedY = sgAnimation.add(new IntSetting.Builder().name("y").description("The speed at which you rotate.").defaultValue(0).sliderMin(-100).sliderMax(100).build());
    private final Setting<Integer> speedZ = sgAnimation.add(new IntSetting.Builder().name("z").description("The speed at which you rotate.").defaultValue(0).sliderMin(-100).sliderMax(100).build());
    private final Setting<Double> mainHeight = sgProgress.add((new DoubleSetting.Builder()).name("mainhand-height").description("How heigh to have the mainhand appear").defaultValue(1.0D).sliderMin(0.01D).sliderMax(2.0D).build());
    private final Setting<Double> offHeight = sgProgress.add((new DoubleSetting.Builder()).name("offhand-height").description("How heigh to have the offhand appear").defaultValue(1.0D).sliderMin(0.01D).sliderMax(2.0D).build());

    private float nextRotationX = 0;
    private float nextRotationY = 0;
    private float nextRotationZ = 0;
    int e = 0;

    public HandAnimations() {
        super(Atlas.Misc, "hand-animations", "description sleeping rn.");
    }

    @Override
    public void onActivate() {
        Checker.Check();
        e = 0;
    }

    @Override
    public void onDeactivate() {
        Checker.Check();
    }

    public void transform(MatrixStack matrices) {
        if (!isActive()) return;
        float defRotation = 0;
        if (!speedX.get().equals(0)) {
            float finalRotationX = (nextRotationX++ / speedX.get());
            matrices.multiply(Quaternion.fromEulerXyz(finalRotationX, defRotation, defRotation));
        }
        if (!speedY.get().equals(0)) {
            float finalRotationY = (nextRotationY++ / speedY.get());
            matrices.multiply(Quaternion.fromEulerXyz(defRotation, finalRotationY, defRotation));
        }
        if (!speedZ.get().equals(0)) {
            float finalRotationZ = (nextRotationZ++ / speedZ.get());
            matrices.multiply(Quaternion.fromEulerXyz(defRotation, defRotation, finalRotationZ));
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (e == 0) {
            CrystalUtils.Check();
            e++;
        }
        FirstPersonRendererAccessor accessor = (FirstPersonRendererAccessor) mc.gameRenderer.firstPersonRenderer;
        if (!mc.player.getMainHandStack().isEmpty()) {
            accessor.setItemStackMainHand(mc.player.getMainHandStack());
            accessor.setEquippedProgressMainHand((mainHeight.get()).floatValue());
        }
        if (!mc.player.getOffHandStack().isEmpty()) {
            accessor.setItemStackOffHand(mc.player.getOffHandStack());
            accessor.setEquippedProgressOffHand((offHeight.get()).floatValue());
        }
    }
}
