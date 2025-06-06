package com.samxel.reducedores.mixins;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.samxel.reducedores.OreConfig;
import net.minecraft.resources.RegistryDataLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.samxel.reducedores.ReducedOres;

import java.util.Locale;
import java.util.Objects;

@Mixin(value = RegistryDataLoader.class, priority = 1001)
public class RegistryDataLoaderMixin {
    private static boolean configLoaded = false;
    private static boolean configAccessAttempted = false;

    @ModifyExpressionValue(
            method = "loadRegistryContents(Lnet/minecraft/resources/RegistryOps$RegistryInfoLookup;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/core/WritableRegistry;Lcom/mojang/serialization/Decoder;Ljava/util/Map;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/google/gson/JsonParser;parseReader(Ljava/io/Reader;)Lcom/google/gson/JsonElement;"
            )
    )
    private static JsonElement modifyOreJson(JsonElement original) {
        if (!configAccessAttempted) {
            try {
                double sizeMultiplier = OreConfig.COMMON.ORE_SIZE_MULTIPLIER.get();
                double generationChance = OreConfig.COMMON.ORE_GENERATION_CHANCE.get();
                configLoaded = true;
                ReducedOres.LOGGER.info(
                        "[ReducedOres] Configuration loaded: Size x{} | Chance x{}",
                        sizeMultiplier, generationChance
                );
            } catch (Exception e) {
                ReducedOres.LOGGER.error(
                        "[ReducedOres] Error loading configuration.", e
                );
                configLoaded = false;
            }
            configAccessAttempted = true;
        }

        if (!configLoaded) {
            ReducedOres.LOGGER.warn(
                    "[ReducedOres] Ore modification skipped, no configuration available."
            );
            return original;
        }

        try {
            if (original == null || !original.isJsonObject()) {
                return original;
            }

            JsonObject json = original.getAsJsonObject();
            ReducedOres.LOGGER.debug(
                    "[ReducedOres] Processing JSON: {}", json
            );

            processOreFeatures(json);
            processPlacedOreFeatures(json);

            ReducedOres.LOGGER.debug(
                    "[ReducedOres] JSON processed: {}", json
            );
        } catch (Exception e) {
            ReducedOres.LOGGER.error(
                    "[ReducedOres] Error processing ore JSON.", e
            );
        }
        return original;
    }

    private static void processOreFeatures(JsonObject json) {
        try {
            if (!json.has("type") || !json.get("type").isJsonPrimitive()) {
                return;
            }

            String type = json.get("type").getAsString();
            if (isOreType(type)) {
                if (json.has("config") && json.get("config").isJsonObject()) {
                    JsonObject config = json.getAsJsonObject("config");
                    if (config.has("size") && config.get("size").isJsonPrimitive()) {
                        int originalSize = safeGetInt(config, "size", 1);
                        int newSize = calculateNewSize(originalSize);
                        if (newSize < 1) newSize = 1;
                        config.addProperty("size", newSize);
                        ReducedOres.LOGGER.info(
                                "[ReducedOres] Ore size changed: {} -> {} (Type: {})",
                                originalSize, newSize, type
                        );
                    }
                }
            }
        } catch (Exception e) {
            ReducedOres.LOGGER.warn(
                    "[ReducedOres] Error adjusting ore size.", e
            );
        }
    }

    private static void processPlacedOreFeatures(JsonObject json) {
        try {
            if (!json.has("feature") || !json.get("feature").isJsonPrimitive()
                    || !json.has("placement") || !json.get("placement").isJsonArray()) {
                return;
            }
            String featureName = json.get("feature").getAsString().toLowerCase(Locale.ROOT);
            if (!isFeatureOre(featureName)) {
                return;
            }
            boolean hasRarity = processRarityFilters(json, featureName);
            if (!hasRarity) {
                processCountPlacements(json, featureName);
            }
        } catch (Exception e) {
            try {
                String feature = json.has("feature") ? json.get("feature").getAsString() : "unknown";
                ReducedOres.LOGGER.warn(
                        "[ReducedOres] Error processing feature '{}'.", feature, e
                );
            } catch (Exception ex) {
                ReducedOres.LOGGER.warn(
                        "[ReducedOres] Error processing feature (unknown name).", e
                );
            }
        }
    }

    private static boolean processRarityFilters(JsonObject json, String featureName) {
        boolean hasRarity = false;
        for (JsonElement element : json.getAsJsonArray("placement")) {
            if (!element.isJsonObject()) continue;
            JsonObject placement = element.getAsJsonObject();
            if (placement.has("type")
                    && "minecraft:rarity_filter".equals(placement.get("type").getAsString())
                    && placement.has("chance")
                    && placement.get("chance").isJsonPrimitive()) {
                try {
                    int originalChance = safeGetInt(placement, "chance", 1);
                    int newChance = calculateNewRarity(originalChance);
                    if (newChance < 1) newChance = 1;
                    placement.addProperty("chance", newChance);
                    ReducedOres.LOGGER.info(
                            "[ReducedOres] Rarity changed: {} -> {} for {}",
                            originalChance, newChance, featureName
                    );
                    hasRarity = true;
                } catch (Exception e) {
                    ReducedOres.LOGGER.warn(
                            "[ReducedOres] Invalid rarity value in {}.", featureName, e
                    );
                }
            }
        }
        return hasRarity;
    }

    private static void processCountPlacements(JsonObject json, String featureName) {
        for (JsonElement element : json.getAsJsonArray("placement")) {
            if (!element.isJsonObject()) continue;
            JsonObject placement = element.getAsJsonObject();
            if (placement.has("type")
                    && "minecraft:count".equals(placement.get("type").getAsString())
                    && placement.has("count")
                    && placement.get("count").isJsonPrimitive()) {
                try {
                    int originalCount = safeGetInt(placement, "count", 1);
                    int newCount = calculateNewCount(originalCount);
                    if (newCount < 1) newCount = 1;
                    placement.addProperty("count", newCount);
                    ReducedOres.LOGGER.info(
                            "[ReducedOres] Placement count changed: {} -> {} for {}",
                            originalCount, newCount, featureName
                    );
                } catch (Exception e) {
                    ReducedOres.LOGGER.warn(
                            "[ReducedOres] Invalid count value in {}.", featureName, e
                    );
                }
            }
        }
    }

    private static boolean isFeatureOre(String featureName) {
        if (featureName == null || featureName.isEmpty()) {
            return false;
        }
        if (isExcludedOre(featureName)) {
            ReducedOres.LOGGER.debug(
                    "[ReducedOres] Ore excluded by configuration: {}", featureName
            );
            return false;
        }
        String[] parts = featureName.split("[:/]", 2);
        String lastPart = parts.length > 0 ? parts[parts.length - 1] : featureName;
        lastPart = lastPart.toLowerCase(Locale.ROOT);
        boolean isStandardOre = lastPart.endsWith("_ore")
                || lastPart.startsWith("ore_")
                || lastPart.contains("_ore_");
        String finalLastPart = lastPart;
        boolean isCustomOre = OreConfig.COMMON.CUSTOM_ORES.get().stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .anyMatch(customOre -> {
                    if (customOre.contains(":")) {
                        String[] cuParts = customOre.split(":", 2);
                        String cuLast = cuParts.length > 1
                                ? cuParts[1].toLowerCase(Locale.ROOT)
                                : customOre.toLowerCase(Locale.ROOT);
                        return featureName.equalsIgnoreCase(customOre)
                                || featureName.endsWith(":" + cuLast)
                                || finalLastPart.startsWith(cuLast);
                    }
                    String cuLower = customOre.toLowerCase(Locale.ROOT);
                    return finalLastPart.equalsIgnoreCase(cuLower)
                            || finalLastPart.startsWith(cuLower);
                });
        if (isCustomOre) {
            ReducedOres.LOGGER.debug(
                    "[ReducedOres] Custom ore detected: {}", featureName
            );
        }
        return isStandardOre || isCustomOre;
    }

    private static boolean isOreType(String type) {
        if (type == null || type.isEmpty()) {
            return false;
        }
        String[] parts = type.split("[:/]", 2);
        String lastPart = parts.length > 0 ? parts[parts.length - 1] : type;
        lastPart = lastPart.toLowerCase(Locale.ROOT);
        return lastPart.equals("ore")
                || lastPart.endsWith("_ore")
                || lastPart.startsWith("ore_")
                || lastPart.contains("_ore_");
    }

    private static int calculateNewSize(int originalSize) {
        try {
            double multiplier = OreConfig.COMMON.ORE_SIZE_MULTIPLIER.get();
            return Math.max(1, (int) Math.round(originalSize * multiplier));
        } catch (Exception e) {
            ReducedOres.LOGGER.warn(
                    "[ReducedOres] Error calculating size, using original value.", e
            );
            return originalSize;
        }
    }

    private static int calculateNewCount(int originalCount) {
        try {
            double chance = OreConfig.COMMON.ORE_GENERATION_CHANCE.get();
            ReducedOres.LOGGER.info(
                    "[ReducedOres] Debug: originalCount={}, chance={}", originalCount, chance
            );
            int result = Math.max(1, (int) Math.round(originalCount * chance));
            ReducedOres.LOGGER.info(
                    "[ReducedOres] Debug: Result for count: {}", result
            );
            return result;
        } catch (Exception e) {
            ReducedOres.LOGGER.warn(
                    "[ReducedOres] Error calculating count, using original value.", e
            );
            return originalCount;
        }
    }

    private static int calculateNewRarity(int originalRarity) {
        try {
            double chance = OreConfig.COMMON.ORE_GENERATION_CHANCE.get();
            return Math.max(1, (int) Math.round(originalRarity / chance));
        } catch (Exception e) {
            ReducedOres.LOGGER.warn(
                    "[ReducedOres] Error calculating rarity, using original value.", e
            );
            return originalRarity;
        }
    }

    private static boolean isExcludedOre(String featureName) {
        if (featureName == null || featureName.isEmpty()) return false;
        String[] parts = featureName.split("[:/]", 2);
        String lastPart = parts.length > 0 ? parts[parts.length - 1] : featureName;
        lastPart = lastPart.toLowerCase(Locale.ROOT);
        String finalLastPart = lastPart;
        return OreConfig.COMMON.EXCLUDED_ORES.get().stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .anyMatch(excludedOre -> {
                    if (excludedOre.contains(":")) {
                        String[] exParts = excludedOre.split(":", 2);
                        String exLast = exParts.length > 1
                                ? exParts[1].toLowerCase(Locale.ROOT)
                                : excludedOre.toLowerCase(Locale.ROOT);
                        return featureName.equalsIgnoreCase(excludedOre)
                                || featureName.endsWith(":" + exLast)
                                || finalLastPart.startsWith(exLast);
                    }
                    String exLower = excludedOre.toLowerCase(Locale.ROOT);
                    return finalLastPart.equalsIgnoreCase(exLower)
                            || finalLastPart.startsWith(exLower);
                });
    }

    private static int safeGetInt(JsonObject obj, String key, int fallback) {
        try {
            if (obj.has(key)
                    && obj.get(key).isJsonPrimitive()
                    && obj.get(key).getAsJsonPrimitive().isNumber()) {
                return obj.get(key).getAsInt();
            }
        } catch (Exception e) {
            ReducedOres.LOGGER.warn(
                    "[ReducedOres] Error parsing '{}' in {}.", key, obj, e
            );
        }
        return fallback;
    }
}