package uz.dexvisual.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.render.GameRenderer;
import uz.dexvisual.Mods;
import uz.dexvisual.Proj;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true, require = 0)
    private void dex$noHurtCam(CallbackInfo ci) {
        if (Mods.NO_HURT.enabled) ci.cancel();
    }

    /** Remember the real world FOV (includes sprint/effects) so ESP lines up exactly. */
    @Inject(method = "getFov", at = @At("RETURN"), require = 0)
    private void dex$captureFov(CallbackInfoReturnable<Float> cir) {
        if (Proj.awaitFov) {
            Proj.capturedFov = cir.getReturnValue();
            Proj.awaitFov = false;
        }
    }
}
