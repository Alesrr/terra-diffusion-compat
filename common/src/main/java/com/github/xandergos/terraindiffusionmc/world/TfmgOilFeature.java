package com.github.xandergos.terraindiffusionmc.world;

import com.github.xandergos.terraindiffusionmc.config.TerrainDiffusionConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

// Stands in for TFMG's oil deposit and oil well features
public final class TfmgOilFeature extends Feature<NoneFeatureConfiguration> {

    public enum Kind {
        DEPOSIT,
        WELL
    }

    private static final int BEDROCK_LIFT = 12;

    // Worldgen may only write near the chunk it is decorating
    private static final int WRITE_MARGIN = 8;

    private final Kind kind;

    public TfmgOilFeature(Kind kind) {
        super(NoneFeatureConfiguration.CODEC);
        this.kind = kind;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!TfmgCompat.blocksResolved()) return false;
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        return kind == Kind.DEPOSIT
                ? placeDeposits(level, context.origin(), random)
                : placeWell(level, context.origin(), random);
    }

    private boolean placeDeposits(WorldGenLevel level, BlockPos origin, RandomSource random) {
        int rarity = TerrainDiffusionConfig.tfmgDepositRarity();
        if (rarity > 1 && random.nextInt(rarity) != 0) return false;

        int blobs = 1 + random.nextInt(Math.max(1, TerrainDiffusionConfig.tfmgDepositBlobs()));
        int height = Math.max(1, TerrainDiffusionConfig.tfmgDepositHeight());
        int spread = Math.max(0, Math.min(WRITE_MARGIN, TerrainDiffusionConfig.tfmgDepositSpread()));

        boolean placed = false;
        int dx = 0;
        int dz = 0;
        for (int i = 0; i < blobs; i++) {
            placed |= placeDeposit(level, origin.offset(dx, 0, dz), random, height);
            dx = clamp(dx + random.nextInt(2 * spread + 1) - spread, spread);
            dz = clamp(dz + random.nextInt(2 * spread + 1) - spread, spread);
        }
        return placed;
    }

    private static int clamp(int value, int bound) {
        return value < -bound ? -bound : (value > bound ? bound : value);
    }

    private boolean placeDeposit(WorldGenLevel level, BlockPos origin, RandomSource random,
                                 int height) {
        BlockPos base = firstRockAtOrAbove(level, origin);
        if (base == null) return false;

        level.setBlock(base, TfmgCompat.depositMarker(), 2);

        BlockState oil = TfmgCompat.crudeOil();
        BlockState fossil = TfmgCompat.fossilstone();
        int reach = random.nextInt(height);
        BlockPos pos = base;
        for (int i = 0; i < reach; i++) {
            pos = pos.above();
            if (!isRock(level, pos)) break;
            level.setBlock(pos, oil, 2);

            Direction side = Direction.getRandom(random);
            if (side.getAxis().isHorizontal() && isRock(level, pos.relative(side))) {
                level.setBlock(pos.relative(side), oil, 2);
            }
            if (i < 4) {
                Direction crust = Direction.getRandom(random);
                if (crust.getAxis().isHorizontal() && isRock(level, pos.relative(crust))) {
                    level.setBlock(pos.relative(crust), fossil, 2);
                }
            }
        }
        return true;
    }

    private boolean placeWell(WorldGenLevel level, BlockPos origin, RandomSource random) {
        BlockPos base = firstRockAtOrAbove(level, origin);
        if (base == null) return false;

        int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, base.getX(), base.getZ());
        int top = surface + TerrainDiffusionConfig.tfmgWellSurfaceOffset();
        if (top <= base.getY()) return false;

        level.setBlock(base, TfmgCompat.depositMarker(), 2);

        BlockState oil = TfmgCompat.crudeOil();
        BlockState fossil = TfmgCompat.fossilstone();
        BlockPos.MutableBlockPos pos = base.mutable();
        for (int y = base.getY() + 1; y < top; y++) {
            pos.setY(y);
            if (!isRock(level, pos)) continue;
            level.setBlock(pos, oil, 2);

            for (Direction side : Direction.Plane.HORIZONTAL) {
                if (random.nextInt(3) != 1) continue;
                BlockPos next = pos.relative(side);
                if (level.getBlockState(next).is(Blocks.STONE)) {
                    level.setBlock(next, fossil, 2);
                }
            }
        }

        placePool(level, base, surface, random);
        return true;
    }

    private static void placePool(WorldGenLevel level, BlockPos base, int surface, RandomSource random) {
        int radius = Math.max(0, Math.min(WRITE_MARGIN, TerrainDiffusionConfig.tfmgWellPoolRadius()));
        if (radius == 0) return;

        BlockState oil = TfmgCompat.crudeOil();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                if (random.nextInt(10) != 7) continue;

                int x = base.getX() + dx;
                int z = base.getZ() + dz;
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                if (Math.abs(y - surface) > radius) continue;

                pos.set(x, y, z);
                if (!level.getBlockState(pos).isAir()
                        && level.getBlockState(pos).getFluidState().isEmpty()) {
                    continue;
                }
                pos.setY(y - 1);
                if (!isRock(level, pos) && !level.getBlockState(pos).isSolid()) continue;
                pos.setY(y);
                level.setBlock(pos, oil, 2);
            }
        }
    }

    // The first position at or above origin holding rock, or null within the lift
    private static BlockPos firstRockAtOrAbove(WorldGenLevel level, BlockPos origin) {
        BlockPos.MutableBlockPos pos = origin.mutable();
        for (int i = 0; i <= BEDROCK_LIFT; i++) {
            pos.setY(origin.getY() + i);
            if (pos.getY() >= level.getMaxBuildHeight()) return null;
            if (isRock(level, pos)) return pos.immutable();
        }
        return null;
    }

    private static boolean isRock(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir()
                && state.getFluidState().isEmpty()
                && !state.is(Blocks.BEDROCK)
                && !state.is(TfmgCompat.depositMarker().getBlock());
    }
}
