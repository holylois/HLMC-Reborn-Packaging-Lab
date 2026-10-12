package holylois.boombox;

/** Size bounds and the different Legendary/Mythic visual contracts, without opening a game. */
public final class SwimmingFishTest {
    public static void main(String[] args){
        net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
        check(SwimmingFish.scale(0.1)<SwimmingFish.scale(1)&&SwimmingFish.scale(1)<SwimmingFish.scale(40),"fish bodies grow with weight");
        check(SwimmingFish.scale(0.01)>=0.65f&&SwimmingFish.scale(200)<=4,"fish body size is bounded");
        check(ShoalFishIdentity.radius(4,8)>ShoalFishIdentity.radius(1,3)&&ShoalFishIdentity.radius(4,32)<=3,"large/populated shoals have bounded wider circles");
        var legendary=FishData.shine(net.minecraft.network.chat.Style.EMPTY,"legendary");
        var mythic=FishData.shine(net.minecraft.network.chat.Style.EMPTY,"mythic");
        check(ShineText.colorAt(legendary,5,0xFFFFFFFF,0.25f)==ShineText.colorAt(legendary,5,0xFFFFFFFF,0.75f),"Legendary base gold does not tween outside the glint");
        check(ShineText.colorAt(mythic,5,0xFFFFFFFF,0.25f)!=ShineText.colorAt(mythic,5,0xFFFFFFFF,0.75f),"Mythic retains its moving gradient");
        check(ShineText.colorAt(legendary,53,0xFFFFFFFF,0.75f)!=ShineText.colorAt(legendary,5,0xFFFFFFFF,0.75f),"Legendary retains its narrow white shine");
        System.out.println("Swimming fish and shine: 6 checks passed");
    }
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
}
