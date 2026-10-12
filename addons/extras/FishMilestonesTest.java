package holylois.boombox;

public final class FishMilestonesTest {
    public static void main(String[] args){
        for(String rarity:java.util.List.of("common","uncommon","rare","epic","legendary","mythic"))for(boolean shiny:new boolean[]{false,true}){
            var awards=FishMilestones.qualified(rarity,shiny,false);
            if(awards.contains("fishing/mythic")!=rarity.equals("mythic")||awards.contains("fishing/shiny_legendary")!=(shiny&&rarity.equals("legendary"))||awards.contains("fishing/shiny_mythic")!=(shiny&&rarity.equals("mythic")))throw new AssertionError("Wrong catch milestone eligibility");
            if(!FishMilestones.qualified(rarity,shiny,true).isEmpty())throw new AssertionError("Fillets must not count as caught fish");
            if(awards.contains("fishing/the_big_one"))throw new AssertionError("Existing Legendary achievement must not be duplicated");
        }
        System.out.println("Catch milestones: all rarity/Shiny/fillet combinations passed");
    }
}
