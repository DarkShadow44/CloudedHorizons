package com.cloudedhorizons.mixin;

import net.minecraft.client.entity.EntityPlayerSP;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Dev hack: vanilla creative flight rises and sinks at a fixed 0.15 per tick regardless of fly speed. This scales that
 * impulse by the fly speed relative to the default 0.05, so faster flight also climbs and descends faster.
 */
@Mixin(EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP {

    private static final float DEFAULT_FLY_SPEED = 0.05F;

    @ModifyConstant(method = "onLivingUpdate", constant = @Constant(doubleValue = 0.15D))
    private double cloudedhorizons$scaleVerticalFlight(double impulse) {
        float flySpeed = ((EntityPlayerSP) (Object) this).capabilities.getFlySpeed();
        return impulse * (flySpeed / DEFAULT_FLY_SPEED);
    }
}
