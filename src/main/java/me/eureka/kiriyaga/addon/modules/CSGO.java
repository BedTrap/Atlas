package me.eureka.kiriyaga.addon.modules;

import com.mojang.blaze3d.systems.RenderSystem;
import me.eureka.kiriyaga.addon.Nigger;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.EXPThrower;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.Items;
import net.minecraft.util.math.*;

public class CSGO extends Module {
    private final SettingGroup sg_spinbot = settings.createGroup("Anti Aim", true);
    private final SettingGroup sg_skeleton_esp = settings.createGroup("Skeleton ESP", false);
    private final SettingGroup sg_hitmarker = settings.createGroup("HitMarker", false);

    private final Setting<Boolean> anti_aim = sg_spinbot.add(new BoolSetting.Builder().name("anti-aim").defaultValue(true).build());
    private final Setting<anti_desync> spinbot_anti_decync = sg_spinbot.add(new EnumSetting.Builder<anti_desync>().name("anti-desync").defaultValue(anti_desync.All).visible(anti_aim::get).build());
    private final Setting<Boolean> yaw = sg_spinbot.add(new BoolSetting.Builder().name("yaw").defaultValue(true).visible(anti_aim::get).build());
    private final Setting<Integer> yaw_speed = sg_spinbot.add(new IntSetting.Builder().name("yaw-speed").defaultValue(5).min(0).sliderMax(100).visible(anti_aim::get).build());
    private final Setting<Boolean> pitch = sg_spinbot.add(new BoolSetting.Builder().name("pitch").defaultValue(false).visible(anti_aim::get).build());
    private final Setting<Integer> pitch_speed = sg_spinbot.add(new IntSetting.Builder().name("pitch-speed").defaultValue(5).min(0).sliderMax(100).visible(anti_aim::get).build());

    private final Setting<Boolean> skeleton_esp = sg_skeleton_esp.add(new BoolSetting.Builder().name("skeleton-ESP").defaultValue(false).build());
    private final Setting<SettingColor> skeleton_color = sg_skeleton_esp.add(new ColorSetting.Builder().name("players-color").defaultValue(new SettingColor(255, 255, 255)).visible(skeleton_esp::get).build());

    private final Setting<Boolean> hitsound = sg_hitmarker.add(new BoolSetting.Builder().name("hitsound").defaultValue(false).build());

    public CSGO() {
        super(Nigger.Category, "CS:GO", "yes.");
    }

    private short count = 0;
    private short yaw_count = 0;
    private short pitch_count = 0;

    @Override
    public void onActivate() {
        count = 0;
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        switch (spinbot_anti_decync.get()) {
            case All -> {
                if (Modules.get().isActive(EXPThrower.class) ||
                    Modules.get().isActive(BedBomb.class) ||
                    Modules.get().isActive(meteordevelopment.meteorclient.systems.modules.combat.BedAura.class) ||
                    mc.player.getMainHandStack().getItem() instanceof ExperienceBottleItem ||
                    mc.player.getOffHandStack().getItem() instanceof ExperienceBottleItem ||
                    mc.player.getMainHandStack().getItem() instanceof EnderPearlItem ||
                    mc.player.getOffHandStack().getItem() instanceof EnderPearlItem ||
                    mc.player.getMainHandStack().getItem() instanceof ExperienceBottleItem ||
                    mc.player.getOffHandStack().getItem() instanceof ExperienceBottleItem ||
                    mc.player.getMainHandStack().getItem() instanceof BowItem ||
                    mc.player.getOffHandStack().getItem() instanceof BowItem ||
                    mc.player.getInventory().getArmorStack(2).getItem() == Items.ELYTRA) return;
            }
            case ExceptElytra -> {
                if (Modules.get().isActive(EXPThrower.class) ||
                    Modules.get().isActive(BedBomb.class) ||
                    Modules.get().isActive(meteordevelopment.meteorclient.systems.modules.combat.BedAura.class) ||
                    mc.player.getMainHandStack().getItem() instanceof ExperienceBottleItem ||
                    mc.player.getOffHandStack().getItem() instanceof ExperienceBottleItem ||
                    mc.player.getMainHandStack().getItem() instanceof EnderPearlItem ||
                    mc.player.getOffHandStack().getItem() instanceof EnderPearlItem ||
                    mc.player.getMainHandStack().getItem() instanceof ExperienceBottleItem ||
                    mc.player.getOffHandStack().getItem() instanceof ExperienceBottleItem ||
                    mc.player.getMainHandStack().getItem() instanceof BowItem ||
                    mc.player.getOffHandStack().getItem() instanceof BowItem) return;
            }
        }

        yaw_count += yaw_speed.get();
        if (yaw_count > 180) {
            yaw_count = -180;
        }

        if (pitch.get()) {
            count++;
            if (count <= pitch_speed.get()) {
                pitch_count = 90;
            }
            if (count > pitch_speed.get()) {
                pitch_count = -90;
            }
            if (count >= pitch_speed.get() + pitch_speed.get()) {
                count = 0;
            }
        }

        if (yaw.get() && !pitch.get()) {
            Rotations.rotate(yaw_count, mc.player.getPitch());
        }
        if (!yaw.get() && pitch.get()) {
            Rotations.rotate(mc.player.getYaw(), pitch_count);
        }
        if (yaw.get() && pitch.get()) {
            Rotations.rotate(yaw_count, pitch_count);
        }
    }

