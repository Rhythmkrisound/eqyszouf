package com.vladik.godbox.mixin.client;

import com.vladik.godbox.client.SeasonClient;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Сезонная окраска травы, листвы и воды. Через эти три метода идут и ванильный BiomeColors,
 * и размытие цвета биомов в Sodium, поэтому хук работает с обоими.
 */
@Mixin(Biome.class)
public abstract class BiomeSeasonMixin {
    @Inject(method = "getGrassColorAt(DD)I", at = @At("RETURN"), cancellable = true)
    private void godbox$grass(double x, double z, CallbackInfoReturnable<Integer> cir) {
        if (SeasonClient.tintActive()) cir.setReturnValue(SeasonClient.grass(cir.getReturnValueI(), ((Biome) (Object) this).getTemperature()));
    }

    @Inject(method = "getFoliageColor()I", at = @At("RETURN"), cancellable = true)
    private void godbox$foliage(CallbackInfoReturnable<Integer> cir) {
        if (SeasonClient.tintActive()) cir.setReturnValue(SeasonClient.foliage(cir.getReturnValueI(), ((Biome) (Object) this).getTemperature()));
    }

    @Inject(method = "getWaterColor()I", at = @At("RETURN"), cancellable = true)
    private void godbox$water(CallbackInfoReturnable<Integer> cir) {
        if (SeasonClient.tintActive()) cir.setReturnValue(SeasonClient.water(cir.getReturnValueI(), ((Biome) (Object) this).getTemperature()));
    }
}
