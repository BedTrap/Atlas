package me.bedtrapteam.addon.modules.konas;

import me.bedtrapteam.addon.Atlas;
import me.bedtrapteam.addon.mixins.PlayerInteractEntityC2SPacketAccessor;
import me.bedtrapteam.addon.utils.*;
import me.bedtrapteam.addon.utils.enchansed.Block2Utils;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.EnchantedGoldenAppleItem;
import net.minecraft.item.EndCrystalItem;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class AutoCrystal extends Module {
    private final SettingGroup sgAntiCheat = settings.createGroup("AntiCheat");
    private final SettingGroup sgSpeeds = settings.createGroup("Speeds");
    private final SettingGroup sgRanges = settings.createGroup("Ranges");
    private final SettingGroup sgSwap = settings.createGroup("Swap");
    private final SettingGroup sgTargeting = settings.createGroup("Targeting");
    private final SettingGroup sgTargets = settings.createGroup("Targets");
    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // AntiCheat
    private final Setting<TimingMode> timingMode = sgAntiCheat.add(new EnumSetting.Builder<TimingMode>().name("timing").defaultValue(TimingMode.Sequential).build());
    private final Setting<Boolean> rotate = sgAntiCheat.add(new BoolSetting.Builder().name("rotate").defaultValue(false).build());
    private final Setting<Boolean> inhibit = sgAntiCheat.add(new BoolSetting.Builder().name("inhibit").defaultValue(true).build());
    private final Setting<Boolean> limit = sgAntiCheat.add(new BoolSetting.Builder().name("limit").defaultValue(false).build());
    private final Setting<YawStepMode> yawStep = sgAntiCheat.add(new EnumSetting.Builder<YawStepMode>().name("yaw-step").defaultValue(YawStepMode.Break).build());
    private final Setting<Double> yawAngle = sgAntiCheat.add(new DoubleSetting.Builder().name("yaw-angle").defaultValue(0.3).min(0.1).max(1).sliderMin(0.1).sliderMax(1).build());
    private final Setting<Integer> yawTicks = sgAntiCheat.add(new IntSetting.Builder().name("yaw-ticks").defaultValue(1).min(1).sliderMin(1).sliderMax(5).build());
    private final Setting<Boolean> rayTrace = sgAntiCheat.add(new BoolSetting.Builder().name("ray-trace").defaultValue(true).build());
    private final Setting<Boolean> strictDirection = sgAntiCheat.add(new BoolSetting.Builder().name("strict-direction").defaultValue(true).build());
    private final Setting<Boolean> protocol = sgAntiCheat.add(new BoolSetting.Builder().name("protocol").defaultValue(true).build());

    // Speeds
    private final Setting<ConfirmMode> confirm = sgSpeeds.add(new EnumSetting.Builder<ConfirmMode>().name("confirm").defaultValue(ConfirmMode.OFF).build());
    private final Setting<Integer> ticksExisted = sgSpeeds.add(new IntSetting.Builder().name("ticks-existed").defaultValue(0).min(0).sliderMin(0).sliderMax(20).build());
    private final Setting<Integer> attackFactor = sgSpeeds.add(new IntSetting.Builder().name("attack-factor").defaultValue(2).min(0).sliderMin(0).sliderMax(20).build());
    private final Setting<Integer> breakSpeed = sgSpeeds.add(new IntSetting.Builder().name("break-speed").defaultValue(20).min(1).sliderMin(1).sliderMax(20).build());
    private final Setting<Integer> placeSpeed = sgSpeeds.add(new IntSetting.Builder().name("place-speed").defaultValue(20).min(1).sliderMin(1).sliderMax(20).build());
    private final Setting<SyncMode> sync = sgSpeeds.add(new EnumSetting.Builder<SyncMode>().name("sync").defaultValue(SyncMode.Strict).build());

    // Ranges
    private final Setting<Double> breakRange = sgRanges.add(new DoubleSetting.Builder().name("break-range").defaultValue(4.3).min(0).sliderMax(6).build());
    private final Setting<Double> breakWallsRange = sgRanges.add(new DoubleSetting.Builder().name("break-walls").defaultValue(1.5).min(0).sliderMax(6).build());
    private final Setting<Double> placeRange = sgRanges.add(new DoubleSetting.Builder().name("place-range").defaultValue(4).min(0).sliderMax(6).build());
    private final Setting<Double> placeWallsRange = sgRanges.add(new DoubleSetting.Builder().name("place-walls").defaultValue(3).min(0).sliderMax(6).build());

    // Swap
    private final Setting<Boolean> autoSwap = sgSwap.add(new BoolSetting.Builder().name("auto-swap").defaultValue(false).build());
    private final Setting<Integer> swapDelay = sgSwap.add(new IntSetting.Builder().name("swap-delay").defaultValue(1).min(0).sliderMax(20).build());
    private final Setting<Integer> switchDelay = sgSwap.add(new IntSetting.Builder().name("ghost-delay").defaultValue(5).min(0).sliderMax(10).build());
    private final Setting<Boolean> antiWeakness = sgSwap.add(new BoolSetting.Builder().name("anti-weakness").defaultValue(false).build());

    // Targeting
    private final Setting<Boolean> onlyOwn = sgTargeting.add(new BoolSetting.Builder().name("only-own").defaultValue(false).build());
    private final Setting<Double> maxBreakSelfDamage = sgTargeting.add(new DoubleSetting.Builder().name("break-self-dmg").defaultValue(6).min(0).sliderMax(20).build());
    private final Setting<Boolean> terrainIgnore = sgTargeting.add(new BoolSetting.Builder().name("terrain-ignore").defaultValue(false).build());
    private final Setting<TargetingMode> targetingMode = sgTargeting.add(new EnumSetting.Builder<TargetingMode>().name("target").defaultValue(TargetingMode.All).build());
    private final Setting<PriorityMode> priorityMode = sgTargeting.add(new EnumSetting.Builder<PriorityMode>().name("priority").defaultValue(PriorityMode.Damage).build());
    private final Setting<Double> enemyRange = sgTargeting.add(new DoubleSetting.Builder().name("target-range").defaultValue(8).min(0).sliderMax(15).build());
    private final Setting<Integer> predictTicks = sgTargeting.add(new IntSetting.Builder().name("extrapolation").defaultValue(1).min(0).sliderMax(20).build());
    private final Setting<Double> minPlaceDamage = sgTargeting.add(new DoubleSetting.Builder().name("min-damage").defaultValue(6).min(0).sliderMax(20).build());
    private final Setting<Double> maxPlaceSelfDamage = sgTargeting.add(new DoubleSetting.Builder().name("place-self-dmg").defaultValue(12).min(0).sliderMax(20).build());
    private final Setting<Double> faceplaceHealth = sgTargeting.add(new DoubleSetting.Builder().name("faceplace-hp").defaultValue(4).min(0).sliderMax(20).build());
    private final Setting<Keybind> forceFaceplace = sgTargeting.add(new KeybindSetting.Builder().name("force").defaultValue(Keybind.none()).build());

    // Targets
    private final Setting<Boolean> players = sgTargets.add(new BoolSetting.Builder().name("players").defaultValue(true).build());
    private final Setting<Boolean> friends = sgTargets.add(new BoolSetting.Builder().name("friends").defaultValue(false).build());
    private final Setting<Boolean> creatures = sgTargets.add(new BoolSetting.Builder().name("creatures").defaultValue(false).build());
    private final Setting<Boolean> monsters = sgTargets.add(new BoolSetting.Builder().name("monsters").defaultValue(false).build());
    private final Setting<Boolean> ambients = sgTargets.add(new BoolSetting.Builder().name("ambients").defaultValue(false).build());

    // Pause
    private final Setting<Boolean> noMineSwitch = sgPause.add(new BoolSetting.Builder().name("mining").defaultValue(false).build());
    private final Setting<Boolean> noGapSwitch = sgPause.add(new BoolSetting.Builder().name("gapping").defaultValue(false).build());
    private final Setting<Boolean> rightClickGap = sgPause.add(new BoolSetting.Builder().name("right-click-gap").defaultValue(false).build());
    private final Setting<Boolean> disableWhenKA = sgPause.add(new BoolSetting.Builder().name("aura").defaultValue(false).build());
    private final Setting<Integer> disableUnderHealth = sgSwap.add(new IntSetting.Builder().name("health").defaultValue(2).min(0).sliderMax(10).build());

    // Render
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").defaultValue(true).build());
    private final Setting<Boolean> thick = sgRender.add(new BoolSetting.Builder().name("thick").visible(render::get).defaultValue(true).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").visible(render::get).defaultValue(ShapeMode.Sides).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The side color.").visible(render::get).defaultValue(new SettingColor(255, 0, 0, 75, true)).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The line color.").visible(render::get).defaultValue(new SettingColor(255, 0, 0, 200)).build());


    public enum TimingMode {
        Sequential, Vanilla
    }

    public enum YawStepMode {
        Off, Break, Full
    }

    public enum ConfirmMode {
        OFF, SEMI, FULL
    }

    public enum SyncMode {
        Strict, Merge, Adapt
    }

    public enum TargetingMode {
        All, Health, Nearest
    }

    public enum PriorityMode {
        Damage, Ratio
    }

    public AutoCrystal() {
        super(Atlas.Konas, "auto-crystal", "Automatically place and break crystals");
    }

    private static final float[] spawnRates = new float[20];
    private static int nextIndex = 0;
    private static long lastSpawn;

    public static ConcurrentHashMap<Integer, Long> silentMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<BlockPos, Long> placeLocations = new ConcurrentHashMap<>();

    private Vec3d rotations;

    public static double getPlaceRange = 5;
    public static double getPlaceWallRange = 5;
    public static boolean getProtocol = true;
    public static boolean getStrictDirection = false;

    private final Timer placeTimer = new Timer();
    private final Timer breakTimer = new Timer();
    private final Timer noGhostTimer = new Timer();
    private final Timer switchTimer = new Timer();
    private final Timer linearTimer = new Timer();
    private final Timer cacheTimer = new Timer();
    private final Timer scatterTimer = new Timer();
    private BlockPos cachePos = null;

    private boolean lastBroken = false;

    private final Timer inhibitTimer = new Timer();
    private EndCrystalEntity inhibitEntity = null;

    private Vec3d bilateralVec = null;

    private final List<BlockPos> selfPlacePositions = new CopyOnWriteArrayList<>();

    private int ticks;
    int m = 0;

    private String targetName;
    private final Timer targetTimer = new Timer();

    @Override
    public void onActivate() {
        Checker.Check();

        getPlaceRange = placeRange.get();
        getPlaceWallRange = placeWallsRange.get();
        getProtocol = protocol.get();
        getStrictDirection = strictDirection.get();

        lastBroken = false;
        silentMap.clear();
        rotations = null;
        cachePos = null;
        inhibitEntity = null;
        selfPlacePositions.clear();
        ticks = 0;
        bilateralVec = null;

        m = 0;
    }

    @Override
    public void onDeactivate() {
        Checker.Check();
    }

    @EventHandler(priority = 100)
    public void onUpdatePre(TickEvent.Pre event) {
        if (timingMode.get() == TimingMode.Sequential) return;

        if (check()) {
            if (!generateBreak()) {
                generatePlace();
            }
        }
    }

    @EventHandler()
    public void onUpdatePre(TickEvent.Post event) {
        if (m == 0) {
            Block2Utils.Check();
            m++;
        }
        if (timingMode.get() == TimingMode.Sequential) return;

        if (check()) {
            if (!generateBreak()) {
                generatePlace();
            }
        }
    }


    @EventHandler(priority = 50)
    public void onUpdateWalkingPlayer(PlayerMoveEvent event) {
        placeLocations.forEach((pos, time) -> {
            if (System.currentTimeMillis() - time > 1500) {
                placeLocations.remove(pos);
            }
        });

        ticks--;

        if (timingMode.get() == TimingMode.Sequential) {
            if (bilateralVec != null) {
                for (Entity entity : mc.world.getEntities()) {
                    if (entity instanceof EndCrystalEntity && Math.sqrt(entity.squaredDistanceTo(bilateralVec.x, bilateralVec.y, bilateralVec.z)) <= 6) {
                        silentMap.put(entity.getId(), System.currentTimeMillis());
                    }
                }
                bilateralVec = null;
            }

            if (check()) {
                if (!generateBreak()) {
                    generatePlace();
                }
            }
        }
    }

    private boolean check() {
        if ((noMineSwitch.get() && mc.interactionManager.isBreakingBlock()) || (noGapSwitch.get() && mc.player.getActiveItem().getItem() instanceof EnchantedGoldenAppleItem) || (mc.player.getHealth() + mc.player.getAbsorptionAmount() < disableUnderHealth.get()) || (disableWhenKA.get() && Modules.get().get(KillAura.class).isActive())) {
            return false;
        }

        if (noGapSwitch.get() && rightClickGap.get() && mc.options.keyUse.isPressed() && mc.player.getInventory().getMainHandStack().getItem() instanceof EndCrystalItem) {
            int gappleSlot = -1;

            for (int l = 0; l < 9; ++l) {
                if (mc.player.getInventory().getStack(l).getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
                    gappleSlot = l;
                    break;
                }
            }

            if (gappleSlot != -1 && gappleSlot != mc.player.getInventory().selectedSlot && switchTimer.hasPassed(swapDelay.get() * 50)) {
                mc.player.getInventory().selectedSlot = gappleSlot;
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(gappleSlot));
                switchTimer.reset();
                noGhostTimer.reset();
                return false;
            }
        }

        if (!isOffhand() && !(mc.player.getInventory().getMainHandStack().getItem() instanceof EndCrystalItem)) {
            if (!autoSwap.get()) {
                return false;
            } else return getCrystalSlot() != -1;
        }

        return true;
    }

    private void generatePlace() {
        boolean cpvp = mc.getCurrentServerEntry() != null && mc.getCurrentServerEntry().address.toLowerCase().contains("crystalpvp");
        int adjustedResponseTime = (int) Math.max(100, ((EntityUtils.getPing(mc.player) + 50) / (TickRateUtils.getLatestTickRate() / 20F))) + 150;
        if ((confirm.get() != ConfirmMode.FULL || inhibitEntity == null || inhibitEntity.age >= ticksExisted.get())) {
            lastBroken = false;
            if ((sync.get() != SyncMode.Strict || breakTimer.hasPassed(950F - breakSpeed.get() * 50F - EntityUtils.getPing(mc.player))) && placeTimer.hasPassed(1000F - placeSpeed.get() * 50F) && (timingMode.get() == TimingMode.Sequential || linearTimer.hasPassed(cpvp ? 20 : 0))) {
                if (confirm.get() != ConfirmMode.OFF) {
                    if (cachePos != null && !cacheTimer.hasPassed(adjustedResponseTime + 100) && canPlaceCrystal(cachePos)) {
                        BlockHitResult result = handlePlaceRotation(cachePos);

                        if (placeCrystal(result)) {
                            placeTimer.reset();
                        }
                        return;
                    }
                }
                List<BlockPos> blocks = findCrystalBlocks();
                if (!blocks.isEmpty()) {
                    BlockPos candidatePos = findPlacePosition(blocks, getTargetsInRange());
                    if (candidatePos != null) {
                        BlockHitResult result = handlePlaceRotation(candidatePos);
                        if (placeCrystal(result)) {
                            placeTimer.reset();
                        }
                    }
                }
            }
        }
    }

    private BlockHitResult handlePlaceRotation(BlockPos pos) {
        Vec3d eyesPos = LookCalculator.getEyesPos(mc.player);

        if (strictDirection.get()) {
            Vec3d closestPoint = null;
            Direction closestDirection = null;
            double closestDistance = 999D;

            for (Vec3d point : multiPoint) {
                Vec3d p = new Vec3d(pos.getX() + point.getX(), pos.getY() + point.getY(), pos.getZ() + point.getZ());
                double dist = p.distanceTo(eyesPos);
                if ((dist < closestDistance && closestDirection == null)) {
                    closestPoint = p;
                    closestDistance = dist;
                }

                BlockHitResult result = mc.world.raycast(new RaycastContext(eyesPos, p, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));

                if (result != null && result.getType() == HitResult.Type.BLOCK && result.getBlockPos().equals(pos)) {
                    double visDist = result.getPos().distanceTo(eyesPos);
                    if (closestDirection == null || visDist < closestDistance) {
                        closestDirection = result.getSide();
                        closestDistance = visDist;
                        closestPoint = result.getPos();
                    }
                }
            }

            if (closestPoint != null) {
                if (rotate.get()) {
                    rotations = closestPoint;
                }

                return new BlockHitResult(closestPoint, closestDirection == null ? Direction.getFacing(eyesPos.x - closestPoint.x, eyesPos.y - closestPoint.y, eyesPos.z - closestPoint.z) : closestDirection, pos, false);
            }
        }

        if (rayTrace.get()) {
            for (Direction direction : Direction.values()) {
                RaycastContext raycastContext = new RaycastContext(eyesPos, new Vec3d(pos.getX() + 0.5 + direction.getVector().getX() * 0.5,
                    pos.getY() + 0.5 + direction.getVector().getY() * 0.5,
                    pos.getZ() + 0.5 + direction.getVector().getZ() * 0.5), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
                BlockHitResult result = mc.world.raycast(raycastContext);
                if (result != null && result.getType() == HitResult.Type.BLOCK && result.getBlockPos().equals(pos)) {
                    rotations = result.getPos();
                    return result;
                }
            }
        }

        if (rotate.get()) {
            rotations = new Vec3d(pos.getX() + 0.5D,
                pos.getY() + 1D,
                pos.getZ() + 0.5D);
        }

        return new BlockHitResult(new Vec3d(pos.getX() + 0.5D,
            pos.getY() + 1D,
            pos.getZ() + 0.5D), Direction.UP, pos, false);
    }

    private boolean generateBreak() {
        List<LivingEntity> targetsInRange = getTargetsInRange();

        int adjustedResponseTime = (int) Math.max(100, ((EntityUtils.getPing(mc.player) + 50) / (TickRateUtils.getLatestTickRate() / 20F))) + 150;

        EndCrystalEntity crystal = findCrystalTarget(targetsInRange, adjustedResponseTime);

        if (crystal != null) {
            if (crystal.age > ticksExisted.get() - 1) {
                if (rotate.get()) {
                    rotations = crystal.getPos();
                }
                if (breakTimer.hasPassed(1020F - breakSpeed.get() * 50F)) {
                    if (lastBroken) {
                        lastBroken = false;
                        if (sync.get() == SyncMode.Strict) {
                            return false;
                        }
                    }
                    if (breakCrystal(crystal)) {
                        lastBroken = true;
                        breakTimer.reset();
                        silentMap.put(crystal.getId(), System.currentTimeMillis());
                        for (Entity entity : mc.world.getEntities()) {
                            if (entity instanceof EndCrystalEntity && entity.distanceTo(crystal) <= 6) {
                                silentMap.put(entity.getId(), System.currentTimeMillis());
                            }
                        }
                    }
                    if (sync.get() != SyncMode.Strict) {
                        generatePlace();
                    }
                }
            }
            return true;
        }

        return false;
    }

    public boolean placeCrystal(BlockHitResult result) {
        if (result != null) {
            if (autoSwap.get()) {
                if (switchTimer.hasPassed(swapDelay.get() * 50)) {
                    if (!setCrystalSlot()) return false;
                } else {
                    return false;
                }
            }

            if (!isOffhand() && mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL) {
                return false;
            }

            mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(isOffhand() ? Hand.OFF_HAND : Hand.MAIN_HAND, result));
            mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(isOffhand() ? Hand.OFF_HAND : Hand.MAIN_HAND));

            placeLocations.put(result.getBlockPos(), System.currentTimeMillis());
            selfPlacePositions.add(result.getBlockPos());
            return true;
        }
        return false;
    }

    private boolean breakCrystal(EndCrystalEntity targetCrystal) {
        if (!noGhostTimer.hasPassed(switchDelay.get() * 100F)) return false;
        if (targetCrystal != null) {
            if (antiWeakness.get() && mc.player.hasStatusEffect(StatusEffects.WEAKNESS) && !(mc.player.getMainHandStack().getItem() instanceof SwordItem)) {
                setSwordSlot();
                return false;
            }

            mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(targetCrystal, mc.player.isSneaking()));
            mc.player.swingHand(Hand.MAIN_HAND);
            if (inhibit.get()) {
                inhibitTimer.reset();
                inhibitEntity = targetCrystal;
            }
            return true;
        }
        return false;
    }

    private BlockPos findPlacePosition(List<BlockPos> blocks, List<LivingEntity> targets) {
        if (!scatterTimer.hasPassed(ticksExisted.get() * 50)) {
            return null;
        }

        BlockPos bestPos = null;

        LivingEntity bestTarget = null;

        // Damage targeting
        float bestDamage = 0.0F;

        // Ratio targeting
        float bestRatio = 0.0F;

        if (targets.isEmpty()) return null;

        for (BlockPos block : blocks) {
            Vec3d blockVec = new Vec3d(block.getX() + 0.5, block.getY() + 1, block.getZ() + 0.5);
            float damage = 0.0F;
            LivingEntity target = null;
            float damageToSelf = DamageCalculator.getExplosionDamage(blockVec, 6F, mc.player);

            if (mc.player.getHealth() + mc.player.getAbsorptionAmount() <= damageToSelf + 2F) {
                continue;
            }

            if (damageToSelf > maxPlaceSelfDamage.get()) {
                continue;
            }

            for (LivingEntity player : targets) {
                boolean localOverrideMinDamage = false;

                float damageToTarget = DamageCalculator.getExplosionDamage(blockVec, 6F, player);

                if (damageToTarget >= 0.5D) {
                    if (player.getHealth() + player.getAbsorptionAmount() - damageToTarget <= 0 || player.getHealth() + player.getAbsorptionAmount() < faceplaceHealth.get()) {
                        localOverrideMinDamage = true;
                    }
                }

                if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), forceFaceplace.get().getValue())) {
                    localOverrideMinDamage = true;
                }

                if (damageToTarget > damage && (damageToTarget >= minPlaceDamage.get() || localOverrideMinDamage)) {
                    damage = damageToTarget;
                    target = player;
                }
            }

            if (priorityMode.get() == PriorityMode.Damage) {
                if (damage > bestDamage) {
                    bestDamage = damage;
                    bestPos = block;
                    bestTarget = target;
                }
            } else {
                if (damage / damageToSelf > bestRatio) {
                    bestDamage = damage;
                    bestRatio = damage / damageToSelf;
                    bestPos = block;
                    bestTarget = target;
                }
            }
        }

        if (bestTarget != null && bestPos != null) {
            targetName = bestTarget.getEntityName();
            targetTimer.reset();
        } else {
            targetName = null;
        }

        cachePos = bestPos;
        cacheTimer.reset();

        return bestPos;
    }

