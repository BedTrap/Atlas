package me.bedtrapteam.addon.mixins;

import me.bedtrapteam.addon.modules.atlas.misc.InstantSneak;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    private float cameraY;

    @Shadow
    private Entity focusedEntity;

    @Inject(at = @At("HEAD"), method = "updateEyeHeight")
    public void updateEyeHeight(CallbackInfo ci) {
        if (Modules.get().isActive(InstantSneak.class) && focusedEntity != null) cameraY = focusedEntity.getStandingEyeHeight();
    }
}
