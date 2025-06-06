package com.samxel.reducedores;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

@Mod(ReducedOres.MODID)
public class ReducedOres {
    public static final String MODID = "reducedores";
    public static final Logger LOGGER = LoggerFactory.getLogger("ReducedOres");

    public ReducedOres() {
        ModLoadingContext.get().registerConfig(
                ModConfig.Type.COMMON,
                OreConfig.COMMON_SPEC
        );
    }
}