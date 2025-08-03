package com.samxel.reducedores;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class OreConfig {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    static {
        final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        COMMON = new Common(builder);
        COMMON_SPEC = builder.build();
    }

    public static class Common {
        public final ForgeConfigSpec.DoubleValue ORE_SIZE_MULTIPLIER;
        public final ForgeConfigSpec.DoubleValue ORE_GENERATION_CHANCE;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> CUSTOM_ORES;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> EXCLUDED_ORES;

        public Common(ForgeConfigSpec.Builder builder) {
            builder.push("ore_generation");

            ORE_SIZE_MULTIPLIER = builder
                    .comment("Multiplier for ore vein size")
                    .defineInRange("ore_size_multiplier", 0.5, 0.01, 1.0);

            ORE_GENERATION_CHANCE = builder
                    .comment("Chance for ore generation")
                    .defineInRange("ore_generation_chance", 0.5, 0.01, 1.0);

            CUSTOM_ORES = builder
                    .comment("List of custom ore names that should be affected (e.g. ore_ancient_debris)",
                            "Format: modid:ore_name or just ore_name for minecraft")
                    .defineList("custom_ores", List.of("ore_ancient_debris"), o -> o instanceof String);

            EXCLUDED_ORES = builder
                    .comment("List of ore names that should be EXCLUDED from reduction.",
                            "Format: modid:ore_name or just ore_name for minecraft")
                    .defineList("excluded_ores", List.of(), o -> o instanceof String);
            builder.pop();
        }
    }
}