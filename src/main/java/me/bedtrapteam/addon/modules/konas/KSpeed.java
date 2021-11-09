package me.bedtrapteam.addon.modules.konas;

import me.bedtrapteam.addon.Atlas;
import me.bedtrapteam.addon.utils.Timer;
import me.bedtrapteam.addon.utils.TimerManager;
import me.bedtrapteam.addon.utils.enchansed.Player2Utils;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.CobwebBlock;
import net.minecraft.block.FluidBlock;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.Vec3d;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class KSpeed extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgWhileIn = settings.createGroup("While");

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>().name("mode").defaultValue(Mode.Strafe).build());
    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder().name("speed").defaultValue(1).min(0.1).max(10).build());
    private final Setting<Boolean> antiLagback = sgGeneral.add(new BoolSetting.Builder().name("strict").defaultValue(true).build());
    private final Setting<Boolean> useTimer = sgGeneral.add(new BoolSetting.Builder().name("use-timer").defaultValue(true).build());
    private final Setting<Boolean> whileSneaking = sgWhileIn.add(new BoolSetting.Builder().name("sneaking").defaultValue(false).build());
    private final Setting<Boolean> whileInLiquid = sgWhileIn.add(new BoolSetting.Builder().name("in-liquid").defaultValue(true).build());
    private final Setting<Boolean> whileInWeb = sgWhileIn.add(new BoolSetting.Builder().name("in-web").defaultValue(false).build());

    public enum Mode {
        Strafe, Vanilla
    }

    private double currentMotion = 0D;
    private double prevMotion = 0D;
    private boolean odd = false;
    private int state = 4;

    private double aacSpeed = 0.2873D;
    private int aacCounter;
    private int aacState = 4;
    private int ticksPassed = 0;

    private Timer timer = new Timer();
    private Timer lagbackTimer = new Timer();

    public KSpeed() {
        super(Atlas.Konas,"k-speed", "Makes you go faster");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!whileSneaking.get() && mc.player.isSneaking()) return;
        if (!whileInLiquid.get() && Player2Utils.checkIfBlockInBB(FluidBlock.class)) return;
        if (!whileInWeb.get() && Player2Utils.checkIfBlockInBB(CobwebBlock.class)) return;
        if (!lagbackTimer.hasPassed(350)) return;

        double x = event.movement.x;
        double y = event.movement.y;
        double z = event.movement.z;

        switch (mode.get()) {
            case Strafe -> {
                if (antiLagback.get()) {
                    aacCounter++;
                    aacCounter %= 5;

                    if (aacCounter != 0) {
                        TimerManager.resetTimer(this);
                    } else if (Player2Utils.isPlayerMoving()) {
                        if (useTimer.get()) {
                            TimerManager.updateTimer(this, 10, 1.3F);
                        }
                        mc.player.setVelocity(mc.player.getVelocity().x * 1.0199999809265137D, mc.player.getVelocity().y, mc.player.getVelocity().z * 1.0199999809265137D);
                    }

                    if (mc.player.isOnGround() && Player2Utils.isPlayerMoving()) {
                        aacState = 2;
                    }

                    if (round(mc.player.getY() - (int) mc.player.getY(), 3) == round(0.138D, 3)) {
                        mc.player.setVelocity(mc.player.getVelocity().x, mc.player.getVelocity().y - 0.08D, mc.player.getVelocity().z);
                        y -= 0.09316090325960147D;
                        mc.player.setPos(mc.player.getX(), mc.player.getY() - 0.09316090325960147D, mc.player.getZ());
                    }

                    if (aacState == 1 && (mc.player.input.movementForward != 0.0F || mc.player.input.movementSideways != 0.0F)) {
                        aacState = 2;
                        aacSpeed = 1.38D * Player2Utils.getBaseMotionSpeed() - 0.01D;
                    } else if (aacState == 2) {
                        aacState = 3;
                        mc.player.setVelocity(mc.player.getVelocity().x, 0.399399995803833D, mc.player.getVelocity().z);
                        y = 0.399399995803833D;
                        aacSpeed *= 2.149D;
                    } else if (aacState == 3) {
                        aacState = 4;
                        double adjustedMotion = 0.66D * (prevMotion - Player2Utils.getBaseMotionSpeed());
                        aacSpeed = prevMotion - adjustedMotion;
                    } else {
                        if (!mc.world.isSpaceEmpty(mc.player.getBoundingBox().offset(0.0, mc.player.getVelocity().y, 0.0)) || mc.player.verticalCollision)
                            aacState = 1;
                        aacSpeed = prevMotion - prevMotion / 159.0D;
                    }

                    aacSpeed = Math.max(aacSpeed, Player2Utils.getBaseMotionSpeed());

                    aacSpeed = Math.min(aacSpeed, (ticksPassed > 25) ? 0.449D : 0.433D);

                    float forward = mc.player.input.movementForward;
                    float strafe = mc.player.input.movementSideways;
                    float yaw = mc.player.getYaw();

                    ticksPassed++;

                    if (ticksPassed > 50)
                        ticksPassed = 0;
                    if (forward == 0.0F && strafe == 0.0F) {
                        x = 0D;
                        z = 0D;
                    } else if (forward != 0.0F) {
                        if (strafe >= 1.0F) {
                            yaw += ((forward > 0.0F) ? -45 : 45);
                            strafe = 0.0F;
                        } else if (strafe <= -1.0F) {
                            yaw += ((forward > 0.0F) ? 45 : -45);
                            strafe = 0.0F;
                        }
                        if (forward > 0.0F) {
                            forward = 1.0F;
                        } else if (forward < 0.0F) {
                            forward = -1.0F;
                        }
                    }

                    double cos = Math.cos(Math.toRadians((yaw + 90.0F)));
                    double sin = Math.sin(Math.toRadians((yaw + 90.0F)));

                    x = forward * aacSpeed * cos + strafe * aacSpeed * sin;
                    z = forward * aacSpeed * sin - strafe * aacSpeed * cos;

                    if (forward == 0.0F && strafe == 0.0F) {
                        x = 0.0D;
                        z = 0.0D;
                    }
                } else {
                    if (state != 1 || (mc.player.input.movementForward == 0.0f || mc.player.input.movementSideways == 0.0f)) {
                        if (state == 2 && (mc.player.input.movementForward != 0.0f || mc.player.input.movementSideways != 0.0f)) {
                            double jumpSpeed = 0.3999D;

                            if (mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST)) {
                                jumpSpeed += (mc.player.getStatusEffect(StatusEffects.JUMP_BOOST).getAmplifier() + 1) * 0.1F;
                            }

                            mc.player.setVelocity(new Vec3d(mc.player.getVelocity().x, jumpSpeed, mc.player.getVelocity().z));
                            y = jumpSpeed;
                            currentMotion *= odd ? 1.6835D : 1.395D;
                        } else if (state == 3) {
                            double adjustedMotion = (antiLagback.get() ? 0.76D : 0.66D) * (prevMotion - Player2Utils.getBaseMotionSpeed());
                            currentMotion = prevMotion - adjustedMotion;
                            odd = !odd;
                        } else {
                            if ((!mc.world.isSpaceEmpty(mc.player.getBoundingBox().offset(0.0, mc.player.getVelocity().y, 0.0)) || mc.player.verticalCollision) && state > 0) {
                                state = mc.player.input.movementForward == 0.0f && mc.player.input.movementSideways == 0.0f ? 0 : 1;
                            }
                            currentMotion = prevMotion - prevMotion / 159.0;
                        }
                    } else {
                        currentMotion = 1.35D * Player2Utils.getBaseMotionSpeed() - 0.01D;
                    }

                    currentMotion = Math.max(currentMotion, Player2Utils.getBaseMotionSpeed());

                    if (antiLagback.get()) {
                        if (lagbackTimer.hasPassed(2500L)) {
                            lagbackTimer.reset();
                        }

                        currentMotion = Math.min(currentMotion, lagbackTimer.hasPassed(1250L) ? 0.44D : 0.43D);
                    }


                    Vec3d directionalSpeed = Player2Utils.getDirectionalSpeed(currentMotion);

                    x = directionalSpeed.x;
                    z = directionalSpeed.z;

                    if (mc.player.input.movementForward != 0.0f || mc.player.input.movementSideways != 0.0f) {
                        state++;
                    }
                }
                break;
            }
            case Vanilla -> {
                if (!antiLagback.get() || !timer.hasPassed(190)) {
                    x *= speed.get();
                    z *= speed.get();
                } else {
                    timer.reset();
                }
                break;
            }
        }

        ((IVec3d) event.movement).setXZ(x,z);
        ((IVec3d) event.movement).setY(y);
    }

    private double round(double value, int places) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    @EventHandler
    public void onUpdateWalkingPlayer(PlayerMoveEvent event) {
        if (!Player2Utils.isPlayerMoving()) {
            currentMotion = 0D;
            mc.player.setVelocity(0, mc.player.getVelocity().y, 0);
            TimerManager.resetTimer(this);
        } else if (useTimer.get() && !(mode.get() == Mode.Strafe && antiLagback.get())) {
            TimerManager.updateTimer(this, 69, 21.75F / 20F); // 22 packets/tick flags sometimes
        } else {
            TimerManager.resetTimer(this);
        }

        double dX = mc.player.getX() - mc.player.prevX;
        double dZ = mc.player.getZ() - mc.player.prevZ;
        prevMotion = Math.sqrt(dX * dX + dZ * dZ);
    }

    @Override
    public void onActivate() {
        if (mc.player == null || mc.world == null) {
            toggle();
            return;
        }
        state = 4;
        currentMotion = Player2Utils.getBaseMotionSpeed();
        prevMotion = 0;
    }

    @Override
    public void onDeactivate() {
        TimerManager.resetTimer(this);
    }
}