//    @Override
//    public String getTargetName() {
//        if (targetTimer.hasPassed(2500) || targetName == null) return "";
//        return targetName;
//    }

    private double getDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double d0 = (x1 - x2);
        double d1 = (y1 - y2);
        double d2 = (z1 - z2);
        return Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
    }

    public static float getCPS() {
        float numSpawns = 0.0F;
        float sumSpawnsRates = 0.0F;
        for (float spawnRate : spawnRates) {
            if (spawnRate > 0.0F) {
                sumSpawnsRates += spawnRate;
                numSpawns += 1.0F;
            }
        }
        if (numSpawns == 0F) return 0F;
        // return MathHelper.clamp((int) Math.ceil((20F / ((sumSpawnsRates / numSpawns) / 50F)) + 0.5F), 0, 10);
        return RoundingUtils.roundFloat(20F / ((sumSpawnsRates / numSpawns) / 50F), 1);
    }

    @EventHandler
    public void onPacketRecive(PacketEvent.Receive event) {
        if (event.packet instanceof EntitySpawnS2CPacket) {
            EntitySpawnS2CPacket packet = (EntitySpawnS2CPacket) event.packet;
            if (packet.getEntityTypeId() == EntityType.END_CRYSTAL) {
                placeLocations.forEach((pos, time) -> {
                    if (getDistance(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, packet.getX(), packet.getY() - 1, packet.getZ()) < 1) {
                        if (lastSpawn != -1L) {
                            float timeElapsed = (float) (System.currentTimeMillis() - lastSpawn);
                            spawnRates[(nextIndex % spawnRates.length)] = timeElapsed;
                            nextIndex += 1;
                        }
                        lastSpawn = System.currentTimeMillis();
                        placeLocations.remove(pos);
                        cachePos = null;
                        if (!limit.get() && inhibit.get()) {
                            scatterTimer.reset();
                        }

                        if (ticksExisted.get() != 0 || mc.player.hasStatusEffect(StatusEffects.WEAKNESS) || !mc.isOnThread())
                            return;

                        if (!noGhostTimer.hasPassed(switchDelay.get() * 100F)) return;

                        if (silentMap.containsKey(packet.getId())) return;

                        if (!check()) return;

                        Vec3d spawnVec = new Vec3d(packet.getX(), packet.getY(), packet.getZ());

                        if (LookCalculator.getEyesPos(mc.player).distanceTo(spawnVec) > breakRange.get()) return;

                        if (!(breakTimer.hasPassed(1000F - breakSpeed.get() * 50F))) return;

                        if (DamageCalculator.getExplosionDamage(spawnVec, 6F, mc.player) + 2F >= mc.player.getHealth() + mc.player.getAbsorptionAmount())
                            return;

                        silentMap.put(packet.getId(), System.currentTimeMillis());
                        bilateralVec = spawnVec;

                        PlayerInteractEntityC2SPacket attackPacket = (PlayerInteractEntityC2SPacket) event.packet;
                        ((PlayerInteractEntityC2SPacketAccessor) attackPacket).setEntityId(packet.getId());
                        // ((PlayerInteractEntityC2SPacketAccessor) attackPacket).setType(PlayerInteractEntityC2SPacket.InteractType.ATTACK);
                        ((PlayerInteractEntityC2SPacketAccessor) attackPacket).setPlayerSneaking(mc.player.isSneaking());
                        mc.player.networkHandler.sendPacket(attackPacket);
                        mc.player.swingHand(Hand.MAIN_HAND);

                        breakTimer.reset();
                        linearTimer.reset();

                        lastBroken = true;

                        if (sync.get() == SyncMode.Adapt) {
                            generatePlace();
                        }
                    }
                });
            }
        }
    }

    private List<BlockPos> findCrystalBlocks() {
        List<BlockPos> positions = new ArrayList<>();
        BlockPos centerPos = mc.player.getBlockPos();
        int r = (int) Math.ceil(placeRange.get()) + 1;
        int h = placeRange.get().intValue();
        for (int i = centerPos.getX() - r; i < centerPos.getX() + r; i++) {
            for (int j = centerPos.getY() - h; j < centerPos.getY() + h; j++) {
                for (int k = centerPos.getZ() - r; k < centerPos.getZ() + r; k++) {
                    BlockPos pos = new BlockPos(i, j, k);
                    if (canPlaceCrystal(pos)) {
                        positions.add(pos);
                    }
                }
            }
        }
        return positions;
    }

    public static final Vec3d[] multiPoint = new Vec3d[]{
        // z
        new Vec3d(0.05, 0.05, 0),
        new Vec3d(0.05, 0.95, 0),
        new Vec3d(0.95, 0.05, 0),
        new Vec3d(0.95, 0.95, 0),
        new Vec3d(0.5, 0.5, 0),
        new Vec3d(0.05, 0.05, 1),
        new Vec3d(0.05, 0.95, 1),
        new Vec3d(0.95, 0.05, 1),
        new Vec3d(0.95, 0.95, 1),
        new Vec3d(0.5, 0.5, 1),
        // y
        new Vec3d(0.05, 0, 0.05),
        new Vec3d(0.05, 0, 0.95),
        new Vec3d(0.95, 0, 0.05),
        new Vec3d(0.95, 0, 0.95),
        new Vec3d(0.5, 0, 0.5),
        new Vec3d(0.05, 1, 0.05),
        new Vec3d(0.05, 1, 0.95),
        new Vec3d(0.95, 1, 0.05),
        new Vec3d(0.95, 1, 0.95),
        new Vec3d(0.5, 1, 0.5),
        // x
        new Vec3d(0, 0.05, 0.05),
        new Vec3d(0, 0.95, 0.05),
        new Vec3d(0, 0.05, 0.95),
        new Vec3d(0, 0.95, 0.95),
        new Vec3d(0, 0.5, 0.5),
        new Vec3d(1, 0.05, 0.05),
        new Vec3d(1, 0.95, 0.05),
        new Vec3d(1, 0.05, 0.95),
        new Vec3d(1, 0.95, 0.95),
        new Vec3d(1, 0.5, 0.5)
    };

    public static final Vec3d[] fastMultiPoint = new Vec3d[]{
        new Vec3d(0.05, 0.05, 0.05),
        new Vec3d(0.05, 0.05, 0.95),
        new Vec3d(0.05, 0.95, 0.05),
        new Vec3d(0.95, 0.05, 0.05),
        new Vec3d(0.95, 0.95, 0.05),
        new Vec3d(0.05, 0.95, 0.95),
        new Vec3d(0.95, 0.95, 0.95),
        new Vec3d(0.95, 0.05, 0.95)
    };

    public boolean canPlaceCrystal(BlockPos blockPos) {
        if (!(mc.world.getBlockState(blockPos).getBlock() == Blocks.BEDROCK
            || mc.world.getBlockState(blockPos).getBlock() == Blocks.OBSIDIAN)) return false;

        BlockPos boost = blockPos.add(0, 1, 0);

        if (!(mc.world.getBlockState(boost).getBlock() == Blocks.AIR)) return false;

        BlockPos boost2 = blockPos.add(0, 2, 0);

        if (protocol.get()) {
            if (!(mc.world.getBlockState(boost2).getBlock() == Blocks.AIR)) {
                return false;
            }
        }

        if (!RayTraceUtils.canSee(new Vec3d(blockPos.getX() + 0.5, blockPos.getY() + 1.7, blockPos.getZ() + 0.5), new Vec3d(blockPos.getX() + 0.5, blockPos.getY() + 1.0, blockPos.getZ() + 0.5))) {
            if (LookCalculator.getEyesPos(mc.player).distanceTo(new Vec3d(blockPos.getX() + 0.5, blockPos.getY() + 1.0, blockPos.getZ() + 0.5)) > breakWallsRange.get()) {
                return false;
            }
        }

        Vec3d playerEyes = LookCalculator.getEyesPos(mc.player);
        boolean canPlace = false;

        if (strictDirection.get()) {
            for (Vec3d point : fastMultiPoint) {
                Vec3d p = new Vec3d(blockPos.getX() + point.getX(), blockPos.getY() + point.getY(), blockPos.getZ() + point.getZ());
                double distanceTo = playerEyes.distanceTo(p);
                if (distanceTo > placeRange.get()) {
                    continue;
                }
                if (distanceTo > placeWallsRange.get()) {
                    if (strictDirection.get()) {
                        BlockHitResult result = mc.world.raycast(new RaycastContext(playerEyes, p, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
                        if (result != null && result.getType() == HitResult.Type.BLOCK && result.getBlockPos().equals(blockPos)) {
                            canPlace = true;
                            break;
                        }
                    }
                } else {
                    canPlace = true;
                    break;
                }
            }
        } else {
            for (Direction dir : Direction.values()) {
                Vec3d p = new Vec3d(blockPos.getX() + 0.5 + dir.getVector().getX() * 0.5,
                    blockPos.getY() + 0.5 + dir.getVector().getY() * 0.5,
                    blockPos.getZ() + 0.5 + dir.getVector().getZ() * 0.5);
                double distanceTo = playerEyes.distanceTo(p);
                if (distanceTo > placeRange.get()) {
                    continue;
                }
                if (distanceTo > placeWallsRange.get()) {
                    if (strictDirection.get()) {
                        BlockHitResult result = mc.world.raycast(new RaycastContext(playerEyes, p, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
                        if (result != null && result.getType() == HitResult.Type.BLOCK && result.getBlockPos().equals(blockPos)) {
                            canPlace = true;
                            break;
                        }
                    }
                } else {
                    canPlace = true;
                    break;
                }
            }
        }

        if (!canPlace) {
            return false;
        }

        return mc.world.getOtherEntities(null, new Box(blockPos).stretch(0, protocol.get() ? 2 : 1, 0)).stream()
            .filter(entity -> !silentMap.containsKey(entity.getId()) && (!(entity instanceof EndCrystalEntity) || entity.age > 20)).count() == 0;
    }

    public boolean setCrystalSlot() {
        if (isOffhand()) {
            return true;
        }
        int crystalSlot = getCrystalSlot();
        if (crystalSlot == -1) {
            return false;
        } else if (mc.player.getInventory().selectedSlot != crystalSlot) {
            mc.player.getInventory().selectedSlot = crystalSlot;
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(crystalSlot));
            switchTimer.reset();
            noGhostTimer.reset();
        }
        return true;
    }

    public void setSwordSlot() {
        int swordSlot = getSwordSlot();
        if (mc.player.getInventory().selectedSlot != swordSlot && swordSlot != -1) {
            mc.player.getInventory().selectedSlot = swordSlot;
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(swordSlot));
            switchTimer.reset();
            noGhostTimer.reset();
        }
    }

    private int getSwordSlot() {
        int swordSlot = -1;

        if (mc.player.getMainHandStack().getItem() == Items.DIAMOND_SWORD) {
            swordSlot = mc.player.getInventory().selectedSlot;
        }

        if (swordSlot == -1) {
            for (int l = 0; l < 9; ++l) {
                if (mc.player.getInventory().getStack(l).getItem() == Items.DIAMOND_SWORD) {
                    swordSlot = l;
                    break;
                }
            }
        }

        return swordSlot;
    }

    private EndCrystalEntity findCrystalTarget(List<LivingEntity> targetsInRange, int adjustedResponseTime) {
        silentMap.forEach((id, time) -> {
            if (System.currentTimeMillis() - time > 1000) {
                silentMap.remove(id);
            }
        });

        EndCrystalEntity bestCrystal = null;

        if (inhibit.get() && !limit.get() && !inhibitTimer.hasPassed(adjustedResponseTime) && inhibitEntity != null) {
            if (mc.world.getEntityById(inhibitEntity.getId()) != null && isValidCrystalTarget(inhibitEntity)) {
                bestCrystal = inhibitEntity;
                return bestCrystal;
            }
        }

        List<EndCrystalEntity> crystalsInRange = getCrystalInRange();

        if (crystalsInRange.isEmpty()) {
            return null;
        }

        double bestDamage = 0.0D;

        for (EndCrystalEntity crystal : crystalsInRange) {
            if (crystal.getPos().distanceTo(LookCalculator.getEyesPos(mc.player)) < breakWallsRange.get() || RayTraceUtils.canSee(crystal)) {

                double selfDamage = DamageCalculator.getExplosionDamage(crystal, mc.player);

                if (!selfPlacePositions.contains(new BlockPos(crystal.getX(), crystal.getY() - 1, crystal.getZ())) && selfDamage > maxBreakSelfDamage.get()) {
                    continue;
                }

                double damage = 0.0D;

                for (LivingEntity target : targetsInRange) {
                    double targetDamage = DamageCalculator.getExplosionDamage(crystal, target);
                    damage += targetDamage;
                }

                if (onlyOwn.get()) {
                    if (!selfPlacePositions.contains(new BlockPos(crystal.getX(), crystal.getY() - 1, crystal.getZ()))) {
                        continue;
                    }
                } else {
                    if (!selfPlacePositions.contains(new BlockPos(crystal.getX(), crystal.getY() - 1, crystal.getZ())) && (damage < minPlaceDamage.get() || damage < selfDamage))
                        continue;
                }

                if (damage > bestDamage || bestDamage == 0D) {
                    bestDamage = damage;
                    bestCrystal = crystal;
                }
            }
        }

        return bestCrystal;
    }

    private List<EndCrystalEntity> getCrystalInRange() {
        List<EndCrystalEntity> list = new ArrayList<>();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof EndCrystalEntity)) continue;
            if (!isValidCrystalTarget((EndCrystalEntity) entity)) continue;
            list.add((EndCrystalEntity) entity);
        }

        return list;
    }

    private boolean isValidCrystalTarget(EndCrystalEntity crystal) {
        if (LookCalculator.getEyesPos(mc.player).distanceTo(crystal.getPos()) > breakRange.get()) return false;
        if (silentMap.containsKey(crystal.getId()) && limit.get()) return false;
        if (silentMap.containsKey(crystal.getId()) && crystal.age > ticksExisted.get() + attackFactor.get())
            return false;
        return !(DamageCalculator.getExplosionDamage(crystal, mc.player) + 2F >= mc.player.getHealth() + mc.player.getAbsorptionAmount());
    }

    private List<LivingEntity> getTargetsInRange() {
        List<LivingEntity> list = new ArrayList<>();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity)) continue;
            if (!shouldTarget(entity)) continue;
            if (entity.distanceTo(mc.player) > enemyRange.get()) continue;
            if (((LivingEntity) entity).isDead()) continue;
            if (((LivingEntity) entity).getHealth() + ((LivingEntity) entity).getAbsorptionAmount() <= 0) continue;
            list.add((LivingEntity) entity);
        }

        if (targetingMode.get() == TargetingMode.All) return list;
        else if (targetingMode.get() == TargetingMode.Health) {
            return list.stream().sorted(Comparator.comparing(e -> (e.getHealth() + e.getAbsorptionAmount()))).limit(1).collect(Collectors.toList());
        } else {
            return list.stream().sorted(Comparator.comparing(e -> (e.distanceTo(mc.player)))).limit(1).collect(Collectors.toList());
        }
    }

    private boolean shouldTarget(Entity entity) {
        if (entity instanceof PlayerEntity) {
            if (entity == mc.player) return false;

            if (Friends.get().isFriend((PlayerEntity) entity)) {
                return friends.get();
            }

            return players.get();
        }

        switch (entity.getType().getSpawnGroup()) {
            case CREATURE:
            case WATER_AMBIENT:
            case WATER_CREATURE:
                return creatures.get();
            case MONSTER:
                return monsters.get();
            case AMBIENT:
                return ambients.get();
            default:
                return false;
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!mc.isOnThread()) return;
        if (!render.get()) return;

        placeLocations.forEach((pos, time) -> {
            if (System.currentTimeMillis() - time > 1000) {
                placeLocations.remove(pos);
            } else {
                event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 1);
            }
        });

//        placeLocations.forEach((pos, time) -> {
//            if (System.currentTimeMillis() - time > 500) {
//                if (thick.get()) {
//                    Render2Utils.thick_box(event, pos, sideColor.get(), lineColor.get(), shapeMode.get());
//                } else {
//                    event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
//                }
//            }
//        });
    }

    private boolean isOffhand() {
        return mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL;
    }

    private int getCrystalSlot() {
        int crystalSlot = -1;

        if (mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL) {
            crystalSlot = mc.player.getInventory().selectedSlot;
        }


        if (crystalSlot == -1) {
            for (int l = 0; l < 9; ++l) {
                if (mc.player.getInventory().getStack(l).getItem() == Items.END_CRYSTAL) {
                    crystalSlot = l;
                    break;
                }
            }
        }

        return crystalSlot;
    }
}
