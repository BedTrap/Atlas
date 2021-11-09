package me.bedtrapteam.addon.mixins;

import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerInteractEntityC2SPacket.class)
public interface PlayerInteractEntityC2SPacketAccessor {
    @Accessor(value = "entityId")
    void setEntityId(int entityId);

    @Accessor(value = "type")
    void setType(PlayerInteractEntityC2SPacket.InteractTypeHandler type);

    @Accessor(value = "playerSneaking")
    void setPlayerSneaking(boolean playerSneaking);
}
