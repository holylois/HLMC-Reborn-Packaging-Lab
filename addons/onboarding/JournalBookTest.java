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
        check(book.caught("minecraft:cod",1,0,"","minecraft:overworld",1,64,2,100),"First species earns one private hint");
        check(book.caught("minecraft:cod",1,8.5,"epic","minecraft:overworld",3,65,4,101),"A first rarity band is a new collection entry");
        book.caught("minecraft:cod",1,2,"rare","minecraft:overworld",5,66,6,102);
        var cod=book.fish.get("minecraft:cod");
        check(cod.count==3&&cod.heaviest==8.5&&cod.rarities==(1|4|8),"Catches accumulate species, best measured weight and all rarity bands");
        check(cod.rarityCounts[0]==1&&cod.rarityCounts[2]==1&&cod.rarityCounts[3]==1,"Rarity counts come only from actual catches");
        book.caught("minecraft:cod",2,3,"legendary",true,"minecraft:overworld",0,64,0,104);
        check(cod.shiny==2&&cod.rarityCounts[4]==2,"Shiny catches also count in their rarity tier");
        var caughtBest=new holylois.boombox.JournalFish("minecraft:cod","",8.5,"epic",false,101);
        book.fishAppearance(caughtBest);book.fishAppearance(new holylois.boombox.JournalFish("minecraft:cod","",3,"legendary",true,104));
        check(cod.best.equals(caughtBest),"Heaviest actual appearance stays separate from a newer lighter catch");
        var oldBest=new JournalBook();oldBest.caught("minecraft:cod",1,20,"mythic",false,"minecraft:overworld",0,64,0,1);oldBest.fishAppearance(caughtBest);
        check(oldBest.fish.get("minecraft:cod").best==null,"A lighter new fish cannot fabricate the missing historical heaviest appearance");
        var legacy=new Gson().fromJson("{\"fish\":{\"minecraft:cod\":{\"species\":\"minecraft:cod\",\"count\":10,\"rarities\":4}}}",JournalBook.class);
        legacy.caught("minecraft:cod",1,1,"rare",false,"minecraft:overworld",0,64,0,105);
        check(legacy.fish.get("minecraft:cod").count==11&&legacy.fish.get("minecraft:cod").rarityCounts[2]==1,"Legacy totals are preserved without inventing old per-rarity counts");
        check(!legacy.caught("minecraft:cod",1,1,"rare",false,"minecraft:overworld",0,64,0,106),"Repeated catches of the same known tier stay quiet");
        book.caught("minecraft:salmon",1,Double.NaN,"common","minecraft:overworld",0,64,0,103);
        check(book.fish.get("minecraft:salmon").heaviest==0,"Unweighed catches do not invent a weight");
        check(book.visit("spire",100)&&!book.visit("spire",200)&&book.places.get("spire")==100,"Repeat visits keep the personal first visit");
        check(book.killed("minecraft:zombie",20,"minecraft:overworld",1,64,2,100),"First personal creature defeat earns a hint");
        check(!book.killed("minecraft:zombie",10,"minecraft:the_nether",3,65,4,200),"Repeat creature defeats stay quiet");
        check(book.kills.get("minecraft:zombie").level==20&&book.kills.get("minecraft:zombie").time==200,"Kills retain the best level and the latest location");
        var record=new holylois.boombox.DefeatRecord("minecraft:zombie",20,40,false,false,100,"minecraft:overworld",1,64,2,List.of(),List.of(),List.of());
        book.strongest(record);book.strongest(new holylois.boombox.DefeatRecord("minecraft:zombie",10,80,false,false,200,"minecraft:overworld",3,65,4,List.of(),List.of(),List.of()));
        check(book.kills.get("minecraft:zombie").strongest.time()==100&&book.kills.get("minecraft:zombie").time==200,"strongest time/location stay separate from latest defeat");
        var alice=UUID.randomUUID();var bob=UUID.randomUUID();var state=new Discoveries.State();state.journals.put(alice,book);state.journals.put(bob,new JournalBook());
        var restored=new Gson().fromJson(new Gson().toJson(state),Discoveries.State.class);
        check(restored.journals.get(alice).kills.get("minecraft:zombie").strongest.equals(record),"strongest metadata survives the saved journal reload");
        check(restored.journals.get(alice).fish.get("minecraft:cod").best.equals(caughtBest),"Best fish appearance survives saved journal reload");
        check(restored.journals.get(alice).fish.get("minecraft:cod").count==5&&restored.journals.get(bob).fish.isEmpty(),"Saved journals stay separate by UUID across a reload or name change");
        check(Discoveries.owns(alice,"RenamedAlice",alice,"Alice")&&!Discoveries.owns(bob,"Bob",alice,"Alice"),"World-first credit follows the owner UUID, not everyone who visits");
        check(Discoveries.owns(alice,"ALICE",null,"Alice")&&!Discoveries.owns(alice,"Bob",null,"Alice"),"Legacy firsts use an exact case-insensitive name match");
        check(Discoveries.hostile(net.minecraft.world.entity.MobCategory.MONSTER,false)&&!Discoveries.hostile(net.minecraft.world.entity.MobCategory.CREATURE,false)&&!Discoveries.hostile(net.minecraft.world.entity.MobCategory.WATER_AMBIENT,false),"Passive and fish categories do not get world-first kills");
        var discoveries=new Discoveries();var first=discoveries.rememberMob("minecraft:zombie","Alice","minecraft:overworld",1,64,2,100,20);
        check(first!=null&&discoveries.rememberMob("minecraft:zombie","Bob","minecraft:overworld",3,64,4,200,50)==null,"Only one first per mob type");
        check(first.player.equals("Alice")&&first.level==50&&first.firstLevel==20&&first.time==100&&first.x==1,"Higher-level kills preserve the first defeated level, credit and location");
        var savedFirst=new Gson().fromJson(new Gson().toJson(first),Discoveries.First.class);
        check(savedFirst.firstLevel==20&&savedFirst.level==50,"First defeat level and later highest level survive saving separately");
        check(Discoveries.firstType(savedFirst).equals("minecraft:zombie"),"Saved firsts retain their entity subject for appearance and drop icons");
        check(new Gson().fromJson("{\"level\":50}",Discoveries.First.class).firstLevel==null,"Legacy highest levels do not invent the first defeat level");
        check(discoveries.rememberMob("minecraft:skeleton","Bob","minecraft:overworld",0,64,0,201,0)!=null,"Different mob types have independent firsts");
        var entries=new ArrayList<JournalState.Entry>();
        for(int i=0;i<18;i++)entries.add(new JournalState.Entry("place"+i,JournalState.PLACE,"Spire "+i,"Alice","minecraft:overworld",i,64,0,i,1,0,0,0,true,true));
        entries.add(new JournalState.Entry("fish",JournalState.FISH,"minecraft:cod","","minecraft:overworld",0,64,0,100,3,8.5,13,0,true,false));
        var page=JournalBook.page(entries,new JournalRequest(0,2,999,6,""));
        check(page.total()==18&&page.page()==2&&page.pages()==3&&page.entries().size()==6,"Filtering clamps excessive page requests and bounds response size");
        check(page.entries().getFirst().key().equals("place5"),"Pages sort newest first with deterministic ties");
        check(JournalBook.page(entries,new JournalRequest(1,4,0,8,"")).total()==18,"Personal firsts filter contains only credited entries");
        var result=JournalBook.page(entries,new JournalRequest(1,1,0,8,"CoD",17));
        check(result.requestId()==17,"Server replies echo the client request identity");
        check(result.total()==1&&result.entries().getFirst().weight()==8.5,"Search is case insensitive inside the chosen category");
        var empty=JournalBook.page(entries,new JournalRequest(1,0,0,8,"no matches"));
        check(empty.total()==0&&empty.pages()==1&&empty.entries().isEmpty(),"Empty journals/searches have a usable first page");
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        JournalState.CODEC.encode(buffer,result);check(JournalState.CODEC.decode(buffer).equals(result),"Journal state round-trips every field");buffer.release();
        buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);var request=new JournalRequest(1,2,3,4,"spire",123);
        var recordPage=new JournalState(1,3,0,1,1,8,"",List.of(new JournalState.Entry("kill:minecraft:zombie",3,"minecraft:zombie","","minecraft:overworld",1,64,2,200,2,0,0,20,true,false).withStrongest(record)));
        JournalState.CODEC.encode(buffer,recordPage);check(JournalState.CODEC.decode(buffer).equals(recordPage),"strongest record and entity subject survive bounded v6 packet roundtrip");buffer.clear();
        var fishPage=new JournalState(1,1,0,1,1,8,"",List.of(new JournalState.Entry("fish:minecraft:cod",1,"minecraft:cod","","minecraft:overworld",0,64,0,104,5,8.5,25,0,true,false).withFish(caughtBest)));
        JournalState.CODEC.encode(buffer,fishPage);check(JournalState.CODEC.decode(buffer).equals(fishPage),"Best fish appearance survives bounded v6 packet roundtrip");buffer.clear();
        JournalRequest.CODEC.encode(buffer,request);check(JournalRequest.CODEC.decode(buffer).equals(request),"Read-only requests round-trip without a target identity");buffer.release();
        try{new JournalRequest(1,0,0,9,"");throw new AssertionError("Oversized request accepted");}catch(IllegalArgumentException expected){checked++;}
        try{new JournalRequest(1,0,0,8,"x".repeat(49));throw new AssertionError("Oversized search accepted");}catch(IllegalArgumentException expected){checked++;}
        try{new JournalRequest(1,0,0,8,"",-1);throw new AssertionError("Negative request identity accepted");}catch(IllegalArgumentException expected){checked++;}
        var root=Files.createTempDirectory("hl-journal-");var file=root.resolve("holylois/discoveries.json");Files.createDirectories(file.getParent());
        Files.writeString(file,"{\"version\":2,\"found\":[{\"key\":\"overworld|spire|0,0\",\"name\":\"Ice Spire\",\"player\":\"Alice\",\"number\":1,\"first\":true,\"time\":100}],\"firsts\":[]}");
        discoveries.load(null,root);var upgraded=new Gson().fromJson(Files.readString(file),Discoveries.State.class);
        check(upgraded.version==4&&upgraded.found.size()==1&&upgraded.found.getFirst().name.equals("Ice Spire"),"Version 2 migrates to snapshot-capable schema 4 without losing discoveries");
        Files.writeString(file,"not valid JSON");
        try{discoveries.load(null,root);throw new AssertionError("Corrupt file accepted");}catch(IllegalStateException expected){checked++;}
        check(Files.readString(file).equals("not valid JSON"),"A broken store is not overwritten with an empty journal");
        Files.delete(file);Files.delete(file.getParent());Files.delete(root);
        System.out.println("Journal checks: "+checked);
    }
    private static void check(boolean okay,String message){if(!okay)throw new AssertionError(message);checked++;}
}
