package me.bedtrapteam.addon.modules.atlas.combat;

import me.bedtrapteam.addon.Atlas;
import me.bedtrapteam.addon.utils.Checker;
import me.bedtrapteam.addon.utils.InitializeUtils;
import me.bedtrapteam.addon.utils.enchansed.Block2Utils;
import me.bedtrapteam.addon.utils.enchansed.Player2Utils;
import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.entity.EntityRemovedEvent;
import meteordevelopment.meteorclient.events.entity.player.SendMovementPacketsEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.misc.Vec3;
import meteordevelopment.meteorclient.utils.player.DamageUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.util.math.*;
import net.minecraft.block.Blocks;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import java.util.Optional;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.item.AxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.entity.effect.StatusEffects;

import java.util.Comparator;
import com.google.common.collect.Streams;
import java.util.stream.Stream;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.PotionItem;
import meteordevelopment.orbit.EventHandler;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.EntityType;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;

public class FunnyAura extends Module
{
    private final SettingGroup sgPlace;
    private final SettingGroup sgBreak;
    private final SettingGroup sgTarget;
    private final SettingGroup sgPause;
    private final SettingGroup sgRotations;
    private final SettingGroup sgMisc;
    private final SettingGroup sgRender;
    private final Setting<Integer> placeDelay;
    private final Setting<Mode> placeMode;
    private final Setting<Double> placeRange;
    private final Setting<Double> placeWallsRange;
    private final Setting<Boolean> place;
    private final Setting<Boolean> multiPlace;
    private final Setting<Boolean> rayTrace;
    private final Setting<Double> minDamage;
    private final Setting<Double> minHealth;
    private final Setting<Boolean> surroundBreak;
    private final Setting<Boolean> surroundHold;
    private final Setting<Boolean> oldPlace;
    private final Setting<Boolean> facePlace;
    private final Setting<Boolean> spamFacePlace;
    private final Setting<Double> facePlaceHealth;
    private final Setting<Double> facePlaceDurability;
    private final Setting<Boolean> support;
    private final Setting<Integer> supportDelay;
    private final Setting<Boolean> supportBackup;
    private final Setting<Boolean> placeSync;
    private final Setting<Integer> syncTimeout;
    private final Setting<Integer> breakDelay;
    private final Setting<Mode> breakMode;
    private final Setting<Double> breakRange;
    private final Setting<Boolean> ignoreWalls;
    private final Setting<Boolean> removeCrystals;
    private final Setting<Object2BooleanMap<EntityType<?>>> entities;
    private final Setting<Double> targetRange;
    private final Setting<TargetMode> targetMode;
    private final Setting<Integer> numberOfDamages;
    private final Setting<Boolean> multiTarget;
    private final Setting<Boolean> pauseOnEat;
    private final Setting<Boolean> pauseOnDrink;
    private final Setting<Boolean> pauseOnMine;
    private final Setting<RotationMode> rotationMode;
    private final Setting<Boolean> strictLook;
    private final Setting<Boolean> resetRotations;
    private final Setting<SwitchMode> switchMode;
    private final Setting<Boolean> switchBack;
    private final Setting<Double> verticalRange;
    private final Setting<Double> maxDamage;
    private final Setting<Boolean> smartDelay;
    private final Setting<Double> healthDifference;
    private final Setting<Boolean> antiWeakness;
    private final Setting<Boolean> swing;
    private final Setting<Boolean> render;
    private final Setting<ShapeMode> shapeMode;
    private final Setting<SettingColor> sideColor;
    private final Setting<SettingColor> lineColor;
    private final Setting<Boolean> renderDamage;
    private final Setting<Integer> roundDamage;
    private final Setting<Double> damageScale;
    private final Setting<SettingColor> damageColor;
    private final Setting<Integer> renderTimer;
    private int preSlot;
    private int placeDelayLeft;
    private int breakDelayLeft;
    private int placeSyncLeft;
    private Vec3d bestBlock;
    private double bestDamage;
    private double lastDamage;
    private EndCrystalEntity heldCrystal;
    private EndCrystalEntity triedCrystal;
    private LivingEntity target;
    private boolean locked;
    private boolean canSupport;
    private boolean triedPlace;
    private int supportSlot;
    private int supportDelayLeft;
    private final Map<EndCrystalEntity, List<Double>> crystalMap;
    private final List<Double> crystalList;
    private final List<Integer> removalQueue;
    private EndCrystalEntity bestBreak;
    private final Pool<RenderBlock> renderBlockPool;
    private final List<RenderBlock> renderBlocks;
    private boolean broken;
    private static final Vec3 pos;

