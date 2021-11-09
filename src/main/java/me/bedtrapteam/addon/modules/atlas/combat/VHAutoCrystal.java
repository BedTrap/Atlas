package me.bedtrapteam.addon.modules.atlas.combat;

import com.google.common.util.concurrent.AtomicDouble;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import me.bedtrapteam.addon.Atlas;
import me.bedtrapteam.addon.utils.enchansed.Block2Utils;
import me.bedtrapteam.addon.utils.enchansed.Render2Utils;
import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.entity.player.SendMovementPacketsEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerManager;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.Vec3;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class VHAutoCrystal extends Module {
    public enum Mode {Safe, Suicide}

    public enum RotationMode {None, Place, Break, Both}

    public enum SwitchMode {None, Auto}

    public enum Logic {PlaceBreak, BreakPlace}

    public enum Canceller {NoDesync, HitCanceller}

    public enum Type {None, Place, Break, Both}

    public enum BreakHand {Mainhand, Offhand, Auto}

    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final SettingGroup sgBreak = settings.createGroup("Break");
    private final SettingGroup sgMisc = settings.createGroup("Misc");
    private final SettingGroup sgTarget = settings.createGroup("Target");
    private final SettingGroup sgFacePlace = settings.createGroup("FacePlace");
    private final SettingGroup sgSupport = settings.createGroup("Support");
    private final SettingGroup sgSurround = settings.createGroup("Surround");
    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final SettingGroup sgSwitch = settings.createGroup("Switch");
    private final SettingGroup sgRotations = settings.createGroup("Rotations");
    private final SettingGroup sgRender = settings.createGroup("Render");
    // Misc
    private final Setting<Type> antiFriendPop = sgMisc.add(new EnumSetting.Builder<Type>().name("anti-friend-pop").description("Avoids popping your friends.").defaultValue(Type.Both).build());
    private final Setting<Boolean> crystalSave = sgMisc.add(new BoolSetting.Builder().name("crystal-saver").description("Only targets players that can get hurt.").defaultValue(false).build());
    private final Setting<Logic> orderLogic = sgMisc.add(new EnumSetting.Builder<Logic>().name("Logic").description("What to do first.").defaultValue(Logic.BreakPlace).build());
    private final Setting<Boolean> antiWeakness = sgMisc.add(new BoolSetting.Builder().name("anti-weakness").description("Switches to tools to break crystals instead of your fist.").defaultValue(true).build());
    public final Setting<Boolean> oldMode = sgMisc.add(new BoolSetting.Builder().name("1.12-mode").description("Won't place in 1 high holes and enables walls options.").defaultValue(false).build());
    private final Setting<Type> rayTrace = sgMisc.add(new EnumSetting.Builder<Type>().name("ray-trace").description("Wont place / break through walls when on.").visible(oldMode::get).defaultValue(Type.None).build());
    // Place
    private final Setting<Mode> placeMode = sgPlace.add(new EnumSetting.Builder<Mode>().name("place-mode").description("The placement mode for crystals.").defaultValue(Mode.Safe).build());
    private final Setting<Integer> placeDelay = sgPlace.add(new IntSetting.Builder().name("place-delay").description("The amount of delay in ticks before placing.").defaultValue(1).min(0).sliderMax(10).build());
    private final Setting<Double> placeRange = sgPlace.add(new DoubleSetting.Builder().name("place-range").description("The radius in which crystals can be placed in.").defaultValue(5).min(0).sliderMax(6).build());
    private final Setting<Double> placeWallsRange = sgPlace.add(new DoubleSetting.Builder().name("walls-range").description("The radius in which crystals can be placed through walls.").visible(() -> rayTrace.get() != Type.Place && rayTrace.get() != Type.Both && oldMode.get()).defaultValue(5).min(0).sliderMax(6).build());
    private final Setting<Double> verticalRange = sgPlace.add(new DoubleSetting.Builder().name("vertical-range").description("Vertical place range.").defaultValue(5).min(0).sliderMax(6).build());
    private final Setting<Double> minPlaceDamage = sgPlace.add(new DoubleSetting.Builder().name("min-damage").description("The minimum damage the crystal will place.").defaultValue(6).build());
    private final Setting<Double> maxPlaceDamage = sgPlace.add(new DoubleSetting.Builder().name("max-damage").description("The maximum self damage the crystal will place.").visible(() -> placeMode.get() == Mode.Safe).defaultValue(2).build());
    private final Setting<Double> torque = sgPlace.add(new DoubleSetting.Builder().name("torque").description("Defines how lethal the placements are; With 0 being ultra careful and 1 completely ignoring self damage.").visible(() -> placeMode.get() == Mode.Suicide).defaultValue(1).min(0).sliderMax(1).max(1).build());
    private final Setting<Boolean> inBreakRange = sgPlace.add(new BoolSetting.Builder().name("within-break-range").description("Will only place when the spawned crystal is within break range.").defaultValue(false).build());
    // Break
    private final Setting<Mode> breakMode = sgBreak.add(new EnumSetting.Builder<Mode>().name("break-mode").description("The type of break mode for crystals.").defaultValue(Mode.Safe).build());
    private final Setting<Integer> breakDelay = sgBreak.add(new IntSetting.Builder().name("break-delay").description("The amount of delay in ticks before breaking.").defaultValue(1).min(0).sliderMax(10).build());
    private final Setting<Double> minBreakDamage = sgBreak.add(new DoubleSetting.Builder().name("min-damage").description("The minimum damage for a crystal to get broken.").defaultValue(4.5).min(0).sliderMax(36).build());
    private final Setting<Double> maxBreakDamage = sgBreak.add(new DoubleSetting.Builder().name("max-self-damage").description("The maximum self-damage allowed.").visible(() -> breakMode.get() == Mode.Safe).defaultValue(3).sliderMax(36).build());
    private final Setting<Double> breakRange = sgBreak.add(new DoubleSetting.Builder().name("break-range").description("The maximum range that crystals can be to be broken.").defaultValue(5).min(0).sliderMax(6).build());
    private final Setting<Double> breakWallsRange = sgBreak.add(new DoubleSetting.Builder().name("walls-range").description("The maximum range that crystals can be to be broken through walls.").visible(() -> rayTrace.get() != Type.Break && rayTrace.get() != Type.Both && oldMode.get()).defaultValue(5).min(0).sliderMax(6).build());
    private final Setting<BreakHand> breakHand = sgBreak.add(new EnumSetting.Builder<BreakHand>().name("hand").description("Which hand to swing for breaking.").defaultValue(BreakHand.Auto).build());
    private final Setting<Integer> breakAttempts = sgBreak.add(new IntSetting.Builder().name("break-attempts").description("How many times to hit a crystal before stopping to target it.").defaultValue(2).sliderMin(1).sliderMax(10).build());
    private final Setting<Canceller> removeCrystals = sgBreak.add(new EnumSetting.Builder<Canceller>().name("canceller").description("Hitcanceller is the fastest but might cause desync on strict anticheats.").defaultValue(Canceller.NoDesync).build());
    private final Setting<Integer> minAge = sgBreak.add(new IntSetting.Builder().name("minimum-crystal-age").description("How ticks a crystal has to exist in order to consider it.").defaultValue(0).sliderMax(4).build());
    // Target
    public final Setting<Object2BooleanMap<EntityType<?>>> entities = sgTarget.add(new EntityTypeListSetting.Builder().name("entities").description("The entities to attack.").defaultValue(Utils.asO2BMap(EntityType.PLAYER)).onlyAttackable().build());
    public final Setting<Double> targetRange = sgTarget.add(new DoubleSetting.Builder().name("target-range").description("The maximum range the entity can be to be targeted.").defaultValue(10).min(0).sliderMax(15).build());
    public final Setting<Boolean> predict = sgTarget.add(new BoolSetting.Builder().name("predict").description("Predicts target movement.").defaultValue(false).build());
    public final Setting<Boolean> ignoreTerrain = sgTarget.add(new BoolSetting.Builder().name("ignore-terrain").description("Ignores non blast resistant blocks in damage calcs (useful during terrain pvp).").defaultValue(true).build());
    private final Setting<Integer> numberOfDamages = sgTarget.add(new IntSetting.Builder().name("number-of-targets").description("Maximum number of targets to perform calculations on. Might lag your game when set too high.").defaultValue(3).sliderMin(1).sliderMax(5).build());
    //Faceplace
    private final Setting<Boolean> facePlace = sgFacePlace.add(new BoolSetting.Builder().name("face-place").description("Will face-place when target is below a certain health or armor durability threshold.").defaultValue(true).build());
    private final Setting<Double> facePlaceHealth = sgFacePlace.add(new DoubleSetting.Builder().name("health").description("The health the target has to be at to start faceplacing.").visible(facePlace::get).defaultValue(12).min(1).sliderMin(1).sliderMax(36).build());
    private final Setting<Double> facePlaceDurability = sgFacePlace.add(new DoubleSetting.Builder().name("durability").description("The durability threshold to be able to face-place.").visible(facePlace::get).defaultValue(10).min(1).sliderMin(1).sliderMax(100).max(100).build());
    private final Setting<Boolean> facePlaceSelf = sgFacePlace.add(new BoolSetting.Builder().name("face-place-self").description("Whether to faceplace when you are in the same hole as your target.").visible(facePlace::get).defaultValue(true).build());
    private final Setting<Boolean> facePlaceHole = sgFacePlace.add(new BoolSetting.Builder().name("hole-fags").description("Automatically starts faceplacing surrounded or burrowed targets.").visible(facePlace::get).defaultValue(false).build());
    private final Setting<Boolean> facePlaceArmor = sgFacePlace.add(new BoolSetting.Builder().name("missing-armor").description("Automatically starts faceplacing when a target misses a piece of armor.").visible(facePlace::get).defaultValue(true).build());
    private final Setting<Keybind> forceFacePlace = sgFacePlace.add(new KeybindSetting.Builder().name("force-face-place").description("Starts faceplacing when this button is pressed").visible(facePlace::get).defaultValue(Keybind.fromKey(-1)).build());
    private final Setting<Boolean> pauseSword = sgFacePlace.add(new BoolSetting.Builder().name("pause-when-swording").description("Doesnt faceplace when you are holding a sword.").visible(facePlace::get).defaultValue(true).build());
    //Support
    private final Setting<Boolean> support = sgSupport.add(new BoolSetting.Builder().name("support").description("Places a block in the air and crystals on it. Helps with killing players that are flying.").defaultValue(false).build());
    private final Setting<Integer> supportDelay = sgSupport.add(new IntSetting.Builder().name("support-delay").description("The delay between support blocks being placed.").visible(support::get).defaultValue(5).min(0).sliderMax(10).build());
    private final Setting<Boolean> supportBackup = sgSupport.add(new BoolSetting.Builder().name("support-backup").description("Makes it so support only works if there are no other options.").visible(support::get).defaultValue(true).build());
    private final Setting<Boolean> supportAirPlace = sgSupport.add(new BoolSetting.Builder().name("airplace").description("Whether to airplace the support block or not.").defaultValue(true).visible(support::get).build());
    //Surround
    private final Setting<Boolean> surroundHold = sgSurround.add(new BoolSetting.Builder().name("surround-hold").description("Places a crystal next to a player so they cannot use Surround.").defaultValue(true).build());
    private final Setting<Boolean> surroundBreak = sgSurround.add(new BoolSetting.Builder().name("surround-break").description("Places a crystal next to a surrounded player and keeps it there so they cannot use Surround again.").defaultValue(false).build());
    private final Setting<Boolean> surroundPickaxe = sgSurround.add(new BoolSetting.Builder().name("only-pickaxe").description("Will only attempt to surround break while you hold a pickaxe").visible(surroundBreak::get).defaultValue(false).build());
    private final Setting<Boolean> antiSurroundBreak = sgSurround.add(new BoolSetting.Builder().name("anti-surround-break").description("Breaks crystals that could surround break you.").defaultValue(false).build());
    // Pause
    private final Setting<Type> pauseMode = sgPause.add(new EnumSetting.Builder<Type>().name("pause-mode").description("What to pause.").defaultValue(Type.None).build());
    private final Setting<Boolean> pauseOnEat = sgPause.add(new BoolSetting.Builder().name("pause-on-eat").description("Pauses Crystal Aura while eating.").visible(() -> pauseMode.get() != Type.None).defaultValue(false).build());
    private final Setting<Boolean> pauseOnDrink = sgPause.add(new BoolSetting.Builder().name("pause-on-drink").description("Pauses Crystal Aura while drinking a potion.").visible(() -> pauseMode.get() != Type.None).defaultValue(false).build());
    private final Setting<Boolean> pauseOnMine = sgPause.add(new BoolSetting.Builder().name("pause-on-mine").description("Pauses Crystal Aura while mining blocks.").visible(() -> pauseMode.get() != Type.None).defaultValue(false).build());
    private final Setting<Boolean> facePlacePause = sgPause.add(new BoolSetting.Builder().name("pause-face-placing").description("When to interrupt face-placing.").visible(() -> pauseMode.get() != Type.None).defaultValue(false).build());
    private final Setting<Boolean> facePlacePauseEat = sgPause.add(new BoolSetting.Builder().name("fp-pause-on-eat").description("Pauses face placing while eating.").visible(facePlacePause::get).defaultValue(false).build());
    private final Setting<Boolean> facePlacePauseDrink = sgPause.add(new BoolSetting.Builder().name("fp-pause-on-drink").description("Pauses face placing while drinking.").visible(facePlacePause::get).defaultValue(false).build());
    private final Setting<Boolean> facePlacePauseMine = sgPause.add(new BoolSetting.Builder().name("fp-pause-on-mine").description("Pauses face placing while mining.").visible(facePlacePause::get).defaultValue(false).build());
    //Switch
    private final Setting<SwitchMode> switchMode = sgSwitch.add(new EnumSetting.Builder<SwitchMode>().name("switch-mode").description("How to switch items.").defaultValue(SwitchMode.Auto).build());
    private final Setting<Boolean> switchBack = sgSwitch.add(new BoolSetting.Builder().name("switch-back").description("Switches back to your previous slot when disabling Crystal Aura.").defaultValue(false).build());
    private final Setting<Integer> switchDelay = sgSwitch.add(new IntSetting.Builder().name("switch-delay").description("The amount of delay in ticks before switching slots again.").defaultValue(1).min(0).sliderMax(5).build());
    private final Setting<Boolean> noFoodSwitch = sgSwitch.add(new BoolSetting.Builder().name("no-food-switch").description("Won't switch when eating food. Useful when using with anti-weakness or mainhanding.").defaultValue(false).build());
    private final Setting<Integer> switchHealth = sgSwitch.add(new IntSetting.Builder().name("switch-health").description("The health to stop switching to crystals.").min(0).sliderMax(20).defaultValue(0).build());
    // Rotations
    private final Setting<RotationMode> rotationMode = sgRotations.add(new EnumSetting.Builder<RotationMode>().name("rotation-mode").description("The method of rotating when using Crystal Aura.").defaultValue(RotationMode.None).build());
    private final Setting<Boolean> strictLook = sgRotations.add(new BoolSetting.Builder().name("strict-look").description("Looks at exactly where you're placing.").visible(() -> rotationMode.get() != RotationMode.None).defaultValue(false).build());
    // Render
    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder().name("swing").description("Renders your swing client-side.").defaultValue(true).build());
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").description("Renders the block under where it is placing a crystal.").defaultValue(true).build());
    private final Setting<Boolean> thick = sgSwitch.add(new BoolSetting.Builder().name("thick").visible(render::get).defaultValue(true).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").visible(render::get).defaultValue(ShapeMode.Sides).build());
    private final Setting<Integer> renderTime = sgRender.add(new IntSetting.Builder().name("render-time").description("The amount of time between changing the block render.").visible(render::get).defaultValue(1).min(0).sliderMax(5).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The side color.").visible(render::get).defaultValue(new SettingColor(255, 0, 0, 75, true)).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The line color.").visible(render::get).defaultValue(new SettingColor(255, 0, 0, 200)).build());
    private final Setting<Boolean> renderDamage = sgRender.add(new BoolSetting.Builder().name("render-damage").description("Renders the damage of the crystal where it is placing.").defaultValue(true).build());
    private final Setting<Integer> roundDamage = sgRender.add(new IntSetting.Builder().name("round-damage").description("Round damage to x decimal places.").visible(renderDamage::get).defaultValue(2).min(0).max(3).sliderMax(3).build());
    private final Setting<Double> damageScale = sgRender.add(new DoubleSetting.Builder().name("damage-scale").description("The scale of the damage text.").visible(renderDamage::get).defaultValue(1.4).min(0).sliderMax(5).build());
    private final Setting<SettingColor> damageColor = sgRender.add(new ColorSetting.Builder().name("damage-color").description("The color of the damage text.").visible(renderDamage::get).defaultValue(new SettingColor(0, 0, 0, 255)).build());

    public VHAutoCrystal() {
        super(Atlas.Combat, "VH-auto-crystal", "Automatically places and breaks crystals to damage other players.");
    }

    private byte fails;
    private Vec3d playerPos;
    public static Vec3d eyeHeight;
    private BlockPos lastBlock;
    private Item mItem, oItem;
    public static float ticksBehind;
    private float lastDamage;
    private boolean switched, isUsing, weak;
    private int supportSlot, preSlot, placeDelayLeft, breakDelayLeft, switchDelayLeft, supportDelayLeft, lastEntityId, dif;
    private FindItemResult supportSlotResult;
    private LivingEntity target, closestTarget;
    private List<LivingEntity> targets = new ArrayList<>();
    private List<PlayerEntity> friends = new ArrayList<>();
    private List<EndCrystalEntity> crystals = new ArrayList<>(), attemptedCrystals = new ArrayList<>();
    private final IntSet entitiesToRemove = new IntOpenHashSet();
    private final Int2IntMap attemptedBreaks = new Int2IntOpenHashMap();

    @Override
    public void onActivate() {
        placeDelayLeft = 0;
        breakDelayLeft = 0;
        supportDelayLeft = 0;
        switchDelayLeft = 0;
        supportSlot = -1;
        preSlot = -1;
        supportSlotResult = null;
        weak = false;
        fails = 0;
        lastDamage = 0;
        lastBlock = null;
        target = null;
        friends.clear();
        crystals.clear();
        targets.clear();
        attemptedBreaks.clear();
    }

    @Override
    public void onDeactivate() {
        if (switchBack.get() && preSlot != -1 && switchDelayLeft <= 0) mc.player.getInventory().selectedSlot = preSlot;
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onTick(SendMovementPacketsEvent.Pre event) {
        placeDelayLeft--;
        breakDelayLeft--;
        supportDelayLeft--;
        switchDelayLeft--;
        if (mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()) != null)
            ticksBehind = (float) mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()).getLatency() / (50 * (20 / TickRate.INSTANCE.getTickRate()));         //50ms is the time it takes for 1 tick to pass under 20tps
        if (TickRate.INSTANCE.getTimeSinceLastTick() >= 1f)
            attemptedBreaks.clear();     //This is to prevent lag spikes from fucking your hit attempts

        getEntities();      //Gets targets, friends and crystals
        if (targets.isEmpty()) return;

        playerPos = mc.player.getPos();
        eyeHeight = new Vec3d(0, mc.player.getEyeHeight(mc.player.getPose()), 0);
        mItem = mc.player.getMainHandStack().getItem();
        oItem = mc.player.getOffHandStack().getItem();
        isUsing = mc.player.isUsingItem();

        Map<StatusEffect, StatusEffectInstance> effects = mc.player.getActiveStatusEffects();   //Anti weakness stuff
        weak = false;
        if (!effects.isEmpty()) {
            boolean strong = false;
            if (effects.containsKey(StatusEffects.STRENGTH) && effects.get(StatusEffects.STRENGTH).getAmplifier() == 1)
                strong = true;   //You can destroy crystals with your bare hands if you have strength 2 with weakness 1
            if (effects.containsKey(StatusEffects.WEAKNESS))
                weak = effects.get(StatusEffects.WEAKNESS).getAmplifier() == 1 || !strong;
        }
        if (fails > renderTime.get()) {
            lastBlock = null;
        }

        switch (orderLogic.get()) {
            case BreakPlace: {
                doBreak();
                doPlace();
                break;
            }
            case PlaceBreak: {
                doPlace();
                doBreak();
                break;
            }
        }
    }

