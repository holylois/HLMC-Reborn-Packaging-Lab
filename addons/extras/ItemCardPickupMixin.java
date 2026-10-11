package holylois.boombox.mixins;

import holylois.boombox.ItemCardsEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Item cards: the local player picked up an item entity (read before vanilla removes it). */
@Mixin(ClientPacketListener.class)
public abstract class ItemCardPickupMixin {
    @Inject(method = "handleTakeItemEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V", shift = At.Shift.AFTER), require = 0)
    private void holyLoisItemCard(ClientboundTakeItemEntityPacket packet, CallbackInfo info) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || packet.getPlayerId() != mc.player.getId()) return;
        if (mc.level.getEntity(packet.getItemId()) instanceof ItemEntity item) ItemCardsEntry.pickedUp(item.getItem().copyWithCount(Math.max(1, packet.getAmount())));
    }
}
