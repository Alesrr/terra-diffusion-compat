package com.github.xandergos.terraindiffusionmc.world;

import com.github.xandergos.terraindiffusionmc.config.TerrainDiffusionConfig;
import com.github.xandergos.terraindiffusionmc.mixin.NoiseBasedChunkGeneratorAccessor;
import com.github.xandergos.terraindiffusionmc.pipeline.TerralithCompat;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TerralithSurfaceRules {
    private static final Logger LOG = LoggerFactory.getLogger(TerralithSurfaceRules.class);

    private static NoiseGeneratorSettings splicedInto;

    private TerralithSurfaceRules() {
    }

    public static void apply(ServerLevel overworld) {
        ChunkGenerator generator = overworld.getChunkSource().getGenerator();

        generator.getBiomeSource().possibleBiomes();

        if (!TerralithCompat.isActive() || !TerrainDiffusionConfig.terralithInjectSurfaceRules()) {
            return;
        }

        if (!(generator instanceof NoiseBasedChunkGenerator noiseGenerator)) {
            return;
        }

        NoiseGeneratorSettings current = noiseGenerator.generatorSettings().value();
        if (current == splicedInto) {
            return;
        }

        SurfaceRules.RuleSource terralithRule = overworld.registryAccess()
                .registryOrThrow(Registries.NOISE_SETTINGS)
                .getOptional(NoiseGeneratorSettings.OVERWORLD)
                .map(NoiseGeneratorSettings::surfaceRule)
                .orElse(null);

        if (terralithRule == null) {
            LOG.warn("Could not read the overworld surface rule; Terralith biomes will use default surfaces");
            return;
        }

        NoiseGeneratorSettings merged = new NoiseGeneratorSettings(
                current.noiseSettings(),
                current.defaultBlock(),
                current.defaultFluid(),
                current.noiseRouter(),
                SurfaceRules.sequence(terralithRule, current.surfaceRule()),
                current.spawnTarget(),
                current.seaLevel(),
                current.disableMobGeneration(),
                current.isAquifersEnabled(),
                current.oreVeinsEnabled(),
                current.useLegacyRandomSource()
        );

        ((NoiseBasedChunkGeneratorAccessor) (Object) noiseGenerator)
                .terrainDiffusion$setSettings(Holder.direct(merged));
        splicedInto = merged;
        LOG.info("Applied Terralith surface rules to the terrain-diffusion overworld");
    }
}
