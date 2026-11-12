package com.vladik.godbox.mixin;

import com.vladik.godbox.Seasons;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Времена года для всех посевов: зимой растут медленно, весной — быстрее (в жарких биомах без изменений). */
@Mixin(CropBlock.class)
public abstract class CropSeasonMixin {
    @Inject(method = "randomTick(Lnet/minecraft/block/BlockState;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/random/Random;)V",
            at = @At("HEAD"), cancellable = true)
    private void godbox$season(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        if (!Seasons.cropsOn()) return;
        int se = Seasons.index();
        if (se != Seasons.WINTER && se != Seasons.SPRING) return;
        if (world.getBiome(pos).value().getTemperature() >= 0.95f) return;
        CropBlock self = (CropBlock) (Object) this;
        if (se == Seasons.WINTER) {
            if (random.nextInt(100) < 75) ci.cancel();
        } else if (random.nextInt(100) < 30 && !self.isMature(state) && world.getBaseLightLevel(pos, 0) >= 9) {
            world.setBlockState(pos, self.withAge(self.getAge(state) + 1), 2);
        }
    }
}
