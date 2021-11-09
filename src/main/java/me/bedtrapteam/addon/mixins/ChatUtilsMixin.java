package me.bedtrapteam.addon.mixins;

import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.text.BaseText;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatUtils.class)
public class ChatUtilsMixin {
    @Inject(method = "getPrefix", at = @At("HEAD"), cancellable = true, remap = false)
    private static void getPrefix(CallbackInfoReturnable<BaseText> cir) {
        BaseText PREFIX = new LiteralText("Atlas");
        BaseText prefix = new LiteralText("");
        PREFIX.setStyle(PREFIX.getStyle().withFormatting(Formatting.AQUA));
        prefix.setStyle(prefix.getStyle().withFormatting(Formatting.DARK_AQUA));
        prefix.append("<");
        prefix.append(PREFIX);
        prefix.append("> ");
        cir.setReturnValue(prefix);
    }
}
