package me.murphy.addon.mixins;

import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.text.BaseText;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatUtils.class)
public class ChatUtilsMixin {
    @Inject(method = "getMeteorPrefix", at = @At("HEAD"), cancellable = true, remap = false)
    private static void getPrefix(CallbackInfoReturnable<BaseText> cir) {
        BaseText logo = new LiteralText("n1gger++");
        BaseText prefix = new LiteralText("");
        logo.setStyle(logo.getStyle().withFormatting(Formatting.BLACK));
        prefix.setStyle(prefix.getStyle().withFormatting(Formatting.GRAY));
        prefix.append("/");
        prefix.append(logo);
        prefix.append("/ ");
        cir.setReturnValue(prefix);
    }
}
