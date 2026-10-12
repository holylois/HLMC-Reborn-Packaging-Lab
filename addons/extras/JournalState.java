package holylois.boombox;

import java.util.List;
import java.util.ArrayList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Bounded journal pages. Personal catches/visits are sent only to their authenticated owner. */
public record JournalState(int tab, int filter, int page, int pages, int total, int size, String search, List<Entry> entries, int requestId) implements CustomPacketPayload {
    /** Zero is an explicit server command opening the Journal, not a reply to an older screen. */
    public JournalState(int tab,int filter,int page,int pages,int total,int size,String search,List<Entry> entries){this(tab,filter,page,pages,total,size,search,entries,0);}
    public static final int FISH=1, PLACE=2, KILL=3, FEAT=4;
    public record Entry(String key, int kind, String name, String by, String dimension, int x, int y, int z,
                        long time, long count, double weight, int rarities, int level, boolean location, boolean first,List<Long> rarityCounts,long shiny) {
        public Entry(String key,int kind,String name,String by,String dimension,int x,int y,int z,long time,long count,double weight,int rarities,int level,boolean location,boolean first){this(key,kind,name,by,dimension,x,y,z,time,count,weight,rarities,level,location,first,List.of(),0);}
        public Entry{rarityCounts=List.copyOf(rarityCounts);if(rarityCounts.size()>6||rarityCounts.stream().anyMatch(v->v<0)||shiny<0)throw new IllegalArgumentException("Invalid fish totals");}
    }
    public static final Type<JournalState> TYPE = new Type<>(Identifier.fromNamespaceAndPath("holylois", "journal_page_v3"));
    public JournalState {
        if(tab<0||tab>1||filter<0||filter>4||page<0||pages<1||page>=pages||total<0||size<1||size>8||search.length()>48||entries.size()>size||requestId<0)
            throw new IllegalArgumentException("Invalid journal page");
        entries=List.copyOf(entries);
    }
    public static final StreamCodec<RegistryFriendlyByteBuf, JournalState> CODEC = CustomPacketPayload.codec((v,b) -> {
        b.writeVarInt(v.tab);b.writeVarInt(v.filter);b.writeVarInt(v.page);b.writeVarInt(v.pages);b.writeVarInt(v.total);b.writeVarInt(v.size);b.writeUtf(v.search,48);b.writeVarInt(v.requestId);b.writeVarInt(v.entries.size());
        for(var e:v.entries){
            b.writeUtf(e.key,256);b.writeVarInt(e.kind);b.writeUtf(e.name,160);b.writeUtf(e.by,32);b.writeUtf(e.dimension,128);
            b.writeInt(e.x);b.writeInt(e.y);b.writeInt(e.z);b.writeLong(e.time);b.writeVarLong(e.count);b.writeDouble(e.weight);
            b.writeVarInt(e.rarities);b.writeVarInt(e.level);b.writeBoolean(e.location);b.writeBoolean(e.first);
            b.writeVarInt(e.rarityCounts.size());for(long count:e.rarityCounts)b.writeVarLong(count);b.writeVarLong(e.shiny);
        }
    }, b -> {
        int tab=b.readVarInt(),filter=b.readVarInt(),page=b.readVarInt(),pages=b.readVarInt(),total=b.readVarInt(),size=b.readVarInt();String search=b.readUtf(48);
        int requestId=b.readVarInt(),count=b.readVarInt();if(count<0||count>8)throw new IllegalArgumentException("Invalid journal size");
        var entries=new ArrayList<Entry>();
        for(int i=0;i<count;i++){
            var entry=new Entry(b.readUtf(256),b.readVarInt(),b.readUtf(160),b.readUtf(32),b.readUtf(128),b.readInt(),b.readInt(),b.readInt(),b.readLong(),b.readVarLong(),b.readDouble(),b.readVarInt(),b.readVarInt(),b.readBoolean(),b.readBoolean());
            int tiers=b.readVarInt();if(tiers<0||tiers>6)throw new IllegalArgumentException("Invalid fish tier count");
            var counts=new ArrayList<Long>();for(int tier=0;tier<tiers;tier++)counts.add(b.readVarLong());
            entries.add(new Entry(entry.key,entry.kind,entry.name,entry.by,entry.dimension,entry.x,entry.y,entry.z,entry.time,entry.count,entry.weight,entry.rarities,entry.level,entry.location,entry.first,counts,b.readVarLong()));
        }
        return new JournalState(tab,filter,page,pages,total,size,search,entries,requestId);
    });
    @Override public Type<JournalState> type() { return TYPE; }
}
