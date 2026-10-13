package holylois.boombox.mixins;

import holylois.boombox.ManagedPackMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.client.gui.screens.packs.TransferableSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Mixin(PackSelectionScreen.class)
public abstract class ManagedPackScreenMixin {
    @Shadow private TransferableSelectionList selectedPackList;
    @Unique private PackRepository holyLoisRepository;

    @Inject(method = "<init>", at = @At("RETURN"), require = 1)
    private void holyLoisRememberRepository(PackRepository repository, Consumer<PackRepository> output, Path dir, Component title, CallbackInfo ci) {
        holyLoisRepository = repository;
    }
    @Inject(method = "filterEntries", at = @At("RETURN"), require = 1)
    private void holyLoisResourceFolder(String query, Stream<PackSelectionModel.Entry> entries, TransferableSelectionList list, PackSelectionModel.EntryBase selected, CallbackInfo ci) {
        if (list == selectedPackList && holyLoisRepository == Minecraft.getInstance().getResourcePackRepository())
            ManagedPackMenu.attach((Screen)(Object)this, holyLoisRepository, list, query);
    }
}
