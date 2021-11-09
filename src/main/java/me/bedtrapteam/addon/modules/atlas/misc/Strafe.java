package me.bedtrapteam.addon.modules.atlas.misc;

import me.bedtrapteam.addon.Atlas;
import me.bedtrapteam.addon.utils.Utils;
import meteordevelopment.meteorclient.events.entity.player.JumpVelocityMultiplierEvent;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Anchor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;

public class Strafe extends Module {
    Vec3d last_velocity = Vec3d.ZERO;
    private static final double diagonal = 1.0 / Math.sqrt(2.0);
    boolean reset = false;
    boolean was_jump = false;
    boolean is_jump = false;
    double speed = 0.2873;
    private long timer = 0L;
    private final SettingGroup sg_general = this.settings.getDefaultGroup();
    public final Setting<Double> cfg_speed = this.sg_general.add(new DoubleSetting.Builder().name("speed").description("How fast you want to go in blocks per second.").defaultValue(5.746).min(0.0).sliderMin(2.0).sliderMax(5.746).build());
    public final Setting<Boolean> cfg_sprint = this.sg_general.add(new BoolSetting.Builder().name("sprint").description("...").defaultValue(false).build());
    public final Setting<Boolean> cfg_ncp = this.sg_general.add(new BoolSetting.Builder().name("ncp-accel").description("...").defaultValue(false).build());
    public final Setting<Double> cfg_multiplier = this.sg_general.add(new DoubleSetting.Builder().name("multiplier").visible(this.cfg_ncp::get).description("...").defaultValue(1.0).min(1.0).sliderMin(1.0).sliderMax(3.0).build());
    public final Setting<Boolean> cfg_speed_limit = this.sg_general.add(new BoolSetting.Builder().name("speed-limit").visible(this.cfg_ncp::get).description("...").defaultValue(true).build());
    public final Setting<Boolean> cfg_lower_jump = this.sg_general.add(new BoolSetting.Builder().name("lower-jump").visible(this.cfg_ncp::get).description("...").defaultValue(true).build());
    public final Setting<Boolean> cfg_apply_speed_potions = this.sg_general.add(new BoolSetting.Builder().name("apply-speed-potions").description("Applies the speed effect via potions.").defaultValue(true).build());
    public final Setting<Boolean> cfg_on_use = this.sg_general.add(new BoolSetting.Builder().name("stop-on-using-item").description("...").defaultValue(false).build());

