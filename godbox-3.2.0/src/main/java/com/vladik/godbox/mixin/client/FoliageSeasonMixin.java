package com.vladik.godbox.mixin.client;

import com.vladik.godbox.client.SeasonClient;
import net.minecraft.world.biome.FoliageColors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Берёза и ель красятся фиксированными цветами, а не биомом: осенью берёза золотая, зимой обе бледнеют. */
@Mixin(FoliageColors.class)
public abstract class FoliageSeasonMixin {
    @Inject(method = "getBirchColor()I", at = @At("RETURN"), cancellable = true)
    private static void godbox$birch(CallbackInfoReturnable<Integer> cir) {
        if (SeasonClient.tintActive()) cir.setReturnValue(SeasonClient.birch(cir.getReturnValueI()));
    }

    @Inject(method = "getSpruceColor()I", at = @At("RETURN"), cancellable = true)
    private static void godbox$spruce(CallbackInfoReturnable<Integer> cir) {
        if (SeasonClient.tintActive()) cir.setReturnValue(SeasonClient.spruce(cir.getReturnValueI()));
    }
}
