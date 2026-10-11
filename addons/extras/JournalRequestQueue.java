package holylois.boombox;

import java.util.*;

/** One pending page per viewer. A resize/search burst replaces pending work instead of losing its response. */
public final class JournalRequestQueue {
    private final Map<UUID,Long> next=new HashMap<>();
    private final Map<UUID,JournalRequest> pending=new HashMap<>();
    public boolean submit(UUID viewer,JournalRequest request,long now){
        if(now<next.getOrDefault(viewer,0L)){pending.put(viewer,request);return false;}
        pending.remove(viewer);next.put(viewer,now+150);return true;
    }
    public Map<UUID,JournalRequest> drain(long now){
        var ready=new HashMap<UUID,JournalRequest>();
        for(var entry:pending.entrySet())if(now>=next.getOrDefault(entry.getKey(),0L))ready.put(entry.getKey(),entry.getValue());
        for(var viewer:ready.keySet()){pending.remove(viewer);next.put(viewer,now+150);}
        return ready;
    }
    public void forget(UUID viewer){next.remove(viewer);pending.remove(viewer);}
    public void clear(){next.clear();pending.clear();}
}
