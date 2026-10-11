package holylois;

import com.google.gson.Gson;
import holylois.boombox.JournalState;
import holylois.boombox.JournalRequest;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.RegistryAccess;
import io.netty.buffer.Unpooled;
import java.nio.file.Files;
import java.util.*;

/** Persistence, owner isolation, repeat credit and bounded wire pages without a live world or account database. */
public final class JournalBookTest {
    private static int checked;
    public static void main(String[] args)throws Exception{
        var book=new JournalBook();
        book.caught("minecraft:cod",1,0,"","minecraft:overworld",1,64,2,100);
        book.caught("minecraft:cod",1,8.5,"epic","minecraft:overworld",3,65,4,101);
        book.caught("minecraft:cod",1,2,"rare","minecraft:overworld",5,66,6,102);
        var cod=book.fish.get("minecraft:cod");
        check(cod.count==3&&cod.heaviest==8.5&&cod.rarities==(1|4|8),"Catches accumulate species, best measured weight and all rarity bands");
        book.caught("minecraft:salmon",1,Double.NaN,"common","minecraft:overworld",0,64,0,103);
        check(book.fish.get("minecraft:salmon").heaviest==0,"Unweighed catches do not invent a weight");
        check(book.visit("spire",100)&&!book.visit("spire",200)&&book.places.get("spire")==100,"Repeat visits keep the personal first visit");
        book.killed("minecraft:zombie",20,"minecraft:overworld",1,64,2,100);
        book.killed("minecraft:zombie",10,"minecraft:the_nether",3,65,4,200);
        check(book.kills.get("minecraft:zombie").level==20&&book.kills.get("minecraft:zombie").time==200,"Kills retain the best level and the latest location");
        var alice=UUID.randomUUID();var bob=UUID.randomUUID();var state=new Discoveries.State();state.journals.put(alice,book);state.journals.put(bob,new JournalBook());
        var restored=new Gson().fromJson(new Gson().toJson(state),Discoveries.State.class);
        check(restored.journals.get(alice).fish.get("minecraft:cod").count==3&&restored.journals.get(bob).fish.isEmpty(),"Saved journals stay separate by UUID across a reload or name change");
        var discoveries=new Discoveries();var first=discoveries.rememberMob("minecraft:zombie","Alice","minecraft:overworld",1,64,2,100,20);
        check(first!=null&&discoveries.rememberMob("minecraft:zombie","Bob","minecraft:overworld",3,64,4,200,50)==null,"Only one first per mob type");
        check(first.player.equals("Alice")&&first.level==50&&first.time==100&&first.x==1,"Higher-level kills preserve original first credit and its location");
        check(discoveries.rememberMob("minecraft:skeleton","Bob","minecraft:overworld",0,64,0,201,0)!=null,"Different mob types have independent firsts");
        var entries=new ArrayList<JournalState.Entry>();
        for(int i=0;i<18;i++)entries.add(new JournalState.Entry("place"+i,JournalState.PLACE,"Spire "+i,"Alice","minecraft:overworld",i,64,0,i,1,0,0,0,true,true));
        entries.add(new JournalState.Entry("fish",JournalState.FISH,"minecraft:cod","","minecraft:overworld",0,64,0,100,3,8.5,13,0,true,false));
        var page=JournalBook.page(entries,new JournalRequest(0,2,999,6,""));
        check(page.total()==18&&page.page()==2&&page.pages()==3&&page.entries().size()==6,"Filtering clamps excessive page requests and bounds response size");
        check(page.entries().getFirst().key().equals("place5"),"Pages sort newest first with deterministic ties");
        var result=JournalBook.page(entries,new JournalRequest(1,1,0,8,"CoD"));
        check(result.total()==1&&result.entries().getFirst().weight()==8.5,"Search is case insensitive inside the chosen category");
        var empty=JournalBook.page(entries,new JournalRequest(1,0,0,8,"no matches"));
        check(empty.total()==0&&empty.pages()==1&&empty.entries().isEmpty(),"Empty journals/searches have a usable first page");
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        JournalState.CODEC.encode(buffer,result);check(JournalState.CODEC.decode(buffer).equals(result),"Journal state round-trips every field");buffer.release();
        buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);var request=new JournalRequest(1,2,3,4,"spire");
        JournalRequest.CODEC.encode(buffer,request);check(JournalRequest.CODEC.decode(buffer).equals(request),"Read-only requests round-trip without a target identity");buffer.release();
        try{new JournalRequest(1,0,0,9,"");throw new AssertionError("Oversized request accepted");}catch(IllegalArgumentException expected){checked++;}
        try{new JournalRequest(1,0,0,8,"x".repeat(49));throw new AssertionError("Oversized search accepted");}catch(IllegalArgumentException expected){checked++;}
        var root=Files.createTempDirectory("hl-journal-");var file=root.resolve("holylois/discoveries.json");Files.createDirectories(file.getParent());
        Files.writeString(file,"{\"version\":2,\"found\":[{\"key\":\"overworld|spire|0,0\",\"name\":\"Ice Spire\",\"player\":\"Alice\",\"number\":1,\"first\":true,\"time\":100}],\"firsts\":[]}");
        discoveries.load(null,root);var upgraded=new Gson().fromJson(Files.readString(file),Discoveries.State.class);
        check(upgraded.version==3&&upgraded.found.size()==1&&upgraded.found.getFirst().name.equals("Ice Spire"),"Version 2 migrates without losing discoveries");
        Files.writeString(file,"not valid JSON");
        try{discoveries.load(null,root);throw new AssertionError("Corrupt file accepted");}catch(IllegalStateException expected){checked++;}
        check(Files.readString(file).equals("not valid JSON"),"A broken store is not overwritten with an empty journal");
        Files.delete(file);Files.delete(file.getParent());Files.delete(root);
        System.out.println("Journal checks: "+checked);
    }
    private static void check(boolean okay,String message){if(!okay)throw new AssertionError(message);checked++;}
}
