package me.bedtrapteam.addon.mixins;

import me.bedtrapteam.addon.events.InteractEvent;
import me.bedtrapteam.addon.modules.atlas.combat.MultiTask;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.mixininterface.IMinecraftClient;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(
    value = {MinecraftClient.class},
    priority = 1001
)
public abstract class MinecraftoClientMixin implements IMinecraftClient {
    @Redirect(
        method = {"handleBlockBreaking"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"
        )
    )
    public boolean breakBlockCheck(ClientPlayerEntity clientPlayerEntity) {
        return Modules.get().isActive(MultiTask.class) ? false : ((InteractEvent) MeteorClient.EVENT_BUS.post((Object)InteractEvent.get(clientPlayerEntity.isUsingItem()))).usingItem;
    }

    @Redirect(
        method = {"doItemUse"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;isBreakingBlock()Z"
        )
    )
    public boolean useItemBreakCheck(ClientPlayerInteractionManager clientPlayerInteractionManager) {
        return Modules.get().isActive(MultiTask.class) ? false : ((InteractEvent)MeteorClient.EVENT_BUS.post((Object)InteractEvent.get(clientPlayerInteractionManager.isBreakingBlock()))).usingItem;
    }
}
