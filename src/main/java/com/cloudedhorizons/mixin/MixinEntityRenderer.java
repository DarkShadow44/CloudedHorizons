package com.cloudedhorizons.mixin;

import net.minecraft.client.renderer.EntityRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.cloudedhorizons.Config;
import com.cloudedhorizons.client.CloudRenderer;

/**
 * Pushes the world projection's far plane out to cover the cloud field, which reaches far beyond the render
 * distance. Only the projection argument changes: {@code farPlaneDistance} itself also drives fog, so it stays as is.
 * The clouds are drawn with this projection and depth-tested against the terrain, so both must use the same far plane.
 */
@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {

    @ModifyArg(
            method = "setupCameraTransform",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V"),
            index = 3)
    private float cloudedhorizons$extendFarPlane(float zFar) {
        return Config.enabled ? Math.max(zFar, CloudRenderer.getFarPlane()) : zFar;
    }
}
