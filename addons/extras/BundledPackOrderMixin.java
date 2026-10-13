package holylois.boombox.mixins;

import holylois.boombox.BundledPackOrder;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Collection;
import java.util.List;

@Mixin(PackRepository.class)
public abstract class BundledPackOrderMixin {
    @Inject(method = "rebuildSelected", at = @At("RETURN"), cancellable = true, require = 1)
    private void holyLoisManagedPackOrder(Collection<String> ids, CallbackInfoReturnable<List<Pack>> result) {
        var client = Minecraft.getInstance();
        if (client == null || (Object)this != client.getResourcePackRepository()) return;
        var before = result.getReturnValue();
        var after = BundledPackOrder.arrange(before, Pack::getId,
            pack -> pack.getId().equals("vanilla") || pack.isRequired() && !pack.getId().startsWith("file/")
                && pack.getPackSource() != PackSource.SERVER && pack.getPackSource() != PackSource.WORLD);
        if (after != before) result.setReturnValue(after);
    }
}
