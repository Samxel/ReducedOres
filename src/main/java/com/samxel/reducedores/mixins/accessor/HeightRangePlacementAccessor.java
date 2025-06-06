package com.samxel.reducedores.mixins.accessor;

import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HeightRangePlacement.class)
public interface HeightRangePlacementAccessor {
    @Accessor("height")
    HeightProvider getHeight();
}