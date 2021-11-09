package me.bedtrapteam.addon.mixins;

import me.bedtrapteam.addon.modules.atlas.misc.HandAnimations;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRenderenMixin {
    @Shadow
    private float field_4053;
    @Shadow
    private float field_4043;
    @Shadow
    private float field_4052;
    @Shadow
    private float field_4051;
    @Shadow
    private ItemStack field_4048;
    @Shadow
    private ItemStack field_4047;

    @Inject(method = {"updateHeldItems"}, at = {@At("HEAD")}, cancellable = true)
    public void updateHeldItems1(CallbackInfo ci) {
        HandAnimations handProgress = Modules.get().get(HandAnimations.class);
        if (handProgress.isActive()) {
            ci.cancel();
            this.field_4053 = this.field_4043;
            this.field_4051 = this.field_4052;
            ClientPlayerEntity clientPlayerEntity = MinecraftClient.getInstance().player;
            ItemStack itemStack = clientPlayerEntity.getMainHandStack();
            ItemStack itemStack2 = clientPlayerEntity.getOffHandStack();
            if (ItemStack.areEqual(this.field_4047, itemStack)) {
                this.field_4047 = itemStack;
            }

            if (ItemStack.areEqual(this.field_4048, itemStack2)) {
                this.field_4048 = itemStack2;
            }

            if (clientPlayerEntity.isRiding()) {
                this.field_4043 = MathHelper.clamp(this.field_4043 - 0.4F, 0.0F, 1.0F);
                this.field_4052 = MathHelper.clamp(this.field_4052 - 0.4F, 0.0F, 1.0F);
            } else {
                this.field_4043 += MathHelper.clamp((this.field_4047 == itemStack ? 1.0F : 0.0F) - this.field_4043, -0.4F, 0.4F);
                this.field_4052 += MathHelper.clamp((float) (this.field_4048 == itemStack2 ? 1 : 0) - this.field_4052, -0.4F, 0.4F);
            }

            if (this.field_4043 < 0.1F) {
                this.field_4047 = itemStack;
            }

            if (this.field_4052 < 0.1F) {
                this.field_4048 = itemStack2;
            }
        }

    }

    @Inject(method = "renderFirstPersonItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformation$Mode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"))
    private void sex(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (!item.isEmpty()) Modules.get().get(HandAnimations.class).transform(matrices);
    }
}
