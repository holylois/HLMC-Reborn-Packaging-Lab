package holylois.boombox.mixins;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Journal previews: Fresh Animations asks the entity whether it is in water, so a preview fish must say yes or it flops. */
@Mixin(Entity.class)
public interface EntityWaterAccessor {
    @Accessor("wasTouchingWater")
    void holyLois$setInWater(boolean value);
}
