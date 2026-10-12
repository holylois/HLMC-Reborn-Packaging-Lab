package holylois.boombox.mixins;

import holylois.boombox.SwimmingFish;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFish.class)
public abstract class SwimmingFishMixin implements SwimmingFish.Memory {
    @Unique private ItemStack holyLoisIdentity=ItemStack.EMPTY;
    @Override public ItemStack holyLoisFish(){return holyLoisIdentity;}
    @Override public void holyLoisFish(ItemStack stack){holyLoisIdentity=stack.copy();}
    @Inject(method="aiStep",at=@At("HEAD"),require=1)
    private void holyLoisInitialize(CallbackInfo info){SwimmingFish.identity((AbstractFish)(Object)this);}
    @Inject(method="addAdditionalSaveData",at=@At("TAIL"),require=1)
    private void holyLoisSave(ValueOutput output,CallbackInfo info){
        var fish=(AbstractFish)(Object)this;var stack=SwimmingFish.identity(fish);
        if(!stack.isEmpty()){
            output.store(SwimmingFish.SAVE_KEY,ItemStack.CODEC,stack);
            output.putDouble(SwimmingFish.SCALE_KEY,SwimmingFish.scale(SwimmingFish.mass(stack)));
        }
    }
    @Inject(method="readAdditionalSaveData",at=@At("TAIL"),require=1)
    private void holyLoisRead(ValueInput input,CallbackInfo info){
        var fish=(AbstractFish)(Object)this;
        holyLoisIdentity=SwimmingFish.valid(fish,input.read(SwimmingFish.SAVE_KEY,ItemStack.CODEC).orElse(ItemStack.EMPTY));
        if(!holyLoisIdentity.isEmpty())SwimmingFish.applyScale(fish,holyLoisIdentity);
    }
    @Inject(method="saveToBucketTag",at=@At("TAIL"),require=1)
    private void holyLoisBucketSave(ItemStack bucket,CallbackInfo info){
        if(!(bucket.getItem() instanceof net.minecraft.world.item.MobBucketItem))return;
        var fish=(AbstractFish)(Object)this;var stack=SwimmingFish.identity(fish);if(stack.isEmpty())return;
        var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,fish.registryAccess());
        output.store(SwimmingFish.SAVE_KEY,ItemStack.CODEC,stack);
        var data=bucket.getOrDefault(net.minecraft.core.component.DataComponents.BUCKET_ENTITY_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        data.merge(output.buildResult());net.minecraft.world.item.component.CustomData.set(net.minecraft.core.component.DataComponents.BUCKET_ENTITY_DATA,bucket,data);
    }
    @Inject(method="loadFromBucketTag",at=@At("TAIL"),require=1)
    private void holyLoisBucketLoad(net.minecraft.nbt.CompoundTag data,CallbackInfo info){
        var fish=(AbstractFish)(Object)this;var saved=data.get(SwimmingFish.SAVE_KEY);if(saved==null)return;
        holyLoisIdentity=SwimmingFish.valid(fish,ItemStack.CODEC.parse(fish.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),saved).result().orElse(ItemStack.EMPTY));
        if(!holyLoisIdentity.isEmpty())SwimmingFish.applyScale(fish,holyLoisIdentity);
    }
}
