package holylois.boombox.mixins;

import com.stevekung.fishofthieves.entity.shoal.Shoal;
import holylois.boombox.ShoalFishIdentity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.level.storage.ValueInput;

@Mixin(Shoal.class)
public abstract class ShoalIdentityMixin {
    @Inject(method="createNaturalSpawn",at=@At("TAIL"),require=1)
    private void holyLoisNatural(boolean expired,CallbackInfo info){var shoal=(Shoal)(Object)this;ShoalFishIdentity.addVanilla(shoal);ShoalFishIdentity.prepare(shoal);}
    @Inject(method="createTreasuredSpawn",at=@At("TAIL"),require=1)
    private void holyLoisTreasured(int tier,CallbackInfo info){ShoalFishIdentity.prepare((Shoal)(Object)this);}
    @Inject(method="readAdditionalSaveData",at=@At("TAIL"),require=1)
    private void holyLoisMigrate(ValueInput input,CallbackInfo info){
        var shoal=(Shoal)(Object)this;
        if(!shoal.level().isClientSide()){
            if(!shoal.isTreasured())ShoalFishIdentity.addVanilla(shoal);
            ShoalFishIdentity.prepare(shoal);com.stevekung.fishofthieves.FOTPlatform.syncClientShoalFish(shoal,false);
        }
    }
    @Inject(method="getRandomFishInShoal",at=@At("HEAD"),require=1)
    private void holyLoisBeforeCatch(CallbackInfoReturnable<net.minecraft.world.entity.LivingEntity> info){ShoalFishIdentity.prepare((Shoal)(Object)this);}
    @ModifyExpressionValue(method="tick",at=@At(value="INVOKE",target="Lcom/stevekung/fishofthieves/entity/shoal/Shoal;getBbWidth()F"),require=1)
    private float holyLoisBubbleRadius(float width){return width*ShoalFishIdentity.radius((Shoal)(Object)this);}
}
