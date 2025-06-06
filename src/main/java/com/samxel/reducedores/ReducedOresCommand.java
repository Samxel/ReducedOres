package com.samxel.reducedores;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = ReducedOres.MODID)
public class ReducedOresCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("reduceores")
                        .requires(source -> source.hasPermission(2))
                        .then(
                                Commands.literal("info")
                                        .then(
                                                Commands.argument("blockstate", StringArgumentType.greedyString())
                                                        .suggests((context, builder) -> suggestOreBlockStates(context.getSource(), builder))
                                                        .executes(ctx -> {
                                                            String blockState = StringArgumentType.getString(ctx, "blockstate")
                                                                    .toLowerCase(Locale.ROOT);

                                                            Registry<PlacedFeature> placedFeatureRegistry = ctx.getSource()
                                                                    .getLevel()
                                                                    .registryAccess()
                                                                    .registryOrThrow(Registries.PLACED_FEATURE);

                                                            List<ResourceLocation> foundFeatures = placedFeatureRegistry.keySet().stream()
                                                                    .filter(key -> placedFeatureHasBlockState(
                                                                            placedFeatureRegistry.get(key), blockState))
                                                                    .collect(Collectors.toList());

                                                            if (!foundFeatures.isEmpty()) {
                                                                ctx.getSource().sendSuccess(
                                                                        () -> Component.literal("-------------------------------------------------")
                                                                                .withStyle(ChatFormatting.GOLD), false
                                                                );
                                                                for (ResourceLocation key : foundFeatures) {
                                                                    var placed = placedFeatureRegistry.get(key);
                                                                    var configuredHolder = placed.feature();
                                                                    var configured = configuredHolder.value();

                                                                    ctx.getSource().sendSuccess(
                                                                            () -> Component.literal("PlacedFeature: ")
                                                                                    .withStyle(ChatFormatting.AQUA)
                                                                                    .append(Component.literal(key.toString())
                                                                                            .withStyle(ChatFormatting.YELLOW)),
                                                                            false
                                                                    );
                                                                    ctx.getSource().sendSuccess(
                                                                            () -> Component.literal("  Feature Type: ")
                                                                                    .withStyle(ChatFormatting.DARK_AQUA)
                                                                                    .append(Component.literal(
                                                                                                    configured.feature().getClass().getSimpleName())
                                                                                            .withStyle(ChatFormatting.WHITE)),
                                                                            false
                                                                    );
                                                                    ctx.getSource().sendSuccess(
                                                                            () -> Component.literal("  Config Type: ")
                                                                                    .withStyle(ChatFormatting.DARK_AQUA)
                                                                                    .append(Component.literal(
                                                                                                    configured.config().getClass().getSimpleName())
                                                                                            .withStyle(ChatFormatting.WHITE)),
                                                                            false
                                                                    );

                                                                    if (configured.config() instanceof OreConfiguration oreConfig) {
                                                                        ctx.getSource().sendSuccess(
                                                                                () -> Component.literal("  Size: ")
                                                                                        .withStyle(ChatFormatting.DARK_GREEN)
                                                                                        .append(Component.literal(
                                                                                                        String.valueOf(oreConfig.size))
                                                                                                .withStyle(ChatFormatting.WHITE)),
                                                                                false
                                                                        );
                                                                        ctx.getSource().sendSuccess(
                                                                                () -> Component.literal("  Discard chance on air exposure: ")
                                                                                        .withStyle(ChatFormatting.DARK_GREEN)
                                                                                        .append(Component.literal(
                                                                                                        String.valueOf(oreConfig.discardChanceOnAirExposure))
                                                                                                .withStyle(ChatFormatting.WHITE)),
                                                                                false
                                                                        );
                                                                        ctx.getSource().sendSuccess(
                                                                                () -> Component.literal("  Targets: ")
                                                                                        .withStyle(ChatFormatting.DARK_GREEN)
                                                                                        .append(Component.literal(
                                                                                                        String.valueOf(oreConfig.targetStates.size()))
                                                                                                .withStyle(ChatFormatting.WHITE)),
                                                                                false
                                                                        );
                                                                        for (OreConfiguration.TargetBlockState target : oreConfig.targetStates) {
                                                                            String blockName = target.state.getBlock()
                                                                                    .builtInRegistryHolder().key().location().toString();
                                                                            ctx.getSource().sendSuccess(
                                                                                    () -> Component.literal("    - " + blockName)
                                                                                            .withStyle(ChatFormatting.GRAY),
                                                                                    false
                                                                            );
                                                                        }
                                                                        ctx.getSource().sendSuccess(
                                                                                () -> Component.literal("  Placement Modifiers:")
                                                                                        .withStyle(ChatFormatting.DARK_GREEN),
                                                                                false
                                                                        );

                                                                        for (var mod : placed.placement()) {
                                                                            try {
                                                                                if (mod instanceof net.minecraft.world.level.levelgen.placement.CountPlacement countPlacement) {
                                                                                    ctx.getSource().sendSuccess(
                                                                                            () -> Component.literal("    - CountPlacement")
                                                                                                    .withStyle(ChatFormatting.GRAY),
                                                                                            false
                                                                                    );
                                                                                } else if (mod instanceof net.minecraft.world.level.levelgen.placement.RarityFilter rarityFilter) {
                                                                                    ctx.getSource().sendSuccess(
                                                                                            () -> Component.literal("    - RarityFilter")
                                                                                                    .withStyle(ChatFormatting.GRAY),
                                                                                            false
                                                                                    );
                                                                                } else if (mod instanceof net.minecraft.world.level.levelgen.placement.HeightRangePlacement heightRange) {
                                                                                    ctx.getSource().sendSuccess(
                                                                                            () -> Component.literal("    - HeightRangePlacement")
                                                                                                    .withStyle(ChatFormatting.GRAY),
                                                                                            false
                                                                                    );
                                                                                } else if (mod.getClass().getSimpleName().equals("InSquarePlacement")) {
                                                                                    ctx.getSource().sendSuccess(
                                                                                            () -> Component.literal("    - InSquarePlacement")
                                                                                                    .withStyle(ChatFormatting.GRAY),
                                                                                            false
                                                                                    );
                                                                                } else if (mod.getClass().getSimpleName().equals("BiomeFilter")) {
                                                                                    ctx.getSource().sendSuccess(
                                                                                            () -> Component.literal("    - BiomeFilter")
                                                                                                    .withStyle(ChatFormatting.GRAY),
                                                                                            false
                                                                                    );
                                                                                } else {
                                                                                    ctx.getSource().sendSuccess(
                                                                                            () -> Component.literal("    - " + mod.getClass().getSimpleName())
                                                                                                    .withStyle(ChatFormatting.GRAY),
                                                                                            false
                                                                                    );
                                                                                }
                                                                            } catch (Exception e) {
                                                                                ctx.getSource().sendSuccess(
                                                                                        () -> Component.literal("    - <error in PlacementModifier: " + mod.getClass().getSimpleName() + ">")
                                                                                                .withStyle(ChatFormatting.RED),
                                                                                        false
                                                                                );
                                                                            }
                                                                        }
                                                                    } else {
                                                                        ctx.getSource().sendSuccess(
                                                                                () -> Component.literal("  Config: ")
                                                                                        .withStyle(ChatFormatting.DARK_GREEN)
                                                                                        .append(Component.literal(
                                                                                                        configured.config().toString())
                                                                                                .withStyle(ChatFormatting.GREEN)),
                                                                                false
                                                                        );
                                                                    }
                                                                }
                                                            } else {
                                                                ctx.getSource().sendSuccess(
                                                                        () -> Component.literal("No PlacedFeature found for blockstate: " + blockState)
                                                                                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                                                                        false
                                                                );
                                                            }
                                                            return 1;
                                                        })
                                        )
                                        .executes(ctx -> {
                                            Set<String> ores = getAllLoadedOreNames(ctx.getSource());
                                            if (ores.isEmpty()) {
                                                ctx.getSource().sendSuccess(
                                                        () -> Component.literal("No ores found!")
                                                                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                                                        false
                                                );
                                            } else {
                                                ctx.getSource().sendSuccess(
                                                        () -> Component.literal("All ore blockstates: " + String.join(", ", ores))
                                                                .withStyle(ChatFormatting.YELLOW),
                                                        false
                                                );
                                            }
                                            return 1;
                                        })
                        )
        );
    }

    private static CompletableFuture<Suggestions> suggestOreBlockStates(
            CommandSourceStack source, SuggestionsBuilder builder
    ) {
        for (String blockState : getAllLoadedOreNames(source)) {
            builder.suggest(blockState);
        }
        return builder.buildFuture();
    }

    private static Object getPrivateField(Object obj, String fieldName) {
        try {
            var field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            return "<error>";
        }
    }

    public static Set<String> getAllLoadedOreNames(CommandSourceStack source) {
        Registry<Block> blockRegistry = source.getLevel().registryAccess()
                .registryOrThrow(Registries.BLOCK);
        return blockRegistry.keySet().stream()
                .filter(ReducedOresCommand::isOreName)
                .map(ResourceLocation::toString)
                .collect(Collectors.toSet());
    }

    private static boolean isOreName(ResourceLocation rl) {
        String path = rl.getPath();
        return path.endsWith("_ore")
                || path.contains("_ore_")
                || path.equals("ancient_debris")
                || path.endsWith("_crystal")
                || path.endsWith("_gem");
    }

    private static boolean placedFeatureHasBlockState(PlacedFeature placed, String blockState) {
        if (placed == null) return false;
        var configuredHolder = placed.feature();
        var configured = configuredHolder.value();
        if (configured.config() instanceof OreConfiguration oreConfig) {
            for (OreConfiguration.TargetBlockState target : oreConfig.targetStates) {
                ResourceLocation blockRL = target.state.getBlock()
                        .builtInRegistryHolder().key().location();
                if (blockRL != null && blockRL.toString().equalsIgnoreCase(blockState)) {
                    return true;
                }
            }
        }
        return false;
    }
}