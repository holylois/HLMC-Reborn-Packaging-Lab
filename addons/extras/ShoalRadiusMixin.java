package holylois.boombox.mixins;

import com.stevekung.fishofthieves.client.renderer.entity.state.ShoalRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Widen the orbit without scaling every fish a second time; slow its angular speed to retain the swimming pace. */
@Mixin(targets="com.stevekung.fishofthieves.client.renderer.entity.ShoalRenderer")
public abstract class ShoalRadiusMixin {
    private static float radius(ShoalRenderState state){
        float largest=1;
        for(var fish:state.shoalFishClient)if(fish instanceof LivingEntityRenderState living)largest=Math.max(largest,living.scale);
        return holylois.boombox.ShoalFishIdentity.radius(largest,state.shoalFishClient.size());
    }
    @ModifyConstant(method="submit(Lcom/stevekung/fishofthieves/client/renderer/entity/state/ShoalRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",constant=@Constant(floatValue=1f),require=1)
    private float holyLoisOrbit(float value,ShoalRenderState state,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.SubmitNodeCollector collector,net.minecraft.client.renderer.state.level.CameraRenderState camera){return value*radius(state);}
    @ModifyConstant(method="submit(Lcom/stevekung/fishofthieves/client/renderer/entity/state/ShoalRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",constant=@Constant(floatValue=0.25f),require=1)
    private float holyLoisPace(float value,ShoalRenderState state,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.SubmitNodeCollector collector,net.minecraft.client.renderer.state.level.CameraRenderState camera){return value*radius(state);}
}
