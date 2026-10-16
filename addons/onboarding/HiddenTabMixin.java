package holylois.mixins;

import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Every packet to a player passes here: Tab list updates get the per-viewer copy from holylois.Hidden. */
@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class HiddenTabMixin {
    @ModifyVariable(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V", at = @At("HEAD"), argsOnly = true)
    private Packet<?> holyLoisUnlistStaff(Packet<?> packet, Packet<?> original, ChannelFutureListener listener) {
        return (Object) this instanceof ServerGamePacketListenerImpl game ? holylois.Hidden.forViewer(packet, game.player) : packet;
    }
}
