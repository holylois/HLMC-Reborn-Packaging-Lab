package holylois.boombox;

import java.util.List;
import java.util.ArrayList;
import net.minecraft.resources.Identifier;

/** Impossible criteria are awarded only by the actual fishing event, never inventory changes. */
public final class FishMilestones {
    private FishMilestones() {}
    static void register(){FishCaught.EVENT.register((player,fish)->{
        for(String id:qualified(FishData.rarity(fish),FishData.shiny(fish),FishData.fillet(fish))){
            var advancement=player.level().getServer().getAdvancements().get(Identifier.parse("holylois:"+id));
            if(advancement!=null)player.getAdvancements().award(advancement,"caught");
        }
    });}
    public static List<String> qualified(String rarity,boolean shiny,boolean fillet){
        if(fillet)return List.of();var result=new ArrayList<String>();
        if(rarity.equals("mythic"))result.add("fishing/mythic");
        if(shiny&&rarity.equals("legendary"))result.add("fishing/shiny_legendary");
        if(shiny&&rarity.equals("mythic"))result.add("fishing/shiny_mythic");
        return List.copyOf(result);
    }
}