    @EventHandler
    public void onAttack(AttackEntityEvent event) {
        if (event.entity instanceof PlayerEntity) {

        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (skeleton_esp.get()) {
            MatrixStack matrixStack = event.matrices;
            float g = event.tickDelta;
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.disableTexture();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(MinecraftClient.isFabulousGraphicsOrBetter());
            RenderSystem.enableCull();
            mc.world.getEntities().forEach(entity -> {
                if (!(entity instanceof PlayerEntity)) return;
                if (mc.options.getPerspective() == Perspective.FIRST_PERSON && !Modules.get().get(Freecam.class).isActive() && mc.player == entity)
                    return;
                int rotationHoldTicks = Config.get().rotationHoldTicks;

                Color skeletonColor = PlayerUtils.getPlayerColor((PlayerEntity) entity, skeleton_color.get());
                PlayerEntity playerEntity = (PlayerEntity) entity;

                Vec3d footPos = getEntityRenderPosition(playerEntity, g);
                PlayerEntityRenderer livingEntityRenderer = (PlayerEntityRenderer) (LivingEntityRenderer<?, ?>) mc.getEntityRenderDispatcher().getRenderer(playerEntity);
                PlayerEntityModel<PlayerEntity> playerEntityModel = (PlayerEntityModel) livingEntityRenderer.getModel();

                float h = MathHelper.lerpAngleDegrees(g, playerEntity.prevBodyYaw, playerEntity.bodyYaw);
                if (mc.player == entity && Rotations.rotationTimer < rotationHoldTicks) h = Rotations.serverYaw;
                float j = MathHelper.lerpAngleDegrees(g, playerEntity.prevHeadYaw, playerEntity.headYaw);
                if (mc.player == entity && Rotations.rotationTimer < rotationHoldTicks) j = Rotations.serverYaw;

                float q = playerEntity.limbAngle - playerEntity.limbDistance * (1.0F - g);
                float p = MathHelper.lerp(g, playerEntity.lastLimbDistance, playerEntity.limbDistance);
                float o = (float) playerEntity.age + g;
                float k = j - h;
                float m = playerEntity.getPitch(g);
                if (mc.player == entity && Rotations.rotationTimer < rotationHoldTicks) m = Rotations.serverPitch;

                playerEntityModel.animateModel(playerEntity, q, p, g);
                playerEntityModel.setAngles(playerEntity, q, p, o, k, m);

                boolean swimming = playerEntity.isInSwimmingPose();
                boolean sneaking = playerEntity.isSneaking();
                boolean flying = playerEntity.isFallFlying();

                ModelPart head = playerEntityModel.head;
                ModelPart leftArm = playerEntityModel.leftArm;
                ModelPart rightArm = playerEntityModel.rightArm;
                ModelPart leftLeg = playerEntityModel.leftLeg;
                ModelPart rightLeg = playerEntityModel.rightLeg;

                matrixStack.translate(footPos.x, footPos.y, footPos.z);
                if (swimming)
                    matrixStack.translate(0, 0.35f, 0);

                matrixStack.multiply(new Quaternion(new Vec3f(0, -1, 0), h + 180, true));
                if (swimming || flying)
                    matrixStack.multiply(new Quaternion(new Vec3f(-1, 0, 0), 90 + m, true));
                if (swimming)
                    matrixStack.translate(0, -0.95f, 0);

                BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
                bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

                Matrix4f matrix4f = matrixStack.peek().getModel();
                bufferBuilder.vertex(matrix4f, 0, sneaking ? 0.6f : 0.7f, sneaking ? 0.23f : 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
                bufferBuilder.vertex(matrix4f, 0, sneaking ? 1.05f : 1.4f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();//spine

                bufferBuilder.vertex(matrix4f, -0.37f, sneaking ? 1.05f : 1.35f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();//shoulders
                bufferBuilder.vertex(matrix4f, 0.37f, sneaking ? 1.05f : 1.35f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();

                bufferBuilder.vertex(matrix4f, -0.15f, sneaking ? 0.6f : 0.7f, sneaking ? 0.23f : 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();//pelvis
                bufferBuilder.vertex(matrix4f, 0.15f, sneaking ? 0.6f : 0.7f, sneaking ? 0.23f : 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();

                matrixStack.push();//head
                matrixStack.translate(0, sneaking ? 1.05f : 1.4f, 0);
                rotate(matrixStack, head);
                matrix4f = matrixStack.peek().getModel();
                bufferBuilder.vertex(matrix4f, 0, 0, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
                bufferBuilder.vertex(matrix4f, 0, 0.15f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
                matrixStack.pop();

                matrixStack.push();//right leg
                matrixStack.translate(0.15f, sneaking ? 0.6f : 0.7f, sneaking ? 0.23f : 0);
                rotate(matrixStack, rightLeg);
                matrix4f = matrixStack.peek().getModel();
                bufferBuilder.vertex(matrix4f, 0, 0, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
                bufferBuilder.vertex(matrix4f, 0, -0.6f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
                matrixStack.pop();

                matrixStack.push();//left leg
                matrixStack.translate(-0.15f, sneaking ? 0.6f : 0.7f, sneaking ? 0.23f : 0);
                rotate(matrixStack, leftLeg);
                matrix4f = matrixStack.peek().getModel();
                bufferBuilder.vertex(matrix4f, 0, 0, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
                bufferBuilder.vertex(matrix4f, 0, -0.6f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
                matrixStack.pop();

                matrixStack.push();//right arm
                matrixStack.translate(0.37f, sneaking ? 1.05f : 1.35f, 0);
                rotate(matrixStack, rightArm);
                matrix4f = matrixStack.peek().getModel();
                bufferBuilder.vertex(matrix4f, 0, 0, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
                bufferBuilder.vertex(matrix4f, 0, -0.55f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
                matrixStack.pop();

                matrixStack.push();//left arm
                matrixStack.translate(-0.37f, sneaking ? 1.05f : 1.35f, 0);
                rotate(matrixStack, leftArm);
                matrix4f = matrixStack.peek().getModel();
                bufferBuilder.vertex(matrix4f, 0, 0, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
                bufferBuilder.vertex(matrix4f, 0, -0.55f, 0).color(skeletonColor.r, skeletonColor.g, skeletonColor.b, skeletonColor.a).next();
                matrixStack.pop();

                bufferBuilder.end();
                BufferRenderer.draw(bufferBuilder);

                if (swimming)
                    matrixStack.translate(0, 0.95f, 0);
                if (swimming || flying)
                    matrixStack.multiply(new Quaternion(new Vec3f(1, 0, 0), 90 + m, true));
                if (swimming)
                    matrixStack.translate(0, -0.35f, 0);

                matrixStack.multiply(new Quaternion(new Vec3f(0, 1, 0), h + 180, true));
                matrixStack.translate(-footPos.x, -footPos.y, -footPos.z);
            });
            RenderSystem.enableTexture();
            RenderSystem.disableCull();
            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
        }
    }

    private void rotate(MatrixStack matrix, ModelPart modelPart) {
        if (modelPart.roll != 0.0F) {
            matrix.multiply(Vec3f.POSITIVE_Z.getRadialQuaternion(modelPart.roll));
        }

        if (modelPart.yaw != 0.0F) {
            matrix.multiply(Vec3f.NEGATIVE_Y.getRadialQuaternion(modelPart.yaw));
        }

        if (modelPart.pitch != 0.0F) {
            matrix.multiply(Vec3f.NEGATIVE_X.getRadialQuaternion(modelPart.pitch));
        }
    }

    private Vec3d getEntityRenderPosition(Entity entity, double partial) {
        double x = entity.prevX + ((entity.getX() - entity.prevX) * partial) - mc.getEntityRenderDispatcher().camera.getPos().x;
        double y = entity.prevY + ((entity.getY() - entity.prevY) * partial) - mc.getEntityRenderDispatcher().camera.getPos().y;
        double z = entity.prevZ + ((entity.getZ() - entity.prevZ) * partial) - mc.getEntityRenderDispatcher().camera.getPos().z;
        return new Vec3d(x, y, z);
    }

    public enum anti_desync {
        All,
        ExceptElytra,
        None
    }
}
