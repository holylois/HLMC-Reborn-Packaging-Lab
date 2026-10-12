package holylois;

import java.util.*;
import holylois.boombox.JournalState;
import holylois.boombox.JournalRequest;

/** Saved by Discoveries, keyed by stable player UUID. No invented historical catches or weights. */
final class JournalBook {
    static final List<String> RARITIES=List.of("common","uncommon","rare","epic","legendary","mythic");
    static final class Catch {
        String species,dim; long count,time; double heaviest; int rarities,x,y,z;
    }
    static final class Kill { String type,dim; long time; int level,x,y,z; }
    final Map<String,Catch> fish=new HashMap<>();
    final Map<String,Kill> kills=new HashMap<>();
    final Map<String,Long> places=new HashMap<>();

    boolean caught(String species,long count,double kg,String rarity,String dim,int x,int y,int z,long time){
        if(count<=0)return false;
        boolean fresh=!fish.containsKey(species);
        var entry=fish.computeIfAbsent(species,id->{var c=new Catch();c.species=id;return c;});
        entry.count=Math.min(Long.MAX_VALUE-count,entry.count)+count;
        if(Double.isFinite(kg)&&kg>0)entry.heaviest=Math.max(entry.heaviest,kg);
        int band=RARITIES.indexOf(rarity);entry.rarities|=1<<Math.max(0,band);
        entry.dim=dim;entry.x=x;entry.y=y;entry.z=z;entry.time=time;
        return fresh;
    }
    boolean killed(String type,int level,String dim,int x,int y,int z,long time){
        boolean fresh=!kills.containsKey(type);
        var entry=kills.computeIfAbsent(type,id->{var k=new Kill();k.type=id;return k;});
        entry.level=Math.max(entry.level,level);entry.time=time;entry.dim=dim;entry.x=x;entry.y=y;entry.z=z;
        return fresh;
    }
    boolean visit(String key,long time){return places.putIfAbsent(key,time)==null;}

    static JournalState page(List<JournalState.Entry> entries,JournalRequest request){
        String query=request.search().strip().toLowerCase(Locale.ROOT);
        var all=entries.stream().filter(e->request.filter()==0||e.kind()==request.filter()||request.filter()==3&&e.kind()==JournalState.FEAT)
            .filter(e->query.isEmpty()||(e.name()+" "+e.by()).toLowerCase(Locale.ROOT).contains(query))
            .sorted(Comparator.comparingLong(JournalState.Entry::time).reversed().thenComparing(JournalState.Entry::key)).toList();
        int pages=Math.max(1,(all.size()+request.size()-1)/request.size()),page=Math.min(request.page(),pages-1),start=page*request.size();
        return new JournalState(request.tab(),request.filter(),page,pages,all.size(),request.size(),request.search(),all.subList(start,Math.min(all.size(),start+request.size())),request.requestId());
    }
}
