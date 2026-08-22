package com.github.xandergos.terraindiffusionmc.world;

import com.github.xandergos.terraindiffusionmc.config.TerrainDiffusionConfig;
import com.github.xandergos.terraindiffusionmc.pipeline.LocalTerrainProvider;
import com.github.xandergos.terraindiffusionmc.pipeline.LocalTerrainProvider.HeightmapData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Aquifer;

public final class TerrainWater {

    public static final int NO_WATER = Integer.MIN_VALUE;

    private static final class TileRef {
        int startX = Integer.MIN_VALUE;
        int startZ = Integer.MIN_VALUE;
        HeightmapData data;
    }

    private static final ThreadLocal<TileRef> LAST = ThreadLocal.withInitial(TileRef::new);

    private TerrainWater() {
    }

    public static int waterLevelY(int x, int z) {
        int tileSize = TerrainDiffusionConfig.tileSize();
        int tileShift = Integer.numberOfTrailingZeros(tileSize);

        int startX = (x >> tileShift) << tileShift;
        int startZ = (z >> tileShift) << tileShift;

        TileRef ref = LAST.get();
        if (ref.startX != startX || ref.startZ != startZ) {
            ref.startX = startX;
            ref.startZ = startZ;
            ref.data = LocalTerrainProvider.getInstance().fetchHeightmap(
                    startZ, startX, startZ + tileSize, startX + tileSize);
        }

        HeightmapData data = ref.data;
        if (data == null || data.waterLevel == null) {
            return NO_WATER;
        }

        int localX = x - startX;
        int localZ = z - startZ;
        if (localX < 0 || localZ < 0 || localX >= data.width || localZ >= data.height) {
            return NO_WATER;
        }

        short meters = data.waterLevel[localZ][localX];
        if (meters == HeightmapData.NO_WATER) {
            return NO_WATER;
        }
        return HeightConverter.convertToMinecraftHeight(meters);
    }

    public static Aquifer.FluidPicker fluidPicker(int seaLevel, BlockState fluid) {
        Aquifer.FluidStatus ocean = new Aquifer.FluidStatus(seaLevel, fluid);
        ThreadLocal<ColumnRef> columns = ThreadLocal.withInitial(ColumnRef::new);
        return (x, y, z) -> {
            ColumnRef ref = columns.get();
            if (ref.x != x || ref.z != z || ref.status == null) {
                ref.x = x;
                ref.z = z;
                int level = waterLevelY(x, z);
                ref.status = level == NO_WATER
                        ? ocean
                        : new Aquifer.FluidStatus(level, fluid);
            }
            return ref.status;
        };
    }

    private static final class ColumnRef {
        int x = Integer.MIN_VALUE;
        int z = Integer.MIN_VALUE;
        Aquifer.FluidStatus status;
    }
}
