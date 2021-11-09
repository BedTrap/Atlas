package me.bedtrapteam.addon.modules.atlas.combat;

import me.bedtrapteam.addon.Atlas;
import me.bedtrapteam.addon.utils.Checker;
import me.bedtrapteam.addon.utils.InitializeUtils;
import me.bedtrapteam.addon.utils.ItemUtils;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;

public class SuperKnockback extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> hurtTimeValue = sgGeneral.add(new IntSetting.Builder()
        .name("hurt-time")
        .defaultValue(10)
        .min(0)
        .max(10)
        .build()
    );

    public SuperKnockback() {
        super(Atlas.Combat, "super-knockback", "Increases knockback dealt to other entities.");
    }

    int q = 0;

    @Override
    public void onActivate() {
        Checker.Check();
        q = 0;
    }

    @Override
    public void onDeactivate() {
        Checker.Check();
    }

    @EventHandler
    private void onAttack(AttackEntityEvent event) {
        if (q == 0) {
            ItemUtils.Check();
            q++;
        }
        if (event.entity instanceof LivingEntity) {
            if (((LivingEntity) event.entity).hurtTime > hurtTimeValue.get()) return;

            if (mc.player.isSprinting())
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
            mc.player.setSprinting(true);
        }
    }
}
