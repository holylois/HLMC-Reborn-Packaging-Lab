package holylois.boombox;

/** Size bounds and the different Legendary/Mythic visual contracts, without opening a game. */
public final class SwimmingFishTest {
    public static void main(String[] args){
        net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
        check(SwimmingFish.scale(0.1)<SwimmingFish.scale(1)&&SwimmingFish.scale(1)<SwimmingFish.scale(40),"fish bodies grow with weight");
        check(SwimmingFish.scale(0.01)>=0.65f&&SwimmingFish.scale(1000)<=4.5f&&SwimmingFish.scale(80)-SwimmingFish.scale(20)>0.6,"fish body size is bounded and still grows");
        check(ShoalFishIdentity.radius(4,8)>ShoalFishIdentity.radius(1,3)&&ShoalFishIdentity.radius(4,32)<=3,"large/populated shoals have bounded wider circles");
        var legendary=FishData.shine(net.minecraft.network.chat.Style.EMPTY,"legendary");
        var mythic=FishData.shine(net.minecraft.network.chat.Style.EMPTY,"mythic");
        check(ShineText.colorAt(legendary,5,0xFFFFFFFF,0.25f)==ShineText.colorAt(legendary,5,0xFFFFFFFF,0.75f),"Legendary base gold does not tween outside the glint");
        check(ShineText.colorAt(mythic,5,0xFFFFFFFF,0.25f)!=ShineText.colorAt(mythic,5,0xFFFFFFFF,0.75f),"Mythic retains its moving gradient");
        check(ShineText.colorAt(legendary,50,100,0xFFFFFFFF,1.2f)!=ShineText.colorAt(legendary,5,100,0xFFFFFFFF,1.2f),"Legendary retains its narrow white shine");
        for(var style:java.util.List.of(legendary,mythic)){
            float period=ShineText.kind(style)==1?2.4f:1.6f;
            for(float width:new float[]{32,73,180})for(float glyph:new float[]{0,width-6}){
                float time=(glyph+20)/(width+40)*period;
                int bright=ShineText.colorAt(style,glyph,width,0x7FFFFFFF,time);
                check((bright&0xFF000000)==0x7F000000&&((bright>>16)&255)>220&&((bright>>8)&255)>220,"shine reaches first and last glyph and retains alpha");
            }
        }
        check(FishTraits.seconds("uncommon",0,false,false)==10&&FishTraits.seconds("uncommon",1,true,false)==10,"Uncommon effects have fixed short duration so meals stack");
        check(FishTraits.seconds("uncommon",1,false,true)<FishTraits.seconds("rare",0,false,false),"cooked Uncommon stays weaker than Rare");
        System.out.println("Swimming fish, meals and shine: 20 checks passed");
    }
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
}
