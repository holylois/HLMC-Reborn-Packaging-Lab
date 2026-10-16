package holylois.mixins;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.ListPlayersCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.util.List;
import java.util.function.Function;

/** /list shows staff only to operators and the console. */
@Mixin(ListPlayersCommand.class)
public abstract class HiddenListMixin {
    @Redirect(method = "format", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;getPlayers()Ljava/util/List;"))
    private static List<ServerPlayer> holyLoisVisiblePlayers(PlayerList players, CommandSourceStack source, Function<ServerPlayer, Component> name) {
        return holylois.Hidden.visibleTo(source.getServer(), source.getPlayer());
    }
}
