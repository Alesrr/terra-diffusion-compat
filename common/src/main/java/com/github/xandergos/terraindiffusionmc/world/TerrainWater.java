package com.github.xandergos.terraindiffusionmc.world;

import com.github.xandergos.terraindiffusionmc.config.TerrainDiffusionConfig;
import com.github.xandergos.terraindiffusionmc.pipeline.DeepCaverns;
import com.github.xandergos.terraindiffusionmc.pipeline.KarstNetwork;
import com.github.xandergos.terraindiffusionmc.pipeline.LocalTerrainProvider;
import com.github.xandergos.terraindiffusionmc.pipeline.LocalTerrainProvider.HeightmapData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Aquifer;

public final class TerrainWater {

    public static final int NO_WATER = Integer.MIN_VALUE;
    public static final int NO_GROUND = Integer.MIN_VALUE;

    private static final boolean DRY_CAVES =
            !"false".equals(System.getProperty("terradiff.dryCaves"));

    private static final boolean KARST_WATER =
            !"false".equals(System.getProperty("terradiff.karstWater"));

    private static final int POOL_DEPTH =
            Integer.parseInt(System.getProperty("terradiff.poolDepth", "6"));

    private static final long NO_BAND =
            ((long) Integer.MIN_VALUE << 32) | (Integer.MAX_VALUE & 0xffffffffL);

    private static final int PERCHED_AIR_SIDES =
            Integer.parseInt(System.getProperty("terradiff.perchedSides", "3"));

    private static final class TileRef {
        int startX = Integer.MIN_VALUE;
        int startZ = Integer.MIN_VALUE;
        HeightmapData data;
    }

    private static final ThreadLocal<TileRef> LAST = ThreadLocal.withInitial(TileRef::new);

    private TerrainWater() {
    }

    private static HeightmapData tileFor(int x, int z) {
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
        return ref.data;
    }

    private static int tileStart(int v) {
        int tileSize = TerrainDiffusionConfig.tileSize();
        int tileShift = Integer.numberOfTrailingZeros(tileSize);
        return (v >> tileShift) << tileShift;
    }

    public static int waterLevelY(int x, int z) {
        HeightmapData data = tileFor(x, z);
        if (data == null || data.waterLevel == null) {
            return NO_WATER;
        }
        int localX = x - tileStart(x);
        int localZ = z - tileStart(z);
        if (localX < 0 || localZ < 0 || localX >= data.width || localZ >= data.height) {
            return NO_WATER;
        }
        short meters = data.waterLevel[localZ][localX];
        if (meters == HeightmapData.NO_WATER) {
            return NO_WATER;
        }
        return HeightConverter.convertToMinecraftHeight(meters);
    }

    public static int groundY(int x, int z) {
        HeightmapData data = tileFor(x, z);
        if (data == null || data.heightmap == null) {
            return NO_GROUND;
        }
        int localX = Math.max(0, Math.min(data.width - 1, x - tileStart(x)));
        int localZ = Math.max(0, Math.min(data.height - 1, z - tileStart(z)));
        return HeightConverter.convertToMinecraftHeight(data.heightmap[localZ][localX]);
    }

    public static KarstNetwork karstNetwork(int x, int z) {
        HeightmapData data = tileFor(x, z);
        return data == null || data.karst == null ? KarstNetwork.EMPTY : data.karst;
    }

    public static float karstWaterTableY(int x, int z) {
        KarstNetwork net = karstNetwork(x, z);
        return net.isEmpty() ? Float.NEGATIVE_INFINITY : net.waterTableY(x, z);
    }

    private static boolean openBlock(int x, int y, int z, int ground) {
        if (ground == NO_GROUND || y >= ground) return false;
        KarstNetwork net = karstNetwork(x, z);
        if (!net.isEmpty() && net.density(x, y, z) < 0f) return true;
        return y <= DeepCaverns.TOP && DeepCaverns.density(x, y, z) < 0f;
    }

    private static boolean wetNear(int x, int y, int z) {
        if (y <= DeepCaverns.TOP && DeepCaverns.fluid(x, y, z) == DeepCaverns.WATER) return true;
        float table = karstWaterTableY(x, z);
        if (Float.isInfinite(table)) return false;
        int levelY = Math.round(table);
        return y < levelY && y >= levelY - POOL_DEPTH;
    }

    /** Pool extent for a column, packed as {@code (top << 32) | bottom}, or {@link #NO_BAND}. */
    private static long bandAt(int x, int z) {
        int ground = groundY(x, z);
        if (ground == NO_GROUND) return NO_BAND;
        float table = karstWaterTableY(x, z);
        if (Float.isInfinite(table)) return NO_BAND;
        int levelY = Math.round(table);
        if (levelY >= ground) levelY = ground - 1;

        KarstNetwork net = karstNetwork(x, z);
        if (!net.isEmpty() && net.dolineDensity(x, levelY - 1, z) < 0f) return NO_BAND;

        int k = 1;
        while (k <= POOL_DEPTH + 1 && openBlock(x, levelY - k, z, ground)) k++;
        if (k == 1 || k > POOL_DEPTH + 1) return NO_BAND;
        return ((long) levelY << 32) | ((levelY - k + 1) & 0xffffffffL);
    }

