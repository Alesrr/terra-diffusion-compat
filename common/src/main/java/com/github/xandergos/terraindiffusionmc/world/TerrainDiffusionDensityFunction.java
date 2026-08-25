package com.github.xandergos.terraindiffusionmc.world;

import com.github.xandergos.terraindiffusionmc.config.TerrainDiffusionConfig;
import com.github.xandergos.terraindiffusionmc.pipeline.LocalTerrainProvider;
import com.github.xandergos.terraindiffusionmc.pipeline.DeepCaverns;
import com.github.xandergos.terraindiffusionmc.pipeline.KarstNetwork;
import com.github.xandergos.terraindiffusionmc.pipeline.LocalTerrainProvider.HeightmapData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

public class TerrainDiffusionDensityFunction implements DensityFunction {
    public static final MapCodec<TerrainDiffusionDensityFunction> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.BOOL.optionalFieldOf("caves", Boolean.TRUE).forGetter(f -> f.caves)
            ).apply(instance, TerrainDiffusionDensityFunction::new));

    private static final int CAVE_ROOF =
            Integer.parseInt(System.getProperty("terradiff.caveRoof", "5"));

    private static final int DEEP_CLEARANCE =
            Integer.parseInt(System.getProperty("terradiff.deepClearance", "8"));

    public static final TerrainDiffusionDensityFunction INSTANCE =
            new TerrainDiffusionDensityFunction(Boolean.TRUE);

    private final boolean caves;

    public TerrainDiffusionDensityFunction(Boolean caves) {
        this.caves = caves == null || caves;
    }

    @Override
    public double compute(DensityFunction.FunctionContext pos) {
        int x = pos.blockX();
        int z = pos.blockZ();
        int y = pos.blockY();

        int tileSize = TerrainDiffusionConfig.tileSize();
        int tileShift = Integer.numberOfTrailingZeros(tileSize);

        int tileX = x >> tileShift;
        int tileZ = z >> tileShift;

        int blockStartX = tileX << tileShift;
        int blockStartZ = tileZ << tileShift;

        int blockEndX = blockStartX + tileSize;
        int blockEndZ = blockStartZ + tileSize;

        HeightmapData data = LocalTerrainProvider.getInstance()
                .fetchHeightmap(blockStartZ, blockStartX, blockEndZ, blockEndX);

        if (data == null || data.heightmap == null) {
            return 0.0;
        }

        int localX = Math.max(0, Math.min(data.width - 1, x - blockStartX));
        int localZ = Math.max(0, Math.min(data.height - 1, z - blockStartZ));

        int targetHeight = HeightConverter.convertToMinecraftHeight(
                data.heightmap[localZ][localX]
        );

        double terrain = targetHeight - y;
        if (terrain <= 0.0) {
            return terrain;
        }
        return caves ? carve(data, x, y, z, terrain, targetHeight) : terrain;
    }

    private static final class FillContext {
        int blockStartX, blockStartZ, blockEndX, blockEndZ;
        HeightmapData data;

        void update(int x, int z) {
            if (x < blockStartX || x >= blockEndX) this.init(x, z);
            if (z < blockStartZ || z >= blockEndZ) this.init(x, z);
        }

        void init(int x, int z) {
            int tileSize = TerrainDiffusionConfig.tileSize();
            int tileShift = Integer.numberOfTrailingZeros(tileSize);

            int tileX = x >> tileShift;
            int tileZ = z >> tileShift;

            this.blockStartX = tileX << tileShift;
            this.blockStartZ = tileZ << tileShift;
            this.blockEndX = blockStartX + tileSize;
            this.blockEndZ = blockStartZ + tileSize;

            this.data = LocalTerrainProvider.getInstance()
                .fetchHeightmap(blockStartZ, blockStartX, blockEndZ, blockEndX);
        }
    }

    @Override
    public void fillArray(double[] densities, DensityFunction.ContextProvider applier) {
        if (densities.length == 0) return;

        FillContext ctx = new FillContext();
        DensityFunction.FunctionContext pos = applier.forIndex(0);
        int x = pos.blockX();
        int z = pos.blockZ();
        int y = pos.blockY();
        ctx.init(x, z);

        for (int i = 0; i < densities.length; i++) {
            pos = applier.forIndex(i);
            x = pos.blockX();
            z = pos.blockZ();
            y = pos.blockY();
            ctx.update(x, z);

            HeightmapData data = ctx.data;
            if (data == null || data.heightmap == null) {
                densities[i] = -y;
                continue;
            }

            int localX = Math.max(0, Math.min(data.width  - 1, x - ctx.blockStartX));
            int localZ = Math.max(0, Math.min(data.height - 1, z - ctx.blockStartZ));

            int targetHeight = HeightConverter
                .convertToMinecraftHeight(data.heightmap[localZ][localX]);
            double terrain = targetHeight - y;
            densities[i] = (!caves || terrain <= 0.0)
                    ? terrain
                    : carve(data, x, y, z, terrain, targetHeight);
        }
    }

    private static double carve(HeightmapData data, int x, int y, int z,
                                double terrain, int targetHeight) {
        double best = terrain;
        boolean shallow = terrain < CAVE_ROOF;
        KarstNetwork karst = data.karst;
        if (karst != null && !karst.isEmpty()) {
            float cave = shallow ? karst.dolineDensity(x, y, z) : karst.density(x, y, z);
            if (cave < best) best = cave;
        }
        if (!shallow && y <= DeepCaverns.TOP && y < targetHeight - DEEP_CLEARANCE) {
            float deep = DeepCaverns.density(x, y, z);
            if (deep < best) best = deep;
        }
        if (best < 0.0 && TerrainWater.rimSolid(x, y, z)) return -best;
        return best;
    }

    @Override
    public DensityFunction mapAll(DensityFunction.Visitor visitor) {
        return visitor.apply(this);
    }

    @Override
    public double minValue() {
        return -4096;
    }

    @Override
    public double maxValue() {
        return 4096;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return KeyDispatchDataCodec.of(CODEC);
    }
}
