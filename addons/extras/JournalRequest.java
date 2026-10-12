package holylois.boombox;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** A read-only page request. The server chooses the viewer; there is no player selector. */
public record JournalRequest(int tab, int filter, int page, int size, String search, int requestId) implements CustomPacketPayload {
    public JournalRequest(int tab,int filter,int page,int size,String search){this(tab,filter,page,size,search,0);}
    public static final Type<JournalRequest> TYPE = new Type<>(Identifier.fromNamespaceAndPath("holylois", "journal_request_v2"));
    public JournalRequest {
        if (tab < 0 || tab > 1 || filter < 0 || filter > 3 || page < 0 || page > 100000 || size < 1 || size > 8 || search.length() > 48 || requestId<0)
            throw new IllegalArgumentException("Invalid journal request");
    }
    public static final StreamCodec<RegistryFriendlyByteBuf, JournalRequest> CODEC = CustomPacketPayload.codec((v,b) -> {
        b.writeVarInt(v.tab); b.writeVarInt(v.filter); b.writeVarInt(v.page); b.writeVarInt(v.size); b.writeUtf(v.search,48);b.writeVarInt(v.requestId);
    }, b -> new JournalRequest(b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readUtf(48),b.readVarInt()));
    @Override public Type<JournalRequest> type() { return TYPE; }
}
