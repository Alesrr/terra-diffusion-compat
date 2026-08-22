package com.github.xandergos.terraindiffusionmc.mixin;

import com.github.xandergos.terraindiffusionmc.world.HeightConverter;
import com.github.xandergos.terraindiffusionmc.world.TerrainDiffusionBiomeSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

@Mixin(Structure.class)
public class StructureMixin {

    private static final int GROUND_TOLERANCE = 4;

    @Inject(method = "generate", at = @At("RETURN"), cancellable = true)
    private void terrainDiffusion$vetoUnsupportedStart(
            RegistryAccess registryAccess, ChunkGenerator generator, BiomeSource biomeSource,
            RandomState randomState, StructureTemplateManager templateManager, long seed,
            ChunkPos chunkPos, int references, LevelHeightAccessor level,
            Predicate<Holder<Biome>> validBiome, CallbackInfoReturnable<StructureStart> cir) {

        if (!(biomeSource instanceof TerrainDiffusionBiomeSource)) {
            return;
        }

        StructureStart start = cir.getReturnValue();
        if (start == null || !start.isValid()) {
            return;
        }

        BoundingBox box = start.getBoundingBox();
        int startY = box.minY();
        if (startY >= HeightConverter.SEA_LEVEL) {
            return;
        }

        BlockPos center = box.getCenter();
        int ground = generator.getBaseHeight(center.getX(), center.getZ(),
                Heightmap.Types.OCEAN_FLOOR_WG, level, randomState);

        if (startY > ground + GROUND_TOLERANCE) {
            cir.setReturnValue(StructureStart.INVALID_START);
        }
    }
}
