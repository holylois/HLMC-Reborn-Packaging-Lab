package holylois.boombox.mixins;

import net.minecraft.client.resources.language.ClientLanguage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * House style: players never see an em dash. Every translated string (vanilla and mods) shows a plain hyphen instead.
 * Liteminer's "Run /amber doctor for details." hint is dropped: the command is operator-only, so players only get the status.
 */
@Mixin(ClientLanguage.class)
public abstract class LanguageDashMixin {
    private static final char EM_DASH = (char) 0x2014;
    private static final java.util.Set<String> OPERATOR_HINTS = java.util.Set.of("liteminer.connection.doctor_hint");

    @Inject(method = "getOrDefault", at = @At("RETURN"), cancellable = true)
    private void holyLoisPlainDash(String key, String fallback, CallbackInfoReturnable<String> callback) {
        if (OPERATOR_HINTS.contains(key)) { callback.setReturnValue(""); return; }
        String text = callback.getReturnValue();
        if (text != null && text.indexOf(EM_DASH) >= 0) callback.setReturnValue(text.replace(EM_DASH, '-'));
    }
}