    private static boolean covers(long band, int y) {
        return band != NO_BAND && y >= (int) band && y < (int) (band >> 32);
    }

    /**
     * True where a pool column shows a horizontal face to open air, so the terrain puts stone there
     * instead. Covers the whole band, giving a bank from the bed up to the waterline.
     */
    public static boolean rimSolid(int x, int y, int z) {
        if (!KARST_WATER) return false;
        RimRef ref = RIM.get();
        if (ref.x != x || ref.z != z) {
            ref.x = x;
            ref.z = z;
            ref.band = bandAt(x, z);
            ref.bank = ref.band != NO_BAND && bankColumn(x, z, ref.band);
        }
        return ref.bank && covers(ref.band, y);
    }

    private static boolean bankColumn(int x, int z, long band) {
        int bottom = (int) band, top = (int) (band >> 32);
        for (int d = 0; d < 4; d++) {
            int nx = x + (d == 0 ? 1 : d == 1 ? -1 : 0);
            int nz = z + (d == 2 ? 1 : d == 3 ? -1 : 0);
            int ng = groundY(nx, nz);
            long nb = bandAt(nx, nz);
            for (int y = bottom; y < top; y++) {
                if (!openBlock(nx, y, nz, ng)) continue;
                if (!covers(nb, y)) return true;
            }
        }
        return false;
    }

    private static final class RimRef {
        int x = Integer.MIN_VALUE;
        int z = Integer.MIN_VALUE;
        long band = NO_BAND;
        boolean bank;
    }

    private static final ThreadLocal<RimRef> RIM = ThreadLocal.withInitial(RimRef::new);

    /** A block with air on nearly every side is a perch, not a pool; refuse to put water there. */
    private static boolean perched(int x, int y, int z) {
        int air = 0;
        for (int d = 0; d < 4; d++) {
            int nx = x + (d == 0 ? 1 : d == 1 ? -1 : 0);
            int nz = z + (d == 2 ? 1 : d == 3 ? -1 : 0);
            int ground = groundY(nx, nz);
            if (!openBlock(nx, y, nz, ground)) continue;
            if (wetNear(nx, y, nz)) continue;
            air++;
        }
        return air >= PERCHED_AIR_SIDES;
    }

    public static Aquifer.FluidPicker fluidPicker(int seaLevel, BlockState fluid) {
        Aquifer.FluidStatus ocean = new Aquifer.FluidStatus(seaLevel, fluid);
        Aquifer.FluidStatus dry =
                new Aquifer.FluidStatus(DimensionType.MIN_Y * 2, Blocks.AIR.defaultBlockState());
        Aquifer.FluidStatus stream = new Aquifer.FluidStatus(Integer.MAX_VALUE / 2, fluid);
        Aquifer.FluidStatus lava =
                new Aquifer.FluidStatus(Integer.MAX_VALUE / 2, Blocks.LAVA.defaultBlockState());
        ThreadLocal<ColumnRef> columns = ThreadLocal.withInitial(ColumnRef::new);

        return (x, y, z) -> {
            ColumnRef ref = columns.get();
            if (ref.x != x || ref.z != z || ref.status == null) {
                ref.x = x;
                ref.z = z;
                ref.ground = DRY_CAVES ? groundY(x, z) : NO_GROUND;
                int level = waterLevelY(x, z);
                ref.status = level == NO_WATER
                        ? ocean
                        : new Aquifer.FluidStatus(level, fluid);
                computeStream(ref, x, z);
            }

            if (ref.ground == NO_GROUND || y >= ref.ground) {
                return ref.status;
            }
            if (y <= DeepCaverns.LAVA_Y) {
                return lava;
            }
            if (!KARST_WATER) {
                return dry;
            }
            boolean wet = (y >= ref.streamBottom && y < ref.streamTop)
                    || (y <= DeepCaverns.TOP && DeepCaverns.fluid(x, y, z) == DeepCaverns.WATER);
            if (!wet || perched(x, y, z)) {
                return dry;
            }
            return stream;
        };
    }

    private static void computeStream(ColumnRef ref, int x, int z) {
        long band = KARST_WATER && ref.ground != NO_GROUND ? bandAt(x, z) : NO_BAND;
        ref.streamTop = (int) (band >> 32);
        ref.streamBottom = (int) band;
    }

    private static final class ColumnRef {
        int x = Integer.MIN_VALUE;
        int z = Integer.MIN_VALUE;
        int ground = NO_GROUND;
        int streamTop = Integer.MIN_VALUE;
        int streamBottom = Integer.MAX_VALUE;
        Aquifer.FluidStatus status;
    }
}
