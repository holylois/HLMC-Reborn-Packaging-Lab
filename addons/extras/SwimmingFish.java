package holylois.boombox;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** One persistent catch identity per living fish, also carried by FoT's saved virtual shoal fish. */
public final class SwimmingFish {
    public static final String SAVE_KEY="holylois_swimming_fish", SCALE_KEY="holylois_swim_scale";
    private static final Identifier SCALE=Identifier.parse("holylois:fish_size");
    public interface Memory {
        ItemStack holyLoisFish();
        void holyLoisFish(ItemStack stack);
    }
    private SwimmingFish() {}

    public static ItemStack identity(AbstractFish fish){
        var memory=(Memory)fish;var stack=memory.holyLoisFish();
        if(stack.isEmpty()&&fish.level() instanceof ServerLevel level&&Legends.fish!=null&&Legends.fish.enabled){
            stack=SchoolCatch.rawItemFor(fish);
            if(!stack.isEmpty()){
                Legends.weighSwimming(stack,level,fish.getRandom(),Legends.fish);
                memory.holyLoisFish(stack);applyScale(fish,stack);
            }
        }
        return stack;
    }
    public static float scale(double kg){return (float)Math.clamp(0.55+0.55*Math.sqrt(Math.max(0,kg)),0.65,4.0);}
    public static void applyScale(AbstractFish fish,ItemStack stack){
        double kg=FishData.kilograms(stack);var attribute=fish.getAttribute(Attributes.SCALE);
        if(kg>0&&attribute!=null)attribute.addOrUpdateTransientModifier(new AttributeModifier(SCALE,scale(kg)-1,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
    public static ItemStack valid(AbstractFish fish,ItemStack stack){
        var raw=SchoolCatch.rawItemFor(fish);
        double kg=FishData.kilograms(stack);
        return !stack.isEmpty()&&stack.getCount()==1&&stack.is(raw.getItem())&&Double.isFinite(kg)&&kg>0&&kg<=200
            &&java.util.List.of("common","uncommon","rare","epic","legendary","mythic").contains(FishData.rarity(stack))?stack:ItemStack.EMPTY;
    }
}
