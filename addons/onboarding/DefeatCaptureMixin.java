package holylois.mixins;

import holylois.DefeatCapture;
import holylois.boombox.DefeatRecord;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class DefeatCaptureMixin implements DefeatRecord.Memory {
    @Unique private DefeatRecord holyLoisRecord;
    public DefeatRecord holyLoisDefeat(){return holyLoisRecord;}
    public void holyLoisDefeat(DefeatRecord record){holyLoisRecord=record;}
    @Inject(method="tick",at=@At("HEAD"),require=1)
    private void holyLoisForgetRevival(CallbackInfo info){if(holyLoisRecord!=null&&((LivingEntity)(Object)this).isAlive())holyLoisRecord=null;}
    @Inject(method="die",at=@At("HEAD"),require=1)
    private void holyLoisBeforeDeath(DamageSource source,CallbackInfo info){
        var entity=(LivingEntity)(Object)this;
        if(entity.level() instanceof net.minecraft.server.level.ServerLevel&&!(entity instanceof net.minecraft.server.level.ServerPlayer)&&entity.getKillCredit() instanceof net.minecraft.server.level.ServerPlayer)
            holyLoisRecord=DefeatCapture.capture(entity);
    }
}
