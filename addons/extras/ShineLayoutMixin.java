package holylois.boombox.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import holylois.boombox.ShineText;
import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import java.util.ArrayList;

/** Measure contiguous shine-font runs, including spaces and bold glyph advances, before coloring them. */
@Mixin(Font.class)
public abstract class ShineLayoutMixin {
    @WrapOperation(method="prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;",
        at=@At(value="INVOKE",target="Lnet/minecraft/util/FormattedCharSequence;accept(Lnet/minecraft/util/FormattedCharSink;)Z"),require=1)
    private boolean holyLoisMeasure(FormattedCharSequence sequence,FormattedCharSink sink,Operation<Boolean> original){
        boolean[] shining={false};sequence.accept((index,style,cp)->{shining[0]|=ShineText.kind(style)!=0;return true;});
        if(shining[0]&&sink instanceof ShineText.Layout layout){
            var font=(Font)(Object)this;var runs=new ArrayList<ShineText.Run>();
            float[] cursor={0};
            sequence.accept((index,style,cp)->{
                float start=cursor[0];
                cursor[0]+=font.getSplitter().stringWidth(FormattedCharSequence.forward(new String(Character.toChars(cp)),style));
                int kind=ShineText.kind(style);
                if(kind!=0){
                    if(!runs.isEmpty()&&runs.getLast().end()==start&&runs.getLast().kind()==kind){
                        var previous=runs.removeLast();runs.add(new ShineText.Run(previous.start(),cursor[0],kind));
                    }else runs.add(new ShineText.Run(start,cursor[0],kind));
                }
                return true;
            });
            layout.holyLoisRuns(runs);
        }
        return original.call(sequence,sink);
    }
}