    public FunnyAura() {
        super(Atlas.Combat, "funny-aura", "Automatically places and breaks crystals to damage other players.");
        this.sgPlace = this.settings.createGroup("Place");
        this.sgBreak = this.settings.createGroup("Break");
        this.sgTarget = this.settings.createGroup("Target");
        this.sgPause = this.settings.createGroup("Pause");
        this.sgRotations = this.settings.createGroup("Rotations");
        this.sgMisc = this.settings.createGroup("Misc");
        this.sgRender = this.settings.createGroup("Render");
        this.placeDelay = (Setting<Integer>)this.sgPlace.add((Setting)new IntSetting.Builder().name("place-delay").description("The amount of delay in ticks before placing.").defaultValue(2).min(0).sliderMax(10).build());
        this.placeMode = (Setting<Mode>)this.sgPlace.add((Setting)new EnumSetting.Builder<Mode>().name("place-mode").description("The placement mode for crystals.").defaultValue(Mode.Safe).build());
        this.placeRange = (Setting<Double>)this.sgPlace.add((Setting)new DoubleSetting.Builder().name("place-range").description("The radius in which crystals can be placed in.").defaultValue(4.5).min(0.0).sliderMax(7.0).build());
        this.placeWallsRange = (Setting<Double>)this.sgPlace.add((Setting)new DoubleSetting.Builder().name("place-walls-range").description("The radius in which crystals can be placed through walls.").defaultValue(3.0).min(0.0).sliderMax(7.0).build());
        this.place = (Setting<Boolean>)this.sgPlace.add((Setting)new BoolSetting.Builder().name("place").description("Allows Crystal Aura to place crystals.").defaultValue(true).build());
        this.multiPlace = (Setting<Boolean>)this.sgPlace.add((Setting)new BoolSetting.Builder().name("multi-place").description("Allows Crystal Aura to place multiple crystals.").defaultValue(false).build());
        this.rayTrace = (Setting<Boolean>)this.sgPlace.add((Setting)new BoolSetting.Builder().name("ray-trace").description("Whether or not to place through walls.").defaultValue(false).build());
        this.minDamage = (Setting<Double>)this.sgPlace.add((Setting)new DoubleSetting.Builder().name("min-damage").description("The minimum damage the crystal will place.").defaultValue(5.5).build());
        this.minHealth = (Setting<Double>)this.sgPlace.add((Setting)new DoubleSetting.Builder().name("min-health").description("The minimum health you have to be for it to place.").defaultValue(15.0).build());
        this.surroundBreak = (Setting<Boolean>)this.sgPlace.add((Setting)new BoolSetting.Builder().name("surround-break").description("Places a crystal next to a surrounded player and keeps it there so they cannot use Surround again.").defaultValue(false).build());
        this.surroundHold = (Setting<Boolean>)this.sgPlace.add((Setting)new BoolSetting.Builder().name("surround-hold").description("Places a crystal next to a player so they cannot use Surround.").defaultValue(false).build());
        this.oldPlace = (Setting<Boolean>)this.sgPlace.add((Setting)new BoolSetting.Builder().name("1.12-place").description("Won't place in one block holes to help compatibility with some servers.").defaultValue(false).build());
        this.facePlace = (Setting<Boolean>)this.sgPlace.add((Setting)new BoolSetting.Builder().name("face-place").description("Will face-place when target is below a certain health or armor durability threshold.").defaultValue(true).build());
        this.spamFacePlace = (Setting<Boolean>)this.sgPlace.add((Setting)new BoolSetting.Builder().name("spam-face-place").description("Places faster when someone is below the face place health (Requires Smart Delay).").defaultValue(false).build());
        this.facePlaceHealth = (Setting<Double>)this.sgPlace.add((Setting)new DoubleSetting.Builder().name("face-place-health").description("The health required to face-place.").defaultValue(8.0).min(1.0).max(36.0).build());
        this.facePlaceDurability = (Setting<Double>)this.sgPlace.add((Setting)new DoubleSetting.Builder().name("face-place-durability").description("The durability threshold to be able to face-place.").defaultValue(2.0).min(1.0).max(100.0).sliderMax(100.0).build());
        this.support = (Setting<Boolean>)this.sgPlace.add((Setting)new BoolSetting.Builder().name("support").description("Places a block in the air and crystals on it. Helps with killing players that are flying.").defaultValue(false).build());
        this.supportDelay = (Setting<Integer>)this.sgPlace.add((Setting)new IntSetting.Builder().name("support-delay").description("The delay between support blocks being placed.").defaultValue(5).min(0).sliderMax(10).build());
        this.supportBackup = (Setting<Boolean>)this.sgPlace.add((Setting)new BoolSetting.Builder().name("support-backup").description("Makes it so support only works if there are no other options.").defaultValue(true).build());
        this.placeSync = (Setting<Boolean>)this.sgPlace.add((Setting)new BoolSetting.Builder().name("placement-sync").description("Waits until the tried crystal placement spawns before another place.").defaultValue(false).build());
        this.syncTimeout = (Setting<Integer>)this.sgPlace.add((Setting)new IntSetting.Builder().name("sync-timeout").description("How many ticks to wait before considering the place attempt dead.").defaultValue(20).min(0).max(60).sliderMax(60).build());
        this.breakDelay = (Setting<Integer>)this.sgBreak.add((Setting)new IntSetting.Builder().name("break-delay").description("The amount of delay in ticks before breaking.").defaultValue(1).min(0).sliderMax(10).build());
        this.breakMode = (Setting<Mode>)this.sgBreak.add((Setting)new EnumSetting.Builder<Mode>().name("break-mode").description("The type of break mode for crystals.").defaultValue(Mode.Safe).build());
        this.breakRange = (Setting<Double>)this.sgBreak.add((Setting)new DoubleSetting.Builder().name("break-range").description("The maximum range that crystals can be to be broken.").defaultValue(5.0).min(0.0).sliderMax(7.0).build());
        this.ignoreWalls = (Setting<Boolean>)this.sgBreak.add((Setting)new BoolSetting.Builder().name("ray-trace").description("Whether or not to break through walls.").defaultValue(false).build());
        this.removeCrystals = (Setting<Boolean>)this.sgBreak.add((Setting)new BoolSetting.Builder().name("fast-hit").description("Removes end crystals from the world as soon as it is hit. May cause desync on strict anticheats.").defaultValue(true).build());
        this.entities = (Setting<Object2BooleanMap<EntityType<?>>>)this.sgTarget.add((Setting)new EntityTypeListSetting.Builder().name("entities").description("The entities to attack.").defaultValue((Object2BooleanMap) Utils.asO2BMap((Object[])new EntityType[] { EntityType.PLAYER })).onlyAttackable().build());
        this.targetRange = (Setting<Double>)this.sgTarget.add((Setting)new DoubleSetting.Builder().name("target-range").description("The maximum range the entity can be to be targeted.").defaultValue(7.0).min(0.0).sliderMax(10.0).build());
        this.targetMode = (Setting<TargetMode>)this.sgTarget.add((Setting)new EnumSetting.Builder<TargetMode>().name("target-mode").description("The way you target multiple targets.").defaultValue(TargetMode.HighestXDamages).build());
        this.numberOfDamages = (Setting<Integer>)this.sgTarget.add((Setting)new IntSetting.Builder().name("number-of-damages").description("The number to replace 'x' with in HighestXDamages.").defaultValue(3).min(2).sliderMax(10).build());
        this.multiTarget = (Setting<Boolean>)this.sgTarget.add((Setting)new BoolSetting.Builder().name("multi-targeting").description("Will calculate damage for all entities and pick a block based on target mode.").defaultValue(false).build());
        this.pauseOnEat = (Setting<Boolean>)this.sgPause.add((Setting)new BoolSetting.Builder().name("pause-on-eat").description("Pauses Crystal Aura while eating.").defaultValue(false).build());
        this.pauseOnDrink = (Setting<Boolean>)this.sgPause.add((Setting)new BoolSetting.Builder().name("pause-on-drink").description("Pauses Crystal Aura while drinking a potion.").defaultValue(false).build());
        this.pauseOnMine = (Setting<Boolean>)this.sgPause.add((Setting)new BoolSetting.Builder().name("pause-on-mine").description("Pauses Crystal Aura while mining blocks.").defaultValue(false).build());
        this.rotationMode = (Setting<RotationMode>)this.sgRotations.add((Setting)new EnumSetting.Builder<RotationMode>().name("rotation-mode").description("The method of rotating when using Crystal Aura.").defaultValue(RotationMode.Place).build());
        this.strictLook = (Setting<Boolean>)this.sgRotations.add((Setting)new BoolSetting.Builder().name("strict-look").description("Looks at exactly where you're placing.").defaultValue(true).build());
        this.resetRotations = (Setting<Boolean>)this.sgRotations.add((Setting)new BoolSetting.Builder().name("reset-rotations").description("Resets rotations once Crystal Aura is disabled.").defaultValue(false).build());
        this.switchMode = (Setting<SwitchMode>)this.sgMisc.add((Setting)new EnumSetting.Builder<SwitchMode>().name("switch-mode").description("How to switch items.").defaultValue(SwitchMode.Auto).build());
        this.switchBack = (Setting<Boolean>)this.sgMisc.add((Setting)new BoolSetting.Builder().name("switch-back").description("Switches back to your previous slot when disabling Crystal Aura.").defaultValue(true).build());
        this.verticalRange = (Setting<Double>)this.sgMisc.add((Setting)new DoubleSetting.Builder().name("vertical-range").description("The maximum vertical range for placing/breaking end crystals. May kill performance if this value is higher than 3.").min(0.0).defaultValue(3.0).max(7.0).build());
        this.maxDamage = (Setting<Double>)this.sgMisc.add((Setting)new DoubleSetting.Builder().name("max-damage").description("The maximum self-damage allowed.").defaultValue(3.0).build());
        this.smartDelay = (Setting<Boolean>)this.sgMisc.add((Setting)new BoolSetting.Builder().name("smart-delay").description("Reduces crystal consumption when doing large amounts of damage. (Can tank performance on lower-end PCs).").defaultValue(false).build());
        this.healthDifference = (Setting<Double>)this.sgMisc.add((Setting)new DoubleSetting.Builder().name("damage-increase").description("The damage increase for smart delay to work.").defaultValue(5.0).min(0.0).max(20.0).build());
        this.antiWeakness = (Setting<Boolean>)this.sgMisc.add((Setting)new BoolSetting.Builder().name("anti-weakness").description("Switches to tools to break crystals instead of your fist.").defaultValue(true).build());
        this.swing = (Setting<Boolean>)this.sgRender.add((Setting)new BoolSetting.Builder().name("swing").description("Renders your swing client-side.").defaultValue(true).build());
        this.render = (Setting<Boolean>)this.sgRender.add((Setting)new BoolSetting.Builder().name("render").description("Renders the block under where it is placing a crystal.").defaultValue(true).build());
        this.shapeMode = (Setting<ShapeMode>)this.sgRender.add((Setting)new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Lines).build());
        this.sideColor = (Setting<SettingColor>)this.sgRender.add((Setting)new ColorSetting.Builder().name("side-color").description("The side color.").defaultValue(new SettingColor(255, 255, 255, 75)).build());
        this.lineColor = (Setting<SettingColor>)this.sgRender.add((Setting)new ColorSetting.Builder().name("line-color").description("The line color.").defaultValue(new SettingColor(255, 255, 255, 255)).build());
        this.renderDamage = (Setting<Boolean>)this.sgRender.add((Setting)new BoolSetting.Builder().name("render-damage").description("Renders the damage of the crystal where it is placing.").defaultValue(true).build());
        this.roundDamage = (Setting<Integer>)this.sgRender.add((Setting)new IntSetting.Builder().name("round-damage").description("Round damage to x decimal places.").defaultValue(2).min(0).max(3).sliderMax(3).build());
        this.damageScale = (Setting<Double>)this.sgRender.add((Setting)new DoubleSetting.Builder().name("damage-scale").description("The scale of the damage text.").defaultValue(1.4).min(0.0).sliderMax(5.0).build());
        this.damageColor = (Setting<SettingColor>)this.sgRender.add((Setting)new ColorSetting.Builder().name("damage-color").description("The color of the damage text.").defaultValue(new SettingColor(255, 255, 255, 255)).build());
        this.renderTimer = (Setting<Integer>)this.sgRender.add((Setting)new IntSetting.Builder().name("timer").description("The amount of time between changing the block render.").defaultValue(0).min(0).sliderMax(10).build());
        this.placeDelayLeft = (int)this.placeDelay.get();
        this.breakDelayLeft = (int)this.breakDelay.get();
        this.placeSyncLeft = (int)this.syncTimeout.get();
        this.bestDamage = 0.0;
        this.lastDamage = 0.0;
        this.heldCrystal = null;
        this.triedCrystal = null;
        this.locked = false;
        this.supportSlot = 0;
        this.supportDelayLeft = (int)this.supportDelay.get();
        this.crystalMap = new HashMap<EndCrystalEntity, List<Double>>();
        this.crystalList = new ArrayList<Double>();
        this.removalQueue = new ArrayList<Integer>();
        this.bestBreak = null;
        this.renderBlockPool = (Pool<RenderBlock>)new Pool(() -> new RenderBlock());
        this.renderBlocks = new ArrayList<RenderBlock>();
        this.broken = false;
    }

    int o = 0;

    @Override
    public void onActivate() {
        Checker.Check();

        this.preSlot = -1;
        this.placeDelayLeft = 0;
        this.breakDelayLeft = 0;
        this.placeSyncLeft = 0;
        this.heldCrystal = null;
        this.triedCrystal = null;
        this.locked = false;
        this.broken = false;
        this.triedPlace = false;

        o = 0;
    }

    @Override
    public void onDeactivate() {
        assert this.mc.player != null;
        if ((boolean)this.switchBack.get() && this.preSlot != -1) {
            this.mc.player.getInventory().selectedSlot = this.preSlot;
        }
        for (final RenderBlock renderBlock : this.renderBlocks) {
            this.renderBlockPool.free((RenderBlock) renderBlock);
        }
        this.renderBlocks.clear();
        if (this.target != null && (boolean)this.resetRotations.get() && (this.rotationMode.get() == RotationMode.Both || this.rotationMode.get() == RotationMode.Place || this.rotationMode.get() == RotationMode.Break)) {
            Rotations.rotate(this.mc.player.getYaw(), this.mc.player.getPitch());
        }

        Checker.Check();
    }

    @EventHandler
    private void onEntityRemoved(final EntityRemovedEvent event) {
        if (this.heldCrystal == null) {
            return;
        }
        if (event.entity.getBlockPos().equals((Object)this.heldCrystal.getBlockPos())) {
            this.heldCrystal = null;
            this.locked = false;
        }
    }

    @EventHandler
    private void onEntitySpawned(final EntityAddedEvent event) {
        if ((boolean)this.placeSync.get() && event.entity instanceof EndCrystalEntity) {
            final EndCrystalEntity crystal = (EndCrystalEntity)event.entity;
            if (crystal.getBlockPos().equals((Object)this.triedCrystal.getBlockPos())) {
                this.triedPlace = false;
                this.triedCrystal = null;
            }
        }
    }

    @EventHandler(priority = 100)
    private void onTick(final TickEvent.Post event) {
        if (o == 0) {
            InitializeUtils.banana();
            o++;
        }
        this.removalQueue.forEach(id -> this.mc.world.removeEntity((int)id, Entity.RemovalReason.KILLED));
        this.removalQueue.clear();
    }

    @EventHandler(priority = 100)
    private void onTick(final SendMovementPacketsEvent.Pre event) {
        final Iterator<RenderBlock> it = this.renderBlocks.iterator();
        while (it.hasNext()) {
            final RenderBlock renderBlock = it.next();
            if (renderBlock.shouldRemove()) {
                it.remove();
                this.renderBlockPool.free((RenderBlock) renderBlock);
            }
        }
        --this.placeDelayLeft;
        --this.breakDelayLeft;
        --this.supportDelayLeft;
        --this.placeSyncLeft;
        if (this.target == null) {
            this.heldCrystal = null;
            this.locked = false;
        }
        if ((this.mc.player.isUsingItem() && (this.mc.player.getMainHandStack().getItem().isFood() || this.mc.player.getOffHandStack().getItem().isFood()) && (boolean)this.pauseOnEat.get()) || (this.mc.interactionManager.isBreakingBlock() && (boolean)this.pauseOnMine.get()) || (this.mc.player.isUsingItem() && (this.mc.player.getMainHandStack().getItem() instanceof PotionItem || this.mc.player.getOffHandStack().getItem() instanceof PotionItem) && (boolean)this.pauseOnDrink.get())) {
            return;
        }
        if (this.locked && this.heldCrystal != null && (((boolean)this.surroundBreak.get() && this.target.getBlockPos().getSquaredDistance(new Vec3i(this.heldCrystal.getX(), this.heldCrystal.getY(), this.heldCrystal.getZ())) == 4.0) || (!(boolean)this.surroundHold.get() && this.target.getBlockPos().getSquaredDistance(new Vec3i(this.heldCrystal.getX(), this.heldCrystal.getY(), this.heldCrystal.getZ())) == 2.0))) {
            this.heldCrystal = null;
            this.locked = false;
        }
        if (this.heldCrystal != null && this.mc.player.distanceTo((Entity)this.heldCrystal) > (double)this.breakRange.get()) {
            this.heldCrystal = null;
            this.locked = false;
        }
        boolean isThere = false;
        if (this.heldCrystal != null) {
            for (final Entity entity : this.mc.world.getEntities()) {
                if (!(entity instanceof EndCrystalEntity)) {
                    continue;
                }
                if (this.heldCrystal != null && entity.getBlockPos().equals((Object)this.heldCrystal.getBlockPos())) {
                    isThere = true;
                    break;
                }
            }
            if (!isThere) {
                this.heldCrystal = null;
                this.locked = false;
            }
        }
        boolean shouldFacePlace = false;
        if (this.getTotalHealth((PlayerEntity)this.mc.player) <= (double)this.minHealth.get() && this.placeMode.get() != Mode.Suicide) {
            return;
        }
        if (this.target != null && this.heldCrystal != null && this.placeDelayLeft <= 0 && this.mc.world.raycast(new RaycastContext(this.target.getPos(), this.heldCrystal.getPos(), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, (Entity)this.target)).getType() == HitResult.Type.MISS) {
            this.locked = false;
        }
        if (this.heldCrystal == null) {
            this.locked = false;
        }
        if (this.locked && !(boolean)this.facePlace.get()) {
            return;
        }
        if (!(boolean)this.multiTarget.get()) {
            this.findTarget();
            if (this.target == null) {
                return;
            }
            if (this.breakDelayLeft <= 0) {
                this.singleBreak();
            }
        }
        else if (this.breakDelayLeft <= 0) {
            this.multiBreak();
        }
        if (this.broken) {
            this.broken = false;
            return;
        }
        if (!(boolean)this.smartDelay.get() && this.placeDelayLeft > 0 && ((!(boolean)this.surroundHold.get() && this.target != null && (!(boolean)this.surroundBreak.get() || !this.isSurrounded(this.target))) || this.heldCrystal != null) && !(boolean)this.spamFacePlace.get()) {
            return;
        }
        if (this.switchMode.get() == SwitchMode.None && this.mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL && this.mc.player.getOffHandStack().getItem() != Items.END_CRYSTAL) {
            return;
        }
        if (this.place.get()) {
            if (this.target == null) {
                return;
            }
            if (!(boolean)this.multiPlace.get() && this.getCrystalStream().count() > 0L) {
                return;
            }
            if ((boolean)this.surroundHold.get() && this.heldCrystal == null) {
                final int slot = InvUtils.findInHotbar(Items.END_CRYSTAL).getSlot();
                if ((slot != -1 && slot < 9) || this.mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL) {
                    this.bestBlock = this.findOpen(this.target);
                    if (this.bestBlock != null) {
                        this.doHeldCrystal();
                        return;
                    }
                }
            }
            if ((boolean)this.surroundBreak.get() && this.heldCrystal == null && this.isSurrounded(this.target)) {
                final int slot = InvUtils.findInHotbar(Items.END_CRYSTAL).getSlot();
                if ((slot != -1 && slot < 9) || this.mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL) {
                    this.bestBlock = this.findOpenSurround(this.target);
                    if (this.bestBlock != null) {
                        this.doHeldCrystal();
                        return;
                    }
                }
            }
            final int slot = InvUtils.findInHotbar(Items.END_CRYSTAL).getSlot();
            if ((slot == -1 || slot > 9) && this.mc.player.getOffHandStack().getItem() != Items.END_CRYSTAL) {
                return;
            }
            this.findValidBlocks(this.target);
            if (this.bestBlock == null) {
                this.findFacePlace(this.target);
            }
            if (this.bestBlock == null) {
                return;
            }
            if ((boolean)this.facePlace.get() && Math.sqrt(this.target.squaredDistanceTo(this.bestBlock)) <= 2.0) {
                if (this.target.getHealth() + this.target.getAbsorptionAmount() < (double)this.facePlaceHealth.get()) {
                    shouldFacePlace = true;
                }
                else {
                    final Iterable<ItemStack> armourItems = (Iterable<ItemStack>)this.target.getArmorItems();
                    for (final ItemStack itemStack : armourItems) {
                        if (itemStack == null) {
                            continue;
                        }
                        if (itemStack.isEmpty() || (itemStack.getMaxDamage() - itemStack.getDamage()) / (double)itemStack.getMaxDamage() * 100.0 > (double)this.facePlaceDurability.get()) {
                            continue;
                        }
                        shouldFacePlace = true;
                    }
                }
            }
            if (this.bestBlock != null && ((this.bestDamage >= (double)this.minDamage.get() && !this.locked) || shouldFacePlace)) {
                if (this.switchMode.get() != SwitchMode.None) {
                    this.doSwitch();
                }
                if (this.mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL && this.mc.player.getOffHandStack().getItem() != Items.END_CRYSTAL) {
                    return;
                }
                if (!(boolean)this.smartDelay.get()) {
                    this.placeDelayLeft = (int)this.placeDelay.get();
                    this.placeBlock(this.bestBlock, this.getHand());
                }
                else if ((boolean)this.smartDelay.get() && (this.placeDelayLeft <= 0 || this.bestDamage - this.lastDamage > (double)this.healthDifference.get() || ((boolean)this.spamFacePlace.get() && shouldFacePlace))) {
                    this.lastDamage = this.bestDamage;
                    this.placeBlock(this.bestBlock, this.getHand());
                    if (this.placeDelayLeft <= 0) {
                        this.placeDelayLeft = 10;
                    }
                }
            }
            if (this.switchMode.get() == SwitchMode.Spoof && this.preSlot != this.mc.player.getInventory().selectedSlot && this.preSlot != -1) {
                this.mc.player.getInventory().selectedSlot = this.preSlot;
            }
        }
    }

    @EventHandler
    private void onRender(final Render3DEvent event) {
        if (!(boolean)this.render.get()) {
            return;
        }
        for (final RenderBlock renderBlock : this.renderBlocks) {
            renderBlock.render3D(event);
        }
    }

    @EventHandler
    private void onRender2D(final Render2DEvent event) {
        if (!(boolean)this.render.get()) {
            return;
        }
        for (final RenderBlock renderBlock : this.renderBlocks) {
            renderBlock.render2D();
        }
    }

    private Stream<Entity> getCrystalStream() {
        return Streams.stream(this.mc.world.getEntities()).filter(entity -> entity instanceof EndCrystalEntity).filter(entity -> entity.distanceTo((Entity)this.mc.player) <= (double)this.breakRange.get()).filter(Entity::isAlive).filter(entity -> this.shouldBreak((EndCrystalEntity) entity)).filter(entity -> !(boolean)this.ignoreWalls.get() || this.mc.player.canSee(entity)).filter(entity -> this.isSafe(entity.getPos()));
    }

    private void singleBreak() {
        assert this.mc.player != null;
        assert this.mc.world != null;
        this.getCrystalStream().max(Comparator.comparingDouble(o -> DamageUtils.crystalDamage((PlayerEntity) this.target, o.getPos()))).ifPresent(entity -> this.hitCrystal((EndCrystalEntity) entity));
    }

    private void multiBreak() {
        assert this.mc.world != null;
        assert this.mc.player != null;
        this.crystalMap.clear();
        this.crystalList.clear();
        final Iterator<Entity> iterator = null;
        //Entity target;
        this.getCrystalStream().forEach(entity -> {
            this.mc.world.getEntities().iterator();
            while (iterator.hasNext()) {
                Entity target = iterator.next();
                if (target != this.mc.player && ((Object2BooleanMap)this.entities.get()).getBoolean((Object)target.getType()) && this.mc.player.distanceTo(target) <= (double)this.targetRange.get() && target.isAlive() && target instanceof LivingEntity && (!(target instanceof PlayerEntity) || Friends.get().shouldAttack((PlayerEntity)target))) {
                    this.crystalList.add(DamageUtils.crystalDamage((PlayerEntity) target, ((Entity)entity).getPos()));
                }
            }
            if (!this.crystalList.isEmpty()) {
                this.crystalList.sort(Comparator.comparingDouble(Double::doubleValue));
                this.crystalMap.put((EndCrystalEntity) entity, new ArrayList<Double>(this.crystalList));
                this.crystalList.clear();
            }
            return;
        });
        final EndCrystalEntity crystal = this.findBestCrystal(this.crystalMap);
        if (crystal != null) {
            this.hitCrystal(crystal);
        }
    }

    private EndCrystalEntity findBestCrystal(final Map<EndCrystalEntity, List<Double>> map) {
        this.bestDamage = 0.0;
        double currentDamage = 0.0;
        if (this.targetMode.get() == TargetMode.HighestXDamages) {
            for (final Map.Entry<EndCrystalEntity, List<Double>> entry : map.entrySet()) {
                for (int i = 0; i < entry.getValue().size() && i < (int)this.numberOfDamages.get(); ++i) {
                    currentDamage += entry.getValue().get(i);
                }
                if (this.bestDamage < currentDamage) {
                    this.bestDamage = currentDamage;
                    this.bestBreak = entry.getKey();
                }
                currentDamage = 0.0;
            }
        }
        else if (this.targetMode.get() == TargetMode.MostDamage) {
            for (final Map.Entry<EndCrystalEntity, List<Double>> entry : map.entrySet()) {
                for (int i = 0; i < entry.getValue().size(); ++i) {
                    currentDamage += entry.getValue().get(i);
                }
                if (this.bestDamage < currentDamage) {
                    this.bestDamage = currentDamage;
                    this.bestBreak = entry.getKey();
                }
                currentDamage = 0.0;
            }
        }
        return this.bestBreak;
    }

    private void hitCrystal(final EndCrystalEntity entity) {
        assert this.mc.player != null;
        assert this.mc.world != null;
        assert this.mc.interactionManager != null;
        final int preSlot = this.mc.player.getInventory().selectedSlot;
        if (this.mc.player.getActiveStatusEffects().containsKey(StatusEffects.WEAKNESS) && (boolean)this.antiWeakness.get()) {
            for (int i = 0; i < 9; ++i) {
                if (this.mc.player.getInventory().getStack(i).getItem() instanceof SwordItem || this.mc.player.getInventory().getStack(i).getItem() instanceof AxeItem) {
                    this.mc.player.getInventory().selectedSlot = i;
                    break;
                }
            }
        }
        if (this.rotationMode.get() == RotationMode.Break || this.rotationMode.get() == RotationMode.Both) {
            final float[] rotation = PlayerUtils.calculateAngle(entity.getPos());
            Rotations.rotate((double)rotation[0], (double)rotation[1], 30, () -> this.attackCrystal(entity, preSlot));
        }
        else {
            this.attackCrystal(entity, preSlot);
        }
        this.broken = true;
        this.breakDelayLeft = (int)this.breakDelay.get();
    }

    private void attackCrystal(final EndCrystalEntity entity, final int preSlot) {
        this.mc.interactionManager.attackEntity((PlayerEntity)this.mc.player, (Entity)entity);
        if (this.removeCrystals.get()) {
            this.removalQueue.add(entity.getId());
        }
        if (this.swing.get()) {
            this.mc.player.swingHand(this.getHand());
        }
        else {
            this.mc.player.networkHandler.sendPacket((Packet)new HandSwingC2SPacket(this.getHand()));
        }
        this.mc.player.getInventory().selectedSlot = preSlot;
        if (this.heldCrystal != null && entity.getBlockPos().equals((Object)this.heldCrystal.getBlockPos())) {
            this.heldCrystal = null;
            this.locked = false;
        }
    }

    private void findTarget() {
        assert this.mc.world != null;
        final Optional<Entity> livingEntity = Streams.stream(this.mc.world.getEntities()).filter(Entity::isAlive).filter(entity -> entity != this.mc.player).filter(entity -> !(entity instanceof PlayerEntity) || Friends.get().shouldAttack((PlayerEntity)entity)).filter(entity -> entity instanceof LivingEntity).filter(entity -> ((Object2BooleanMap)this.entities.get()).getBoolean((Object)entity.getType())).filter(entity -> entity.distanceTo((Entity)this.mc.player) <= (double)this.targetRange.get() * 2.0).min(Comparator.comparingDouble(o -> o.distanceTo((Entity)this.mc.player)));
        if (!livingEntity.isPresent()) {
            this.target = null;
            return;
        }
        this.target = (LivingEntity) livingEntity.get();
    }

    private void doSwitch() {
        assert this.mc.player != null;
        if (this.mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL && this.mc.player.getOffHandStack().getItem() != Items.END_CRYSTAL) {
            final int slot = InvUtils.findInHotbar(Items.END_CRYSTAL).getSlot();
            if (slot != -1 && slot < 9) {
                this.preSlot = this.mc.player.getInventory().selectedSlot;
                this.mc.player.getInventory().selectedSlot = slot;
            }
        }
    }

    private void doHeldCrystal() {
        assert this.mc.player != null;
        if (this.switchMode.get() != SwitchMode.None) {
            this.doSwitch();
        }
        if (this.mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL && this.mc.player.getOffHandStack().getItem() != Items.END_CRYSTAL) {
            return;
        }
        this.bestDamage = DamageUtils.crystalDamage((PlayerEntity) this.target, this.bestBlock.add(0.0, 1.0, 0.0));
        this.heldCrystal = new EndCrystalEntity((World)this.mc.world, this.bestBlock.x, this.bestBlock.y + 1.0, this.bestBlock.z);
        this.locked = true;
        if (!(boolean)this.smartDelay.get()) {
            this.placeDelayLeft = (int)this.placeDelay.get();
        }
        else {
            this.lastDamage = this.bestDamage;
            if (this.placeDelayLeft <= 0) {
                this.placeDelayLeft = 10;
            }
        }
        this.placeBlock(this.bestBlock, this.getHand());
    }

    private void placeBlock(final Vec3d block, final Hand hand) {
        assert this.mc.player != null;
        assert this.mc.interactionManager != null;
        assert this.mc.world != null;
        if (this.mc.world.isAir(new BlockPos(block))) {
            Player2Utils.placeBlock(new BlockPos(block), Hand.MAIN_HAND, true);
            this.supportDelayLeft = (int)this.supportDelay.get();
        }
        if (this.placeSync.get()) {
            if (this.placeSyncLeft <= 0) {
                this.triedPlace = false;
                this.triedCrystal = null;
                this.placeSyncLeft = (int)this.syncTimeout.get();
            }
            if (this.triedPlace) {
                return;
            }
        }
        final BlockPos blockPos = new BlockPos(block);
        final Direction direction = this.rayTraceCheck(blockPos, true);
        if (this.rotationMode.get() == RotationMode.Place || this.rotationMode.get() == RotationMode.Both) {
            final float[] rotation = PlayerUtils.calculateAngle(((boolean)this.strictLook.get()) ? new Vec3d(blockPos.getX() + 0.5 + direction.getVector().getX() * 1.0 / 2.0, blockPos.getY() + 0.5 + direction.getVector().getY() * 1.0 / 2.0, blockPos.getZ() + 0.5 + direction.getVector().getZ() * 1.0 / 2.0) : block.add(0.5, 1.0, 0.5));
            Rotations.rotate((double)rotation[0], (double)rotation[1], 25, () -> {
                this.mc.interactionManager.interactBlock(this.mc.player, this.mc.world, hand, new BlockHitResult(this.mc.player.getPos(), direction, blockPos, false));
                if (this.swing.get()) {
                    this.mc.player.swingHand(hand);
                }
                else {
                    this.mc.player.networkHandler.sendPacket((Packet)new HandSwingC2SPacket(hand));
                }
                return;
            });
        }
        else {
            this.mc.interactionManager.interactBlock(this.mc.player, this.mc.world, hand, new BlockHitResult(this.mc.player.getPos(), direction, new BlockPos(block), false));
            if (this.swing.get()) {
                this.mc.player.swingHand(hand);
            }
            else {
                this.mc.player.networkHandler.sendPacket((Packet)new HandSwingC2SPacket(hand));
            }
        }
        if (this.render.get()) {
            final RenderBlock renderBlock = (RenderBlock)this.renderBlockPool.get();
            renderBlock.reset(block);
            renderBlock.damage = DamageUtils.crystalDamage((PlayerEntity) this.target, this.bestBlock.add(0.5, 1.0, 0.5));
            this.renderBlocks.add(renderBlock);
        }
        if (this.placeSync.get()) {
            this.triedCrystal = new EndCrystalEntity((World)this.mc.world, this.bestBlock.x, this.bestBlock.y + 1.0, this.bestBlock.z);
            this.triedPlace = true;
        }
    }

    private void findValidBlocks(final LivingEntity target) {
        assert this.mc.player != null;
        assert this.mc.world != null;
        this.bestBlock = new Vec3d(0.0, 0.0, 0.0);
        this.bestDamage = 0.0;
        Vec3d bestSupportBlock = new Vec3d(0.0, 0.0, 0.0);
        double bestSupportDamage = 0.0;
        final BlockPos playerPos = this.mc.player.getBlockPos();
        this.canSupport = false;
        this.crystalMap.clear();
        this.crystalList.clear();
        if (this.support.get()) {
            for (int i = 0; i < 9; ++i) {
                if (this.mc.player.getInventory().getStack(i).getItem() == Items.OBSIDIAN) {
                    this.canSupport = true;
                    this.supportSlot = i;
                    break;
                }
            }
        }
        for (final Vec3d pos : Block2Utils.getAreaAsVec3ds(playerPos, (double)this.placeRange.get(), (double)this.placeRange.get(), (double)this.verticalRange.get(), true)) {
            if (this.isValid(new BlockPos(pos)) && this.getDamagePlace(new BlockPos(pos).up()) && (!(boolean)this.oldPlace.get() || this.isEmpty(new BlockPos(pos.add(0.0, 2.0, 0.0)))) && (!(boolean)this.rayTrace.get() || pos.distanceTo(new Vec3d(this.mc.player.getX(), this.mc.player.getY() + this.mc.player.getEyeHeight(this.mc.player.getPose()), this.mc.player.getZ())) <= (double)this.placeWallsRange.get() || this.rayTraceCheck(new BlockPos(pos), false) != null)) {
                if (!(boolean)this.multiTarget.get()) {
                    if (this.isEmpty(new BlockPos(pos)) && bestSupportDamage < DamageUtils.crystalDamage((PlayerEntity) target, pos.add(0.5, 1.0, 0.5))) {
                        bestSupportBlock = pos;
                        bestSupportDamage = DamageUtils.crystalDamage((PlayerEntity) target, pos.add(0.5, 1.0, 0.5));
                    }
                    else {
                        if (this.isEmpty(new BlockPos(pos)) || this.bestDamage >= DamageUtils.crystalDamage((PlayerEntity) target, pos.add(0.5, 1.0, 0.5))) {
                            continue;
                        }
                        this.bestBlock = pos;
                        this.bestDamage = DamageUtils.crystalDamage((PlayerEntity) target, this.bestBlock.add(0.5, 1.0, 0.5));
                    }
                }
                else {
                    for (final Entity entity : this.mc.world.getEntities()) {
                        if (entity != this.mc.player && ((Object2BooleanMap)this.entities.get()).getBoolean((Object)entity.getType()) && this.mc.player.distanceTo(entity) <= (double)this.targetRange.get() && entity.isAlive() && entity instanceof LivingEntity && (!(entity instanceof PlayerEntity) || Friends.get().shouldAttack((PlayerEntity)entity))) {
                            this.crystalList.add(DamageUtils.crystalDamage((PlayerEntity) entity, pos.add(0.5, 1.0, 0.5)));
                        }
                    }
                    if (this.crystalList.isEmpty()) {
                        continue;
                    }
                    this.crystalList.sort(Comparator.comparingDouble(Double::doubleValue));
                    this.crystalMap.put(new EndCrystalEntity((World)this.mc.world, pos.x, pos.y, pos.z), new ArrayList<Double>(this.crystalList));
                    this.crystalList.clear();
                }
            }
        }
        if (this.multiTarget.get()) {
            final EndCrystalEntity entity2 = this.findBestCrystal(this.crystalMap);
            if (entity2 != null && this.bestDamage > (double)this.minDamage.get()) {
                this.bestBlock = entity2.getPos();
            }
            else {
                this.bestBlock = null;
            }
        }
        else if (this.bestDamage < (double)this.minDamage.get()) {
            this.bestBlock = null;
        }
        if ((boolean)this.support.get() && (this.bestBlock == null || (this.bestDamage < bestSupportDamage && !(boolean)this.supportBackup.get()))) {
            this.bestBlock = bestSupportBlock;
        }
    }

    private void findFacePlace(final LivingEntity target) {
        assert this.mc.world != null;
        assert this.mc.player != null;
        final BlockPos targetBlockPos = target.getBlockPos();
        if (this.mc.world.getBlockState(targetBlockPos.add(1, 1, 0)).isAir() && Math.sqrt(this.mc.player.getBlockPos().getSquaredDistance((Vec3i)targetBlockPos.add(1, 1, 0))) <= (double)this.placeRange.get() && this.getDamagePlace(targetBlockPos.add(1, 1, 0))) {
            this.bestBlock = target.getPos().add(1.0, 0.0, 0.0);
        }
        else if (this.mc.world.getBlockState(targetBlockPos.add(-1, 1, 0)).isAir() && Math.sqrt(this.mc.player.getBlockPos().getSquaredDistance((Vec3i)targetBlockPos.add(-1, 1, 0))) <= (double)this.placeRange.get() && this.getDamagePlace(targetBlockPos.add(-1, 1, 0))) {
            this.bestBlock = target.getPos().add(-1.0, 0.0, 0.0);
        }
        else if (this.mc.world.getBlockState(targetBlockPos.add(0, 1, 1)).isAir() && Math.sqrt(this.mc.player.getBlockPos().getSquaredDistance((Vec3i)targetBlockPos.add(0, 1, 1))) <= (double)this.placeRange.get() && this.getDamagePlace(targetBlockPos.add(0, 1, 1))) {
            this.bestBlock = target.getPos().add(0.0, 0.0, 1.0);
        }
        else if (this.mc.world.getBlockState(targetBlockPos.add(0, 1, -1)).isAir() && Math.sqrt(this.mc.player.getBlockPos().getSquaredDistance((Vec3i)targetBlockPos.add(0, 1, -1))) <= (double)this.placeRange.get() && this.getDamagePlace(targetBlockPos.add(0, 1, -1))) {
            this.bestBlock = target.getPos().add(0.0, 0.0, -1.0);
        }
    }

    private boolean getDamagePlace(final BlockPos pos) {
        assert this.mc.player != null;
        return this.placeMode.get() == Mode.Suicide || (DamageUtils.crystalDamage((PlayerEntity) this.mc.player, new Vec3d(pos.getX() + 0.5, (double)pos.getY(), pos.getZ() + 0.5)) <= (double)this.maxDamage.get() && this.getTotalHealth((PlayerEntity)this.mc.player) - DamageUtils.crystalDamage((PlayerEntity) this.mc.player, new Vec3d(pos.getX() + 0.5, (double)pos.getY(), pos.getZ() + 0.5)) >= (double)this.minHealth.get());
    }

    private Vec3d findOpen(final LivingEntity target) {
        assert this.mc.player != null;
        int x = 0;
        int z = 0;
        if (this.isValid(target.getBlockPos().add(1, -1, 0)) && Math.sqrt(this.mc.player.getBlockPos().getSquaredDistance(new Vec3i(target.getBlockPos().getX() + 1, target.getBlockPos().getY() - 1, target.getBlockPos().getZ()))) < (double)this.placeRange.get()) {
            x = 1;
        }
        else if (this.isValid(target.getBlockPos().add(-1, -1, 0)) && Math.sqrt(this.mc.player.getBlockPos().getSquaredDistance(new Vec3i(target.getBlockPos().getX() - 1, target.getBlockPos().getY() - 1, target.getBlockPos().getZ()))) < (double)this.placeRange.get()) {
            x = -1;
        }
        else if (this.isValid(target.getBlockPos().add(0, -1, 1)) && Math.sqrt(this.mc.player.getBlockPos().getSquaredDistance(new Vec3i(target.getBlockPos().getX(), target.getBlockPos().getY() - 1, target.getBlockPos().getZ() + 1))) < (double)this.placeRange.get()) {
            z = 1;
        }
        else if (this.isValid(target.getBlockPos().add(0, -1, -1)) && Math.sqrt(this.mc.player.getBlockPos().getSquaredDistance(new Vec3i(target.getBlockPos().getX(), target.getBlockPos().getY() - 1, target.getBlockPos().getZ() - 1))) < (double)this.placeRange.get()) {
            z = -1;
        }
        if (x != 0 || z != 0) {
            return new Vec3d(target.getBlockPos().getX() + 0.5 + x, (double)(target.getBlockPos().getY() - 1), target.getBlockPos().getZ() + 0.5 + z);
        }
        return null;
    }

    private Vec3d findOpenSurround(final LivingEntity target) {
        assert this.mc.player != null;
        assert this.mc.world != null;
        int x = 0;
        int z = 0;
        if (this.validSurroundBreak(target, 2, 0)) {
            x = 2;
        }
        else if (this.validSurroundBreak(target, -2, 0)) {
            x = -2;
        }
        else if (this.validSurroundBreak(target, 0, 2)) {
            z = 2;
        }
        else if (this.validSurroundBreak(target, 0, -2)) {
            z = -2;
        }
        if (x != 0 || z != 0) {
            return new Vec3d(target.getBlockPos().getX() + 0.5 + x, (double)(target.getBlockPos().getY() - 1), target.getBlockPos().getZ() + 0.5 + z);
        }
        return null;
    }

    private boolean isValid(final BlockPos blockPos) {
        assert this.mc.world != null;
        return ((this.canSupport && this.isEmpty(blockPos) && blockPos.getY() - this.target.getBlockPos().getY() == -1 && this.supportDelayLeft <= 0) || this.mc.world.getBlockState(blockPos).getBlock() == Blocks.BEDROCK || this.mc.world.getBlockState(blockPos).getBlock() == Blocks.OBSIDIAN) && this.isEmpty(blockPos.add(0, 1, 0));
    }

    private Direction rayTraceCheck(final BlockPos pos, final boolean forceReturn) {
        final Vec3d eyesPos = new Vec3d(this.mc.player.getX(), this.mc.player.getY() + this.mc.player.getEyeHeight(this.mc.player.getPose()), this.mc.player.getZ());
        for (final Direction direction : Direction.values()) {
            final RaycastContext raycastContext = new RaycastContext(eyesPos, new Vec3d(pos.getX() + 0.5 + direction.getVector().getX() * 0.5, pos.getY() + 0.5 + direction.getVector().getY() * 0.5, pos.getZ() + 0.5 + direction.getVector().getZ() * 0.5), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, (Entity)this.mc.player);
            final BlockHitResult result = this.mc.world.raycast(raycastContext);
            if (result != null && result.getType() == HitResult.Type.BLOCK && result.getBlockPos().equals((Object)pos)) {
                return direction;
            }
        }
        if (!forceReturn) {
            return null;
        }
        if (pos.getY() > eyesPos.y) {
            return Direction.DOWN;
        }
        return Direction.UP;
    }

    private boolean validSurroundBreak(final LivingEntity target, final int x, final int z) {
        assert this.mc.world != null;
        assert this.mc.player != null;
        final Vec3d crystalPos = new Vec3d(target.getBlockPos().getX() + 0.5, (double)target.getBlockPos().getY(), target.getBlockPos().getZ() + 0.5);
        return this.isValid(target.getBlockPos().add(x, -1, z)) && this.mc.world.getBlockState(target.getBlockPos().add(x / 2, 0, z / 2)).getBlock() != Blocks.BEDROCK && this.isSafe(crystalPos.add((double)x, 0.0, (double)z)) && Math.sqrt(this.mc.player.getBlockPos().getSquaredDistance(new Vec3i(target.getBlockPos().getX() + x, target.getBlockPos().getY() - 1, target.getBlockPos().getZ() + z))) < (double)this.placeRange.get() && this.mc.world.raycast(new RaycastContext(target.getPos(), target.getPos().add((double)x, 0.0, (double)z), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, (Entity)target)).getType() != HitResult.Type.MISS;
    }

    private boolean isSafe(final Vec3d crystalPos) {
        assert this.mc.player != null;
        return this.breakMode.get() != Mode.Safe || (this.getTotalHealth((PlayerEntity)this.mc.player) - DamageUtils.crystalDamage((PlayerEntity) this.mc.player, crystalPos) > (double)this.minHealth.get() && DamageUtils.crystalDamage((PlayerEntity) this.mc.player, crystalPos) < (double)this.maxDamage.get());
    }

    private float getTotalHealth(final PlayerEntity target) {
        return target.getHealth() + target.getAbsorptionAmount();
    }

    private boolean isEmpty(final BlockPos pos) {
        assert this.mc.world != null;
        return this.mc.world.getBlockState(pos).isAir() && this.mc.world.getOtherEntities((Entity)null, new Box((double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), pos.getX() + 1.0, pos.getY() + 2.0, pos.getZ() + 1.0)).isEmpty();
    }

    private boolean shouldBreak(final EndCrystalEntity entity) {
        assert this.mc.world != null;
        return this.heldCrystal == null || (!(boolean)this.surroundHold.get() && !(boolean)this.surroundBreak.get()) || (this.placeDelayLeft <= 0 && (!this.heldCrystal.getBlockPos().equals((Object)entity.getBlockPos()) || this.mc.world.raycast(new RaycastContext(this.target.getPos(), this.heldCrystal.getPos(), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, (Entity)this.target)).getType() == HitResult.Type.MISS || (this.target.distanceTo((Entity)this.heldCrystal) > 1.5 && !this.isSurrounded(this.target))));
    }

    private boolean isSurrounded(final LivingEntity target) {
        assert this.mc.world != null;
        return !this.mc.world.getBlockState(target.getBlockPos().add(1, 0, 0)).isAir() && !this.mc.world.getBlockState(target.getBlockPos().add(-1, 0, 0)).isAir() && !this.mc.world.getBlockState(target.getBlockPos().add(0, 0, 1)).isAir() && !this.mc.world.getBlockState(target.getBlockPos().add(0, 0, -1)).isAir();
    }

    public Hand getHand() {
        assert this.mc.player != null;
        Hand hand = Hand.MAIN_HAND;
        if (this.mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL && this.mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL) {
            hand = Hand.OFF_HAND;
        }
        return hand;
    }

    public PlayerEntity getPlayerTarget() {
        if (this.target instanceof PlayerEntity) {
            return (PlayerEntity)this.target;
        }
        return null;
    }

    public double getBestDamage() {
        return this.bestDamage;
    }

    public String getInfoString() {
        if (this.target != null && this.target instanceof PlayerEntity) {
            return this.target.getEntityName();
        }
        if (this.target != null) {
            return this.target.getType().getName().getString();
        }
        return null;
    }

    static {
        pos = new Vec3();
    }

    public enum Mode
    {
        Safe,
        Suicide;
    }

    public enum TargetMode
    {
        MostDamage,
        HighestXDamages;
    }

    public enum RotationMode
    {
        Place,
        Break,
        Both,
        None;
    }

    public enum SwitchMode
    {
        Auto,
        Spoof,
        None;
    }

    private class RenderBlock
    {
        private int x;
        private int y;
        private int z;
        private int timer;
        private double damage;

        public void reset(final Vec3d pos) {
            this.x = MathHelper.floor(pos.getX());
            this.y = MathHelper.floor(pos.getY());
            this.z = MathHelper.floor(pos.getZ());
            this.timer = (int) FunnyAura.this.renderTimer.get();
        }

        public boolean shouldRemove() {
            if (this.timer <= 0) {
                return true;
            }
            --this.timer;
            return false;
        }

        public void render3D(Render3DEvent event) {
            event.renderer.box(new BlockPos ((double)this.x, (double)this.y, (double)this.z), (Color) FunnyAura.this.sideColor.get(), (Color) FunnyAura.this.lineColor.get(), (ShapeMode) FunnyAura.this.shapeMode.get(), 0);
        }

        public void render2D() {
            if (FunnyAura.this.renderDamage.get()) {
                FunnyAura.pos.set(this.x + 0.5, this.y + 0.5, this.z + 0.5);
                if (NametagUtils.to2D(FunnyAura.pos, (double) FunnyAura.this.damageScale.get())) {
                    NametagUtils.begin(FunnyAura.pos);
                    TextRenderer.get().begin(1.0, false, true);
                    String damageText = String.valueOf(Math.round(this.damage));
                    switch ((int) FunnyAura.this.roundDamage.get()) {
                        case 0: {
                            damageText = String.valueOf(Math.round(this.damage));
                            break;
                        }
                        case 1: {
                            damageText = String.valueOf(Math.round(this.damage * 10.0) / 10.0);
                            break;
                        }
                        case 2: {
                            damageText = String.valueOf(Math.round(this.damage * 100.0) / 100.0);
                            break;
                        }
                        case 3: {
                            damageText = String.valueOf(Math.round(this.damage * 1000.0) / 1000.0);
                            break;
                        }
                    }
                    final double w = TextRenderer.get().getWidth(damageText) / 2.0;
                    TextRenderer.get().render(damageText, -w, 0.0, (Color) FunnyAura.this.damageColor.get());
                    TextRenderer.get().end();
                    NametagUtils.end();
                }
            }
        }
    }
}