    public Strafe() {
        super(Atlas.Misc, "strafe", "Makes you faster.");
    }

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event) {
        assert (this.mc.player != null);
        if (event.type != MovementType.SELF || this.mc.player.isFallFlying() || this.mc.player.isClimbing() || this.mc.player.getVehicle() != null) {
            return;
        }
        if (this.mc.player.isSneaking()) {
            return;
        }
        if (this.mc.player.isTouchingWater() || this.mc.player.isInLava()) {
            return;
        }
        if (this.cfg_on_use.get().booleanValue() && this.mc.player.isUsingItem()) {
            return;
        }
        Anchor anchor = Modules.get().get(Anchor.class);
        if (anchor.isActive() && anchor.controlMovement) {
            return;
        }
        double packet_speed_sqr = event.movement.x * event.movement.x + event.movement.z * event.movement.z;
        double new_speed = this.GetSpeed();
        double new_speed_sqr = new_speed * new_speed;
        if (!(!(packet_speed_sqr >= new_speed_sqr) || this.cfg_ncp.get().booleanValue() && this.cfg_speed_limit.get().booleanValue())) {
            return;
        }
        float yaw = this.mc.player.getYaw();
        Vec3d forward = Vec3d.fromPolar((float)0.0f, (float)yaw);
        Vec3d right = Vec3d.fromPolar((float)0.0f, (float)(yaw + 90.0f));
        double velX = 0.0;
        double velZ = 0.0;
        boolean a = false;
        boolean b = false;
        if (this.mc.player.input.pressingForward) {
            if (this.cfg_sprint.get().booleanValue() && !this.mc.player.isSprinting() && (this.mc.options.keyJump.isPressed() || !this.mc.player.isOnGround())) {
                this.mc.player.setSprinting(true);
            }
            velX += forward.x * new_speed;
            velZ += forward.z * new_speed;
            a = true;
        }
        if (this.mc.player.input.pressingBack) {
            velX -= forward.x * new_speed;
            velZ -= forward.z * new_speed;
            a = true;
        }
        if (this.mc.player.input.pressingRight) {
            velX += right.x * new_speed;
            velZ += right.z * new_speed;
            b = true;
        }
        if (this.mc.player.input.pressingLeft) {
            velX -= right.x * new_speed;
            velZ -= right.z * new_speed;
            b = true;
        }
        if (a && b) {
            velX *= diagonal;
            velZ *= diagonal;
        }
        ((IVec3d)event.movement).setXZ(velX, velZ);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        assert (this.mc.player != null);
        this.last_velocity = new Vec3d(this.mc.player.getX() - this.mc.player.prevX, this.mc.player.getY() - this.mc.player.prevY, this.mc.player.getZ() - this.mc.player.prevZ);
    }

    @EventHandler
    private void onJumpVelocityMultiplier(JumpVelocityMultiplierEvent event) {
        this.is_jump = true;
        if (this.cfg_lower_jump.get().booleanValue()) {
            event.multiplier *= 0.95531255f;
        }
    }

    private double GetSpeed() {
        assert (this.mc.player != null);
        assert (this.mc.world != null);
        double def_speed = this.GetDefaultSpeed();
        if (!this.cfg_ncp.get().booleanValue() || !this.mc.options.keyJump.isPressed() && this.mc.player.isOnGround()) {
            return def_speed;
        }
        if (this.reset) {
            this.reset = false;
            this.speed = (double)1.18f * def_speed - 0.01;
        } else if (this.is_jump) {
            this.is_jump = false;
            this.was_jump = true;
            this.speed *= this.cfg_multiplier.get().doubleValue();
        } else if (this.was_jump) {
            this.was_jump = false;
            double distance = this.last_velocity.length();
            this.speed = distance - 0.76 * (distance - def_speed);
        } else {
            if (!this.mc.world.isSpaceEmpty(this.mc.player.getBoundingBox().offset(0.0, this.mc.player.getVelocity().y, 0.0)) || this.mc.player.verticalCollision) {
                this.reset = true;
            }
            double distance = this.last_velocity.length();
            this.speed = distance - distance / 159.0;
        }
        if (this.speed < def_speed) {
            this.speed = def_speed;
        }
        if (this.cfg_speed_limit.get().booleanValue()) {
            if (Utils.GetCurTime() - this.timer > 2500L) {
                this.timer = Utils.GetCurTime();
            }
            this.speed = Math.min(this.speed, Utils.GetCurTime() - this.timer > 1250L ? 0.44 : 0.43);
        }
        return this.speed;
    }

    private double GetDefaultSpeed() {
        assert (this.mc.player != null);
        double default_speed = this.cfg_speed.get() * 0.05;
        if (this.cfg_apply_speed_potions.get().booleanValue()) {
            int amplifier;
            if (this.mc.player.hasStatusEffect(StatusEffects.SPEED)) {
                amplifier = Objects.requireNonNull(this.mc.player.getStatusEffect(StatusEffects.SPEED)).getAmplifier();
                default_speed *= 1.0 + 0.2 * (double)(amplifier + 1);
            }
            if (this.mc.player.hasStatusEffect(StatusEffects.SLOWNESS)) {
                amplifier = Objects.requireNonNull(this.mc.player.getStatusEffect(StatusEffects.SLOWNESS)).getAmplifier();
                default_speed /= 1.0 + 0.2 * (double)(amplifier + 1);
            }
        }
        return default_speed;
    }
}
