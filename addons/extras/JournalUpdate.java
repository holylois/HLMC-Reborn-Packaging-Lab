package holylois.boombox;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Private first-entry hint, with no location or other player's identity. */
public record JournalUpdate(int kind) implements CustomPacketPayload {
    public static final Type<JournalUpdate> TYPE=new Type<>(Identifier.fromNamespaceAndPath("holylois","journal_update"));
    public JournalUpdate{if(kind<1||kind>3)throw new IllegalArgumentException("Invalid journal hint");}
    public static final StreamCodec<RegistryFriendlyByteBuf,JournalUpdate> CODEC=CustomPacketPayload.codec((v,b)->b.writeVarInt(v.kind),b->new JournalUpdate(b.readVarInt()));
    @Override public Type<JournalUpdate> type(){return TYPE;}
}
