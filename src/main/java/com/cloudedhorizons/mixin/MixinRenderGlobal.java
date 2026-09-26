package com.cloudedhorizons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderGlobal;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.cloudedhorizons.Config;
import com.cloudedhorizons.client.CloudRenderer;

/** Replaces vanilla cloud rendering with the Clouded Horizons renderer in surface worlds. */
@Mixin(RenderGlobal.class)
public abstract class MixinRenderGlobal {

    @Shadow
    private WorldClient theWorld;

    @Shadow
    private Minecraft mc;

    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    private void cloudedhorizons$renderClouds(float partialTicks, CallbackInfo ci) {
        if (!Config.enabled || theWorld == null || !theWorld.provider.isSurfaceWorld()) {
            return;
        }
        CloudRenderer.render(mc, theWorld, partialTicks);
        ci.cancel();
    }
}
