package holylois.mixins;

import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.List;

/** Lets holylois.Hidden send a per-viewer copy of a Tab list update. */
@Mixin(ClientboundPlayerInfoUpdatePacket.class)
public interface PlayerInfoEntriesAccessor {
    @Mutable @Accessor("entries")
    void holyLoisSetEntries(List<ClientboundPlayerInfoUpdatePacket.Entry> entries);
}
