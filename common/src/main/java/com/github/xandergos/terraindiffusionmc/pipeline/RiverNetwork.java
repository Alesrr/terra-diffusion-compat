package com.github.xandergos.terraindiffusionmc.pipeline;

public final class RiverNetwork {
    private static final FastNoiseLite CHANNEL = makeFnl(0x5217, 1f / 2600f, 2, 2f, 0.5f);

    private static final FastNoiseLite MEANDER = makeFnl(0x77A3, 1f / 520f, 2, 2f, 0.5f);

    private static final FastNoiseLite WIDTH = makeFnl(0x3C91, 1f / 1400f, 2, 2f, 0.5f);

    private static final float BASE_HALF_WIDTH = 5.5f;

    private static final float WIDTH_VARIATION = 0.45f;

    private static final float FULL_ELEV_M = 55f;

    private static final float MAX_ELEV_M = 150f;

    private static final float FULL_SLOPE = 0.06f;

    private static final float MAX_SLOPE = 0.20f;

    private static final float MAX_DEPTH_M = 42f;

    private RiverNetwork() {
    }

    private static FastNoiseLite makeFnl(int seed, float freq, int oct, float lac, float gain) {
        FastNoiseLite fnl = new FastNoiseLite(seed);
        fnl.SetNoiseType(FastNoiseLite.NoiseType.Perlin);
        fnl.SetFractalType(FastNoiseLite.FractalType.FBm);
        fnl.SetFrequency(freq);
        fnl.SetFractalOctaves(oct);
        fnl.SetFractalLacunarity(lac);
        fnl.SetFractalGain(gain);
        return fnl;
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    private static float field(float x, float z) {
        return CHANNEL.GetNoise(x, z) + 0.22f * MEANDER.GetNoise(x, z);
    }

    public static float carveDepthMeters(int worldX, int worldZ, float elevM, float slope) {
        if (elevM < 0f || elevM > MAX_ELEV_M || slope > MAX_SLOPE) {
            return 0f;
        }

        float n0 = field(worldX, worldZ);
        float gx = field(worldX + 1, worldZ) - n0;
        float gz = field(worldX, worldZ + 1) - n0;
        float gradient = (float) Math.sqrt(gx * gx + gz * gz);
        if (gradient < 1.0e-7f) {
            return 0f;
        }
        float distanceBlocks = Math.abs(n0) / gradient;

        float halfWidth = BASE_HALF_WIDTH
                * (1f + WIDTH_VARIATION * WIDTH.GetNoise(worldX, worldZ));
        if (distanceBlocks >= halfWidth) {
            return 0f;
        }

        float elevFade = clamp01((MAX_ELEV_M - elevM) / (MAX_ELEV_M - FULL_ELEV_M));
        float slopeFade = clamp01((MAX_SLOPE - slope) / (MAX_SLOPE - FULL_SLOPE));

        float t = 1f - distanceBlocks / halfWidth;
        float profile = t * t * (3f - 2f * t);

        return MAX_DEPTH_M * profile * elevFade * slopeFade;
    }

    public static boolean isRiver(int worldX, int worldZ, float elevM, float slope) {
        return carveDepthMeters(worldX, worldZ, elevM, slope) > 6f;
    }
}
