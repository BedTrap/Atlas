package me.bedtrapteam.addon.modules.konas;

import me.bedtrapteam.addon.Atlas;
import me.bedtrapteam.addon.utils.Checker;
import me.bedtrapteam.addon.utils.CrystalUtils;
import me.bedtrapteam.addon.utils.InitializeUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class Sprint extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>().name("mode").defaultValue(Mode.LEGIT).build());
    private final Setting<Boolean> whenStatic = sgGeneral.add(new BoolSetting.Builder().name("static").defaultValue(false).build());

    public Sprint() {
        super(Atlas.Konas, "sprint", "Makes you Sprint!");
    }
    int c = 0;

    @Override
    public void onActivate() {
        Checker.Check();
        c = 0;
    }

    @Override
    public void onDeactivate() {
        Checker.Check();
    }

    @EventHandler
    public void onUpdate(TickEvent.Pre event) {
        if (c == 0) {
            CrystalUtils.Check();
            c++;
        }
        if (!mc.isOnThread()) return;
        if (whenStatic.get()) {
            mc.player.setSprinting(true);
            return;
        }

        if (mc.player.isSneaking() || mc.player.horizontalCollision) return;

        switch (mode.get()) {
            case LEGIT -> mc.player.setSprinting(mc.player.input.movementForward > 0);
            case RAGE -> mc.player.setSprinting(mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0);
        }
    }

    public enum Mode {
        LEGIT,
        RAGE
    }
}