//    private void getEntities() {
//        targets.clear();
//        friends.clear();
//        crystals.clear();
//        closestTarget = null;
//        for (Entity entity : mc.world.getEntities()) {
//            if (entity.isAlive())
//                if (entity.isInRange(mc.player, targetRange.get()))
//                    if (entity != mc.player)
//                        if (!(entity instanceof PlayerEntity) || Friends.get().shouldAttack((PlayerEntity) entity)) {
//                            if (entities.get().getBoolean(entity.getType())) {
//                                if (targets.size() < numberOfDamages.get()) {
//                                    targets.add((LivingEntity) entity);
//                                    if (closestTarget == null || mc.player.distanceTo(entity) < mc.player.distanceTo(closestTarget))
//                                        closestTarget = (LivingEntity) entity;
//                                }
//                            } else if (entity instanceof EndCrystalEntity)
//                                crystals.add((EndCrystalEntity) entity);
//                        } else
//                            friends.add((PlayerEntity) entity);
//        }
//    }

    private void getEntities() {
        targets.clear();
        friends.clear();
        crystals.clear();

        // Players
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player.getAbilities().creativeMode || player == mc.player) continue;

            if (!player.isDead() && player.isAlive() && Friends.get().shouldAttack(player) && player.distanceTo(mc.player) <= targetRange.get()) {
                if (targets.size() < numberOfDamages.get()) {
                    targets.add(player);
                    if (closestTarget == null || mc.player.distanceTo(player) < mc.player.distanceTo(closestTarget))
                        closestTarget = player;
                }
            }
        }

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity && mc.player.distanceTo(entity) <= breakRange.get()) {
                crystals.add((EndCrystalEntity) entity);
            }
        }

        // Fake players
        for (PlayerEntity player : FakePlayerManager.getPlayers()) {
            if (!player.isDead() && player.isAlive() && Friends.get().shouldAttack(player) && player.distanceTo(mc.player) <= targetRange.get()) {
                targets.add(player);
            }
        }
    }

    private void doPlace() {//TODO rework the order in which damages are calculated
        if (placeDelayLeft > 0) return;

        //Check if crystals are available
        if (oItem != Items.END_CRYSTAL && mItem != Items.END_CRYSTAL) {
            if (switchMode.get() == SwitchMode.None) {
                fails++;
                return;
            } else {
                if (noFoodSwitch.get() && isUsing && (mItem.isFood() || oItem.isFood())) {
                    fails++;
                    return;
                }   //No food switch
                int slot = InvUtils.findInHotbar(Items.END_CRYSTAL).getSlot();
                if (slot < 0 || slot > 8) {
                    fails++;
                    return;
                }  //Return if no crystals available
            }
        }

        //Pauses
        if (pauseMode.get() == Type.Place || pauseMode.get() == Type.Both) {
            if (isUsing) {
                if ((pauseOnDrink.get() && (mItem instanceof PotionItem || oItem instanceof PotionItem)) || pauseOnEat.get() && (mItem.isFood() || oItem.isFood())) {
                    fails++;
                    return;
                }
            }
            if (pauseOnMine.get() && mc.interactionManager.isBreakingBlock()) {
                fails++;
                return;
            }
        }

        boolean canSpprt = false;
        if (support.get()) {    //Check if there is obby in hotbar for support block
            for (int i = 0; i < 9; i++) {
                if (mc.player.getInventory().getStack(i).getItem() == Items.OBSIDIAN) {
                    canSpprt = true;
                    supportSlot = i;
                    break;
                }
            }
            supportSlotResult = InvUtils.findInHotbar(Items.OBSIDIAN);
        }
        final boolean canSupport = canSpprt;
        AtomicReference<Vec3d> bestSupportPos = new AtomicReference<>();
        AtomicReference<Vec3d> bestPos = new AtomicReference<>();
        AtomicReference<LivingEntity> surroundTarget = new AtomicReference<>();
        AtomicDouble bestValue = new AtomicDouble();
        AtomicDouble displayDamage = new AtomicDouble();
        AtomicDouble bestSupportValue = new AtomicDouble();
        AtomicDouble displayBackupDamage = new AtomicDouble();
        AtomicInteger surroundBroken = new AtomicInteger();

        BlockIterator.register((int) Math.ceil(placeRange.get()), (int) Math.ceil(verticalRange.get()), (blockPos, blockState) -> {
            Vec3d posNew = new Vec3d(blockPos.getX(), blockPos.getY(), blockPos.getZ());
            Vec3d checkVector = new Vec3d(      //Vector to accurately check ranges
                posNew.x + (blockPos.getX() < playerPos.x ? Math.min(1, playerPos.x - blockPos.getX()) : 0),
                posNew.y + (blockPos.getY() < playerPos.y ? Math.min(1, playerPos.y - blockPos.getY()) : 0),
                posNew.z + (blockPos.getZ() < playerPos.z ? Math.min(1, playerPos.z - blockPos.getZ()) : 0));

            //Making sure its a sphere not a cube
            if (!checkVector.isInRange(playerPos.add(eyeHeight), placeRange.get())) return;

            if (inBreakRange.get() && !inBreakRange(new EndCrystalEntity(mc.world, posNew.x + 0.5, posNew.y + 1, posNew.z + 0.5)))
                return;

            if (antiSurroundBreak.get() && Block2Utils.getSurroundBreak(mc.player, blockPos) > 0 && Block2Utils.isSurrounded(mc.player))
                return;

            float friendDamage = 0;
            float selfDamage;
            boolean facePlaceLimit = false;
            for (LivingEntity target : targets) {
                if (target.isDead()) continue;
                Vec3d v = predict.get() ? target.getVelocity() : new Vec3d(0, 0, 0);
                Vec3d targetPos = target.getPos().add(v.x * ticksBehind, v.y * ticksBehind, v.z * ticksBehind);

                boolean facePlace = shouldFacePlace(target) && !facePlaceLimit;
                if (facePlace) facePlaceLimit = true;

                if (crystalSave.get() && target.hurtTime - (Math.max(breakDelayLeft, 0)) > 0 && !Block2Utils.isFucked(target))
                    continue;//removing ticksbehind for now since it can be buggy

                if (blockPos.getY() > target.getBlockPos().getY())
                    continue;    //Blocks that are above the targets feet pos wont do any significant damage so we can filter them out

                if (checkVector.distanceTo(targetPos) > 9)
                    continue;    //9 blocks is the max distance where you can one shot a naked player so if the target is further than that there is no point in running calcs since it wont do any damage

                //check if we can place here
                if ((mc.world.getBlockState(blockPos).getBlock() != Blocks.BEDROCK && mc.world.getBlockState(blockPos).getBlock() != Blocks.OBSIDIAN && (!canSupport || !mc.world.getBlockState(blockPos).getMaterial().isReplaceable() || supportDelayLeft > 0)) || !isEmpty(blockPos.add(0, 1, 0)))
                    return;

                if (mc.world.getBlockState(blockPos).isAir()) {
                    if (supportBackup.get() && bestPos.get() != null)
                        return;        //Prevents unnecessary calcs on empty blocks when support backup is on and a valid non air block has been found already
                    if (mc.player.getInventory().selectedSlot != supportSlot && switchDelayLeft > 0)
                        return;      //cant place obby if this condition is met so we wont try to place on air blocks then
                    if (!supportAirPlace.get()) {   // Check if there is a valid neighbour to place against
                        boolean neighbourFound = false;
                        for (Direction side : Direction.values()) {
                            BlockPos neighbor = blockPos.offset(side);
                            if (mc.world.getBlockState(neighbor).isAir() || BlockUtils.isClickable(mc.world.getBlockState(neighbor).getBlock()))
                                continue;
                            neighbourFound = true;
                            break;
                        }
                        if (!neighbourFound) return;
                    }
                }
                //Check if we can place through walls here
                if (Block2Utils.rayTraceCheck(blockPos, false) == null && oldMode.get()) {
                    if (rayTrace.get() == Type.Place || rayTrace.get() == Type.Both) return;
                    if (!checkVector.isInRange(playerPos.add(eyeHeight), placeWallsRange.get())) return;
                }
                if (!isSafePlace(mc.player, posNew.add(0.5, 1, 0.5))) return;

                if (antiFriendPop.get() == Type.Place || antiFriendPop.get() == Type.Both) {    //Anti friend pop
                    for (PlayerEntity friend : friends) {
                        if (!checkVector.isInRange(friend.getPos(), 9)) continue;

                        if (!isSafePlace(friend, posNew.add(0.5, 1, 0.5))) return;

                        friendDamage = +Block2Utils.crystalDamage(friend, posNew.add(0.5, 1, 0.5), predict.get(), ignoreTerrain.get());
                    }
                }
                if (surroundBreak.get()) {   //Surround break
                    if ((bestPos.get() == null || surroundBroken.get() > 0) && target == TargetUtils.getPlayerTarget(placeRange.get() + 1, SortPriority.LowestHealth) && !Block2Utils.isSurroundBroken(target) && fails > 0) {
                        if (mItem instanceof PickaxeItem || !surroundPickaxe.get()) {
                            int breakValue = Block2Utils.getSurroundBreak(target, blockPos);
                            if (breakValue > surroundBroken.get()) {
                                surroundBroken.set(breakValue);
                                bestPos.set(posNew);
                                surroundTarget.set(target);
                            }
                        }
                    }
                }
                //Check for min dmg
                float minDmg = facePlace ? 2.5F : (float) minPlaceDamage.get().doubleValue();
                float damage = Block2Utils.crystalDamage(target, posNew.add(0.5, 1, 0.5), predict.get(), ignoreTerrain.get());
                if (damage < minDmg) continue;

                //Preventing multiplace
                boolean stop = false;
                for (Entity entity : mc.world.getEntities()) {
                    if (!(entity instanceof EndCrystalEntity)) continue;
                    EndCrystalEntity crystal = (EndCrystalEntity) entity;
                    if (crystal.distanceTo(target) > 9) continue;
                    if (!shouldBreak(crystal)) continue;
                    if (!inBreakRange(crystal)) continue;
                    float multiDamage = Block2Utils.crystalDamage(target, crystal.getPos(), predict.get(), ignoreTerrain.get());
                    if (multiDamage < minDmg) continue;
                    if (!isSafeBreak(mc.player, crystal.getPos()))
                        continue; //removing ticksbehind for now since it can be buggy
                    stop = true;
                    break;
                }
                if (stop) continue;

                selfDamage = Block2Utils.crystalDamage(mc.player, posNew.add(0.5, 1, 0.5), false, ignoreTerrain.get()) + friendDamage;

                float newValue = damage / (float) Math.pow(selfDamage, (1 - torque.get()));
                if (!mc.world.isAir(blockPos) || !supportBackup.get()) {
                    if (newValue > bestValue.get()) {
                        bestPos.set(posNew);
                        bestValue.set(newValue);
                        displayDamage.set(damage);
                        surroundBroken.set(0);
                    }
                } else if (bestPos.get() == null) {
                    if (newValue > bestSupportValue.get()) {
                        bestSupportPos.set(posNew);
                        bestSupportValue.set(newValue);
                        displayBackupDamage.set(damage);
                        surroundBroken.set(0);
                    }
                }
            }
        });
        BlockIterator.after(() -> {
            if (bestPos.get() != null || bestSupportPos.get() != null) {
                Vec3d pos = bestPos.get() != null ? bestPos.get() : bestSupportPos.get();
                BlockPos blockPos = new BlockPos((int) pos.x, (int) pos.y, (int) pos.z);
                fails = 0;
                lastBlock = blockPos;
                if (mc.world.isAir(blockPos)) {     //Placing the support block
                    if (mc.player.getInventory().selectedSlot != supportSlot && switchDelayLeft > 0) return;
                    BlockUtils.place(new BlockPos(pos), supportSlotResult, false, 0, swing.get(), true, false);
                    supportDelayLeft = supportDelay.get();
                }
                //Placing the crystal
                lastDamage = (float) displayDamage.get();
                placeDelayLeft = placeDelay.get();
                EndCrystalEntity newCrystal = new EndCrystalEntity(mc.world, pos.x + 0.5, pos.y + 1, pos.z + 0.5);
                if (switchMode.get() == SwitchMode.Auto) doSwitch();
                attemptedCrystals.add(newCrystal);
                BlockHitResult result = Block2Utils.getPlaceResult(blockPos);
                if (rotationMode.get() == RotationMode.Place || rotationMode.get() == RotationMode.Both) {
                    float[] rotation = PlayerUtils.calculateAngle(strictLook.get() ? new Vec3d(
                        result.getBlockPos().getX() + 0.5 + result.getSide().getVector().getX() * 0.5,
                        result.getBlockPos().getY() + 0.5 + result.getSide().getVector().getY() * 0.5,
                        result.getBlockPos().getZ() + 0.5 + result.getSide().getVector().getZ() * 0.5) : pos.add(0.5, 1.0, 0.5));
                    Rotations.rotate(rotation[0], rotation[1], 25, () -> mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(getHand(), result)));

                } else mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(getHand(), result));

                entitiesToRemove.clear();
                if (swing.get()) mc.player.swingHand(getHand());
                else mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(getHand()));
            } else fails++;
        });
    }

    private void doBreak() {
        if (breakDelayLeft > 0) return;
        if (pauseBreak()) return;    //Pauses

        float bestDamage = 0;
        EndCrystalEntity bestCrystal = null;
        for (EndCrystalEntity crystal : crystals) {

            if (!shouldBreak(crystal)) continue;
            if (crystal.age < minAge.get()) continue;

            if (!inBreakRange(crystal)) continue;

            Vec3d crystalPos = crystal.getPos();
            for (LivingEntity target : targets) {

                if (target.isDead()) continue;

                if (target instanceof PlayerEntity && ((PlayerEntity) target).getAbilities().invulnerable) continue;

                if (crystal.distanceTo(target) > 9) continue;

                if (crystalSave.get() && target.hurtTime > 0)
                    continue; //removing ticksbehind for now since it can be buggy

                float minDmg = shouldFacePlace(target) ? 2.5F : (float) minBreakDamage.get().doubleValue();
                float damage = Block2Utils.crystalDamage(target, crystalPos, predict.get(), ignoreTerrain.get());

                if (surroundHold.get() && target.hurtTime > 0 && Block2Utils.isFucked(target) && damage >= 10)
                    continue;//removing ticksbehind for now since it can be buggy

                if (!isSafeBreak(mc.player, crystalPos)) break;//removing ticksbehind for now since it can be buggy

                if (antiFriendPop.get() == Type.Both || antiFriendPop.get() == Type.Break) {
                    boolean skip = false;
                    for (PlayerEntity friend : friends) {
                        if (!Block2Utils.crystalEdgePos(crystal).isInRange(friend.getPos(), 9)) continue;

                        if (!isSafeBreak(friend, crystalPos)) {
                            skip = true;
                            break;
                        }
                    }
                    if (skip) break;
                }
                if (antiSurroundBreak.get() && bestCrystal == null) {
                    BlockPos playerBlockPos = new BlockPos(playerPos);
                    boolean stop = false;
                    for (Vec3i block : Block2Utils.city) {
                        double x = playerBlockPos.add(block).getX();
                        double y = playerBlockPos.add(block).getY();
                        double z = playerBlockPos.add(block).getZ();
                        if (mc.world.getBlockState(playerBlockPos.add(block)).isOf(Blocks.BEDROCK)) continue;
                        for (Entity entity : mc.world.getOtherEntities(null, new Box(x, y, z, x + 1, y + 1, z + 1))) {
                            if (entity.equals(crystal)) {
                                bestCrystal = crystal;
                                stop = true;
                                break;
                            }
                        }
                        if (stop) break;
                    }
                    if (stop) continue;
                }

                if (damage < minDmg) continue;
                if (damage > bestDamage) {
                    bestDamage = damage;
                    bestCrystal = crystal;
                    this.target = target;
                }
            }
        }
        if (bestCrystal == null) return;

        final EndCrystalEntity crystal = bestCrystal;
        preSlot = mc.player.getInventory().selectedSlot;
        if (weak && antiWeakness.get() && switchDelayLeft <= 0) {
            for (int i = 0; i < 9; i++) {
                if (mc.player.getInventory().getStack(i).getItem() instanceof ToolItem) {
                    mc.player.getInventory().selectedSlot = i;
                    switched = true;
                    break;
                }
            }
        }
        doAttack(crystal);
    }

    private void doAttack(EndCrystalEntity crystal) {
        entitiesToRemove.add(crystal.getId());
        if (rotationMode.get() == RotationMode.Break || rotationMode.get() == RotationMode.Both) {
            float[] rotation = PlayerUtils.calculateAngle(Block2Utils.crystalEdgePos(crystal));
            Rotations.rotate(rotation[0], rotation[1], 50, () -> attackCrystal(crystal));
        } else attackCrystal(crystal);
    }

    private void attackCrystal(EndCrystalEntity crystal) {
        if (antiWeakness.get() && weak) mc.interactionManager.attackEntity(mc.player, crystal);
        else mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, false));
        Hand breakerHand = breakHand.get() == BreakHand.Auto ? getHand() : breakHand.get() == BreakHand.Mainhand ? Hand.MAIN_HAND : Hand.OFF_HAND;
        if (swing.get()) mc.player.swingHand(breakerHand);
        else mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(breakerHand));

        if (removeCrystals.get() == Canceller.HitCanceller) {
            crystals.remove(crystal);
            crystal.kill();
        }
        attemptedBreaks.put(crystal.getId(), attemptedBreaks.get(crystal.getId()) + 1);

        if (switchDelayLeft <= 0 && switched && switchBack.get()) {
            mc.player.getInventory().selectedSlot = preSlot;
            switched = false;
        }
        breakDelayLeft = breakDelay.get();
    }


    private boolean shouldBreak(EndCrystalEntity crystal) {
        if (!crystal.isAlive()) return false;            //Self explanatory

        for (int id : entitiesToRemove)
            if (crystal == mc.world.getEntityById(id))
                return false;            //Continue if crystal is already about to get removed

        return attemptedBreaks.get(crystal.getId()) < breakAttempts.get();            //Check break attempts
    }

    private boolean inBreakRange(EndCrystalEntity crystal) {
        Vec3d crystalPos = Block2Utils.crystalEdgePos(crystal);
        if (!crystalPos.isInRange(playerPos.add(eyeHeight), breakRange.get()))
            return false;    //Making sure crystal is in range and its a sphere not a cube
        if (!mc.player.canSee(crystal) && oldMode.get()) {   //Raytrace & walls range check
            if (rayTrace.get() == Type.Break || rayTrace.get() == Type.Both) return false;
            if (!crystalPos.isInRange(playerPos.add(eyeHeight), breakWallsRange.get())) return false;
        }
        return true;
    }

    private boolean isSafeBreak(LivingEntity entity, Vec3d crystalPos) {
        if (breakMode.get() == Mode.Suicide) return true;
        float damage = Block2Utils.crystalDamage(entity, crystalPos, !entity.equals(mc.player) && predict.get(), ignoreTerrain.get());
        return (EntityUtils.getTotalHealth((PlayerEntity) entity)) > damage && damage <= maxBreakDamage.get();
    }

    private boolean isSafePlace(LivingEntity entity, Vec3d crystalPos) {
        if (placeMode.get() == Mode.Suicide) return true;
        float damage = Block2Utils.crystalDamage(entity, crystalPos, !entity.equals(mc.player) && predict.get(), ignoreTerrain.get());
        return (EntityUtils.getTotalHealth((PlayerEntity) entity)) > damage && damage <= maxPlaceDamage.get();
    }

    private boolean pauseBreak() {
        if (pauseMode.get() == Type.Break || pauseMode.get() == Type.Both) {
            if (isUsing) {
                if (pauseOnEat.get() && (mItem.isFood() || oItem.isFood())) return true;
                if (pauseOnDrink.get() && (mItem instanceof PotionItem || oItem instanceof PotionItem)) return true;
            }
            if (pauseOnMine.get() && mc.interactionManager.isBreakingBlock()) return true;
        }
        if (weak) {
            if (noFoodSwitch.get() && isUsing && (mItem.isFood() || (oItem.isFood() && !(mItem instanceof ToolItem))))
                return true;
            boolean strong = false;
            for (int i = 0; i < 9; i++) {
                Item item = mc.player.getInventory().getStack(i).getItem();
                if (item instanceof ToolItem) {
                    strong = true;
                    break;
                }
            }
            if (!strong) return true;
        }
        return false;
    }

    private void doSwitch() {
        if (switchDelayLeft > 0) return;
        if (mItem == Items.END_CRYSTAL || oItem == Items.END_CRYSTAL) return;
        if (mc.player.getHealth() <= switchHealth.get()) return;
        int slot = InvUtils.find(Items.END_CRYSTAL).getSlot();
        if (slot != -1 && slot < 9) {
            preSlot = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = slot;
            switched = true;
        }
    }

    private boolean isEmpty(BlockPos pos) {
        double x = pos.up().getX();
        double y = pos.up().getY();
        double z = pos.up().getZ();
        List<Entity> entities = mc.world.getOtherEntities(null, new Box(x, y - 1, z, x + 1.0D, y + 1.0D, z + 1.0D));
        for (int id : entitiesToRemove) {
            entities.remove(mc.world.getEntityById(id));
        }
        return (mc.world.getBlockState(pos).isAir() && entities.isEmpty() && (!oldMode.get() || mc.world.getBlockState(pos.add(0, 1, 0)).isAir()));
    }

    private boolean shouldFacePlace(LivingEntity target) {
        if (!facePlace.get()) return false;
        if (forceFacePlace.get().isPressed() && target.equals(closestTarget)) return true;
        if (!(target instanceof PlayerEntity)) return false;
        if (!facePlaceSelf.get() && mc.player.distanceTo(target) < 1) return false;
        if (pauseSword.get() && (mItem instanceof ToolItem)) return false;
        if (facePlacePause.get()) {  //faceplace pause;
            if (isUsing) {
                if (facePlacePauseEat.get() && (mItem.isFood() || oItem.isFood())) return false;
                if (facePlacePauseDrink.get() && (mItem instanceof PotionItem || oItem instanceof PotionItem))
                    return false;
            }
            if (facePlacePauseMine.get() && mc.interactionManager.isBreakingBlock()) return false;
        }
        if (facePlaceHole.get() && (Block2Utils.isSurrounded(target) || Block2Utils.isBurrowed(target))) return true;
        if (EntityUtils.getTotalHealth((PlayerEntity) target) <= facePlaceHealth.get()) return true;

        Iterable<ItemStack> armourItems = target.getArmorItems();
        for (ItemStack itemStack : armourItems) {
            if (itemStack == null || itemStack.isEmpty()) {
                if (facePlaceArmor.get()) return true;
            } else if ((((double) (itemStack.getMaxDamage() - itemStack.getDamage()) / itemStack.getMaxDamage()) * 100) <= facePlaceDurability.get())
                return true;
        }
        return false;
    }

    private Hand getHand() {
        Hand hand = Hand.MAIN_HAND;
        if (mItem != Items.END_CRYSTAL && oItem == Items.END_CRYSTAL) {
            hand = Hand.OFF_HAND;
        }
        return hand;
    }

    @EventHandler
    private void onEntityAdded(EntityAddedEvent event) {
        dif = event.entity.getId() - lastEntityId;
        lastEntityId = event.entity.getId();
        if (!(event.entity instanceof EndCrystalEntity)) return;
        EndCrystalEntity crystal = (EndCrystalEntity) event.entity;
        if (pauseBreak() || breakDelayLeft > 0) return;
        if (!shouldBreak(crystal)) return;
        if (crystal.age < minAge.get()) return;
        if (!inBreakRange(crystal)) return;

        for (LivingEntity target : targets) {
            if (target.isDead()) continue;
            if (target.hurtTime > 0 && crystalSave.get()) continue; //removing ticksbehind for now since it can be buggy
            if (crystal.distanceTo(target) > 9) continue;
            float damage = Block2Utils.crystalDamage(target, crystal.getPos(), predict.get(), ignoreTerrain.get());
            if (target.hurtTime > 0 && Block2Utils.isFucked(target) && surroundHold.get() && damage >= 10) continue;
            if (damage < (shouldFacePlace(target) ? 2.5 : minBreakDamage.get())) continue;

            if (!isSafeBreak(mc.player, crystal.getPos())) return; //removing ticksbehind for now since it can be buggy

            if (antiFriendPop.get() == Type.Both || antiFriendPop.get() == Type.Break) {
                for (PlayerEntity friend : friends) {
                    if (!Block2Utils.crystalEdgePos(crystal).isInRange(friend.getPos(), 9)) continue;
                    if (!isSafeBreak(friend, crystal.getPos())) return;
                }
            }
            doAttack(crystal);
            break;
        }
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof UpdateSelectedSlotC2SPacket) {
            switchDelayLeft = switchDelay.get();
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (lastBlock == null || !render.get() || targets.isEmpty()) return;
        if (thick.get()) {
            Render2Utils.thick_box(event, lastBlock, sideColor.get(), lineColor.get(), shapeMode.get());
        } else {
            event.renderer.box(lastBlock, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (lastBlock == null || !renderDamage.get() || targets.isEmpty() || lastDamage == 0) return;
        Vec3 pos = new Vec3(lastBlock.getX() + 0.5, lastBlock.getY() + 0.5, lastBlock.getZ() + 0.5);
        if (NametagUtils.to2D(pos, damageScale.get())) {
            NametagUtils.begin(pos);
            TextRenderer.get().begin(1, false, true);
            String damageText = String.valueOf(Math.round(lastDamage));
            switch (roundDamage.get()) {
                case 0:
                    damageText = String.valueOf(Math.round(lastDamage));
                    break;
                case 1:
                    damageText = String.valueOf(Math.round(lastDamage * 10.0) / 10.0);
                    break;
                case 2:
                    damageText = String.valueOf(Math.round(lastDamage * 100.0) / 100.0);
                    break;
                case 3:
                    damageText = String.valueOf(Math.round(lastDamage * 1000.0) / 1000.0);
                    break;
            }
            double w = TextRenderer.get().getWidth(damageText) / 2;
            TextRenderer.get().render(damageText, -w, 0, damageColor.get());
            TextRenderer.get().end();
            NametagUtils.end();
        }
    }

    @Override
    public String getInfoString() {
        if (target != null && target.distanceTo(mc.player) <= targetRange.get() && !target.isDead())
            return target.getEntityName();
        else return null;
    }
}
