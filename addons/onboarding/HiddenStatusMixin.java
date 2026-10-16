package holylois.mixins;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.network.ServerStatusPacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** The server-list reply without staff. Runs after MiniMOTD, which redirects the status field read in the same method. */
@Mixin(ServerStatusPacketListenerImpl.class)
public abstract class HiddenStatusMixin {
    @Shadow @Final private Connection connection;

    @ModifyArg(method = "handleStatusRequest", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/network/protocol/status/ClientboundStatusResponsePacket;<init>(Lnet/minecraft/network/protocol/status/ServerStatus;)V"))
    private ServerStatus holyLoisHideStaff(ServerStatus status) {
        return holylois.Hidden.publicStatus(connection, status);
    }
}
