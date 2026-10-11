package holylois.boombox;

import java.util.UUID;

public final class JournalRequestQueueTest {
    public static void main(String[] args){
        var queue=new JournalRequestQueue();var alice=UUID.randomUUID();var bob=UUID.randomUUID();
        var initial=new JournalRequest(1,0,0,6,"");var resized=new JournalRequest(1,0,0,8,"");
        check(queue.submit(alice,initial,1000),"initial page is immediate");
        check(!queue.submit(alice,resized,1001)&&queue.drain(1149).isEmpty(),"resize waits for the rate limit");
        check(queue.drain(1150).get(alice).equals(resized),"resize response is eventually delivered");
        for(int i=0;i<100;i++)queue.submit(alice,new JournalRequest(1,1,0,8,"cod"+i),1151+i);
        var ready=queue.drain(1300);
        check(ready.size()==1&&ready.get(alice).search().equals("cod99"),"burst keeps only the latest request");
        check(queue.submit(bob,initial,1300),"viewers have independent limits");
        queue.submit(bob,resized,1301);queue.forget(bob);
        check(!queue.drain(1500).containsKey(bob),"disconnect drops pending work");
        queue.clear();check(queue.submit(alice,initial,0),"new server clears old limits");
        System.out.println("Journal request coalescing: 7 checks passed");
    }
    private static void check(boolean okay,String message){if(!okay)throw new AssertionError(message);}
}
