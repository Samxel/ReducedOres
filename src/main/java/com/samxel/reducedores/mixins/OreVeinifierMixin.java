package com.samxel.reducedores.mixins;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.OreVeinifier;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(OreVeinifier.class)
public class OreVeinifierMixin {
    @Inject(method = "create", at = @At("HEAD"), cancellable = true)
    private static void disableVeins(DensityFunction a, DensityFunction b, DensityFunction c, PositionalRandomFactory d, CallbackInfoReturnable<NoiseChunk.BlockStateFiller> cir) {
        cir.setReturnValue((pos) -> null);
    }
}