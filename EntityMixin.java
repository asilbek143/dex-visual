package uz.dexvisual.mixin;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import uz.dexvisual.DexVisual;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    private void dex$glow(CallbackInfoReturnable<Boolean> cir) {
        if (DexVisual.glow((Entity) (Object) this)) cir.setReturnValue(true);
    }

    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true, require = 0)
    private void dex$color(CallbackInfoReturnable<Integer> cir) {
        int c = DexVisual.glowColor((Entity) (Object) this);
        if (c != -1) cir.setReturnValue(c);
    }
}
