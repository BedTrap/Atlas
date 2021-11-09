package me.bedtrapteam.addon.modules.atlas.combat;

import me.bedtrapteam.addon.Atlas;
import me.bedtrapteam.addon.utils.Checker;
import me.bedtrapteam.addon.utils.InitializeUtils;
import me.bedtrapteam.addon.utils.enchansed.Render2Utils;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.misc.Vec3;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.tag.FluidTags;
import net.minecraft.tag.Tag;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class AutoCityRewrite extends Module {
    private final SettingGroup sgGeneral;
    private final SettingGroup sgRender;
    public final Setting<Double> targetRange;
    public final Setting<Double> range;
    private final Setting<Boolean> support;
    public final Setting<Boolean> old;
    private final Setting<Boolean> rotate;
    private final Setting<Boolean> delayed;
    private final Setting<Boolean> selfToggle;
    private final Setting<Boolean> swing;
    private final Setting<Boolean> render;
    private final Setting<Boolean> thick;
    private final Setting<ShapeMode> shapeMode;
    private final Setting<SettingColor> sideColor;
    private final Setting<SettingColor> lineColor;
    private final Setting<Boolean> renderProgress;
    private final Setting<Double> progressScale;
    private final Setting<SettingColor> progressColor;
    private PlayerEntity target;
    private BlockPos blockPosTarget;
    private int timer;
    private int max;
    private boolean firstTime;

    public int i = 0;

    public AutoCityRewrite() {
        super(Atlas.Combat, "auto-city-rewrite", "Automatically cities a target by mining the nearest obsidian next to them.");
        sgGeneral = settings.getDefaultGroup();
        sgRender = settings.createGroup("Render");
        targetRange = sgGeneral.add((Setting<Double>) new DoubleSetting.Builder().name("target-range").description("The radius in which players get targeted.").defaultValue(4.0).min(0.0).sliderMax(5.0).build());
        range = sgGeneral.add((Setting<Double>) new DoubleSetting.Builder().name("range").description("The radius in which blocks are allowed to get broken").defaultValue(5.0).min(0.0).sliderMax(6.0).build());
        support = sgGeneral.add( new BoolSetting.Builder().name("support").description("If there is no block below a city block it will place one before mining.").defaultValue(true).build());
        old = sgGeneral.add( new BoolSetting.Builder().name("1.12-mode").description("Requires an air block above in order to target it.").defaultValue(false).build());
        rotate = sgGeneral.add( new BoolSetting.Builder().name("rotate").description("Automatically rotates you towards the city block.").defaultValue(true).build());
        delayed = sgGeneral.add( new BoolSetting.Builder().name("delayed-switch").description("Will only switch to the pickaxe when the block is ready to be broken.").defaultValue(true).build());
        selfToggle = sgGeneral.add( new BoolSetting.Builder().name("self-toggle").description("Automatically toggles off after activation.").defaultValue(true).build());
        swing = sgRender.add( new BoolSetting.Builder().name("swing").description("Renders your swing client-side.").defaultValue(true).build());
        render = sgRender.add( new BoolSetting.Builder().name("render").description("Renders the block you are mining.").defaultValue(true).build());
        thick = sgRender.add(new BoolSetting.Builder().name("thick").defaultValue(true).build());
        shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").visible(render::get).defaultValue(ShapeMode.Sides).build());
        sideColor = sgRender.add((Setting<SettingColor>) new ColorSetting.Builder().name("side-color").description("The side color.").visible(render::get).defaultValue(new SettingColor(255, 0, 0, 75, true)).build());
        lineColor = sgRender.add((Setting<SettingColor>) new ColorSetting.Builder().name("line-color").description("The line color.").visible(render::get).defaultValue(new SettingColor(255, 0, 0, 200)).build());
        renderProgress = sgRender.add( new BoolSetting.Builder().name("render-progress").description("Renders the block break progress").defaultValue(true).build());
        progressScale = sgRender.add((Setting<Double>) new DoubleSetting.Builder().name("progress-scale").description("The scale of the progress text.").visible(renderProgress::get).defaultValue(1.4).min(0.0).sliderMax(5.0).build());
        progressColor = sgRender.add((Setting<SettingColor>) new ColorSetting.Builder().name("progress-color").description("The color of the progress text.").visible(renderProgress::get).defaultValue(new SettingColor(0, 0, 0, 255)).build());
    }

    @Override
    public void onActivate() {
        Checker.Check();

        timer = 0;
        max = 0;
        target = null;
        blockPosTarget = null;
        firstTime = true;

        i = 0;
    }

    @Override
    public void onDeactivate() {
        if (blockPosTarget == null) {
            return;
        }
        mc.player.networkHandler.sendPacket((Packet) new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, blockPosTarget, Direction.UP));

        Checker.Check();
    }

    @EventHandler
    private void onTick(final TickEvent.Pre event) {
        if (i == 0) {
            InitializeUtils.Check();
            i++;
        }
        timer--;
        if (TargetUtils.isBadTarget(target, targetRange.get())) {
            target = TargetUtils.getPlayerTarget(targetRange.get(), SortPriority.LowestDistance);
        }
        if (TargetUtils.isBadTarget(target, targetRange.get())) {
            target = null;
            blockPosTarget = null;
            timer = 0;
            firstTime = true;
            if (selfToggle.get()) {
                ChatUtils.error(name, "No target found... disabling.");
                toggle();
            }
            return;
        }
        blockPosTarget = ((blockPosTarget == null) ? EntityUtils.getCityBlock(target) : blockPosTarget);
        FindItemResult slot = InvUtils.findInHotbar(itemStack -> itemStack.getItem() == Items.NETHERITE_PICKAXE || itemStack.getItem() == Items.DIAMOND_PICKAXE);
        if (blockPosTarget == null || PlayerUtils.distanceTo(blockPosTarget) > mc.interactionManager.getReachDistance() || !slot.found()) {
            if (selfToggle.get()) {
                if (blockPosTarget == null) {
                    ChatUtils.error(name, "No target block found... disabling.", new Object[0]);
                } else if (PlayerUtils.distanceTo(blockPosTarget) > mc.interactionManager.getReachDistance()) {
                    ChatUtils.error(name, "Target block out of reach... disabling.", new Object[0]);
                } else if (!slot.found()) {
                    ChatUtils.error(name, "No pickaxe found... disabling.", new Object[0]);
                }
                toggle();
            }
            target = null;
            firstTime = true;
            return;
        }
        if (firstTime) {
            ChatUtils.info(name, "Attempting to city " + target.getGameProfile().getName(), new Object[0]);
            if (support.get()) {
                BlockUtils.place(blockPosTarget.down(1), InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, true);
            }
            if (!delayed.get()) {
                mc.player.getInventory().selectedSlot = slot.getSlot();
            }
            if (rotate.get()) {
                Rotations.rotate(Rotations.getYaw(blockPosTarget), Rotations.getPitch(blockPosTarget), () -> mine(blockPosTarget));
            } else {
                mine(blockPosTarget);
            }
        }
        max = getBlockBreakingSpeed(mc.world.getBlockState(blockPosTarget), blockPosTarget, slot.getSlot());
        timer = (firstTime ? max : timer);
        if (firstTime) {
            firstTime = false;
        }
        if (timer <= 0) {
            blockPosTarget = null;
            mc.player.getInventory().selectedSlot = slot.getSlot();
            firstTime = true;
            if (selfToggle.get()) {
                toggle();
            }
        }
    }

    private void mine(final BlockPos blockPos) {
        mc.getNetworkHandler().sendPacket((Packet) new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.UP));
        if (swing.get()) {
            mc.player.swingHand(Hand.MAIN_HAND);
        } else {
            mc.player.networkHandler.sendPacket((Packet) new HandSwingC2SPacket(Hand.MAIN_HAND));
        }
        mc.getNetworkHandler().sendPacket((Packet) new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP));
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (blockPosTarget == null || !render.get() || target == null || mc.player.getAbilities().creativeMode) {
            return;
        }

        if (thick.get()) {
            Render2Utils.thick_box(event, blockPosTarget, sideColor.get(), lineColor.get(), shapeMode.get());
        } else {
            event.renderer.box(blockPosTarget, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (blockPosTarget == null || !renderProgress.get() || target == null || mc.player.getAbilities().creativeMode) {
            return;
        }
        final Vec3 pos = new Vec3(blockPosTarget.getX() + 0.5, blockPosTarget.getY() + 0.5, blockPosTarget.getZ() + 0.5);
        if (NametagUtils.to2D(pos, progressScale.get())) {
            NametagUtils.begin(pos);
            TextRenderer.get().begin(1.0, false, true);
            final String progress = Math.round((max - timer) / (double) max * 100.0) + "%";
            TextRenderer.get().render(progress, -TextRenderer.get().getWidth(progress) / 2.0, 0.0, progressColor.get());
            TextRenderer.get().end();
            NametagUtils.end();
        }
    }

    public int getBlockBreakingSpeed(final BlockState block, final BlockPos pos, final int slot) {
        final PlayerEntity player = mc.player;
        float f = player.getInventory().getStack(slot).getMiningSpeedMultiplier(block);
        if (f > 1.0f) {
            final int i = EnchantmentHelper.get(player.getInventory().getStack(slot)).getOrDefault(Enchantments.EFFICIENCY, 0);
            if (i > 0) {
                f += i * i + 1;
            }
        }
        if (StatusEffectUtil.hasHaste((LivingEntity) player)) {
            f *= 1.0f + (StatusEffectUtil.getHasteAmplifier((LivingEntity) player) + 1) * 0.2f;
        }
        if (player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            float k;
            switch (player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) {
                case 0: {
                    k = 0.3f;
                    break;
                }
                case 1: {
                    k = 0.09f;
                    break;
                }
                case 2: {
                    k = 0.0027f;
                    break;
                }
                default: {
                    k = 8.1E-4f;
                    break;
                }
            }
            f *= k;
        }
        if (player.isSubmergedIn((Tag) FluidTags.WATER) && !EnchantmentHelper.hasAquaAffinity((LivingEntity) player)) {
            f /= 5.0f;
        }
        if (!player.isOnGround()) {
            f /= 5.0f;
        }
        final float t = block.getHardness(mc.world, pos);
        if (t == -1.0f) {
            return 0;
        }
        return (int) Math.ceil(1.0f / (f / t / 30.0f));
    }

    @Override
    public String getInfoString() {
        if (target != null) {
            return target.getEntityName();
        }
        return null;
    }
}

