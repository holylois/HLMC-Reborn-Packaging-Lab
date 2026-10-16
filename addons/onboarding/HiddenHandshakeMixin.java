package holylois.mixins;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.server.network.ServerHandshakePacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Remembers the address the client typed, so holylois.Hidden can tell the server's own tools from public pings. */
@Mixin(ServerHandshakePacketListenerImpl.class)
public abstract class HiddenHandshakeMixin {
    @Shadow @Final private Connection connection;

    @Inject(method = "handleIntention", at = @At("HEAD"))
    private void holyLoisRememberHost(ClientIntentionPacket packet, CallbackInfo callback) {
        holylois.Hidden.rememberHost(connection, packet.hostName());
    }
}
