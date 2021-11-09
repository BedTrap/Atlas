package me.bedtrapteam.addon.modules.konas;

import me.bedtrapteam.addon.Atlas;
import me.bedtrapteam.addon.utils.Checker;
import me.bedtrapteam.addon.utils.DamageCalculator;
import me.bedtrapteam.addon.utils.InitializeUtils;
import me.bedtrapteam.addon.utils.Timer;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.screen.slot.SlotActionType;

public class OffHand extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> force = sgGeneral.add(new BoolSetting.Builder().name("force").defaultValue(false).build());
    private final Setting<Integer> health = sgGeneral.add(new IntSetting.Builder().name("health").defaultValue(12).min(1).max(20).build());
    private final Setting<Double> delay = sgGeneral.add(new DoubleSetting.Builder().name("delay").defaultValue(0).min(0).max(5).build());
    private final Setting<Action> action = sgGeneral.add(new EnumSetting.Builder<Action>().name("action").defaultValue(Action.Integration).build());
    private final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder().name("swap-back").defaultValue(true).visible(() -> action.get() == Action.Integration).build());
    private final Setting<Safety> safety = sgGeneral.add(new EnumSetting.Builder<Safety>().name("safety").defaultValue(Safety.Lethal).build());
    private final Setting<Boolean> cancelMotion = sgGeneral.add(new BoolSetting.Builder().name("cancel-motion").defaultValue(false).build());

    public enum Action {
        None,
        Totem,
        GApple,
        Crystal,
        Integration
    }

    public enum Safety {
        None,
        Lethal,
        Health
    }

    public OffHand() {
        super(Atlas.Konas,"OffHand", "Automatically manages your offhand");
    }

    private Timer timer = new Timer();
    private Item itemTarget = null;
    private boolean hasTotem = false;
    private boolean rightClick = false;
    private int swapBackSlot = -1;
    int w = 0;

    @Override
    public void onActivate() {
        Checker.Check();

        itemTarget = null;
        hasTotem = false;
        rightClick = false;
        swapBackSlot = -1;

        w = 0;
    }

    @Override
    public void onDeactivate() {
        Checker.Check();
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (w == 0) {
            InitializeUtils.banana();
            w++;
        }
        if (!mc.isOnThread()) return;

        if (mc.player.playerScreenHandler != mc.player.currentScreenHandler || mc.currentScreen instanceof AbstractInventoryScreen || mc.player.isCreative())
            return;

        if (!hasTotem) {
            itemTarget = getItemTarget();
            if (itemTarget == mc.player.getOffHandStack().getItem()) {
                itemTarget = null;
            }
        }

        if (itemTarget == null) {
            if (swapBackSlot != -1 && mc.player.getOffHandStack().getItem() == Items.ENCHANTED_GOLDEN_APPLE && !mc.options.keyUse.isPressed()) {
                if (cancelMotion.get() && mc.player.getVelocity().length() >= 9.0E-4D) {
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.isOnGround()));
                }
                mc.interactionManager.clickSlot(0, 45, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(0, swapBackSlot, 0, SlotActionType.PICKUP, mc.player);
                if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                    mc.interactionManager.clickSlot(0, 45, 0, SlotActionType.PICKUP, mc.player);
                }
                swapBackSlot = -1;
            }
            return;
        }

        if (timer.hasPassed(delay.get() * 100F) && mc.player.currentScreenHandler.getCursorStack().getItem() != itemTarget) {
            int index = 44;
            while (index >= 9) {
                if (mc.player.getInventory().getStack(index >= 36 ? index - 36 : index).getItem() == itemTarget) {
                    hasTotem = true;
                    if (cancelMotion.get() && mc.player.getVelocity().length() >= 9.0E-4D) {
                        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.isOnGround()));
                    }
                    mc.interactionManager.clickSlot(0, index, 0, SlotActionType.PICKUP, mc.player);
                    if (rightClick) {
                        rightClick = false;
                        swapBackSlot = index;
                    } else {
                        swapBackSlot = -1;
                    }
                }
                index--;
            }
        }

        if (timer.hasPassed(delay.get() * 200F) && mc.player.currentScreenHandler.getCursorStack().getItem() == itemTarget) {
            if (cancelMotion.get() && mc.player.getVelocity().length() >= 9.0E-4D) {
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.isOnGround()));
            }
            mc.interactionManager.clickSlot(0, 45, 0, SlotActionType.PICKUP, mc.player);
            if (mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                hasTotem = false;
                return;
            }
        }

        if (timer.hasPassed(delay.get() * 300F) && !mc.player.currentScreenHandler.getCursorStack().isEmpty() && mc.player.getOffHandStack().getItem() == itemTarget) {
            int index = 44;
            while (index >= 9) {
                if (mc.player.getInventory().getStack(index >= 36 ? index - 36 : index).isEmpty()) {
                    if (timer.hasPassed(delay.get() * 1000F) && mc.player.currentScreenHandler.getCursorStack().getItem() != itemTarget) {
                        if (cancelMotion.get() && mc.player.getVelocity().length() >= 9.0E-4D) {
                            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.isOnGround()));
                        }
                        mc.interactionManager.clickSlot(0, index, 0, SlotActionType.PICKUP, mc.player);
                        hasTotem = false;
                        if (rightClick) {
                            rightClick = false;
                            swapBackSlot = index;
                        } else {
                            swapBackSlot = -1;
                        }
                    }
                }
                index--;
            }
        }
    }

    private Item getItemTarget() {
        if (mc.player.getHealth() + mc.player.getAbsorptionAmount() <= health.get()) {
            return Items.TOTEM_OF_UNDYING;
        }

        if (safety.get() != Safety.None) {
            if (mc.player.fallDistance > 8F) return Items.TOTEM_OF_UNDYING;
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof EndCrystalEntity && entity.distanceTo(mc.player) < 6F) {
                    if ((mc.player.getHealth() + mc.player.getAbsorptionAmount()) - DamageCalculator.getExplosionDamage((EndCrystalEntity) entity, mc.player) <= ((safety.get() == Safety.Lethal) ? 1 : health.get())) {
                        return Items.TOTEM_OF_UNDYING;
                    }
                }
            }
        }

        if (action.get() == Action.Totem) return Items.TOTEM_OF_UNDYING;
        else if (action.get() == Action.GApple) return Items.ENCHANTED_GOLDEN_APPLE;
        else if (action.get() == Action.Crystal) return Items.END_CRYSTAL;

        if (action.get() == Action.Integration) {
            if (mc.player.isFallFlying()) {
                return Items.TOTEM_OF_UNDYING;
            } else if (mc.player.getMainHandStack().getItem() instanceof SwordItem && mc.options.keyUse.isPressed()) {
                if (swapBack.get()) {
                    rightClick = true;
                }
                return Items.ENCHANTED_GOLDEN_APPLE;
            } else if (Modules.get().get(AutoCrystal.class).isActive()) {
                return Items.END_CRYSTAL;
            }
            if (force.get()) {
                return Items.TOTEM_OF_UNDYING;
            }
            return null;
        }

        if (force.get()) {
            return Items.TOTEM_OF_UNDYING;
        }
        return null;
    }
}
