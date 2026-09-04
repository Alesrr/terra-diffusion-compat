package com.github.xandergos.terraindiffusionmc.pipeline;

public final class WaterNetwork {

    public static final float NO_WATER = -1.0e9f;

    private static final float SEA_LEVEL_M = 0f;

    private static final FastNoiseLite WIDTH_VAR     = makeFnl(0x3C91, 1f / 1400f, 2, 2f, 0.5f);

    private static final FastNoiseLite BANK_ROUGH    = makeFnl(0x1F55, 1f / 11f, 2, 2f, 0.55f);

    private static final float LOWLAND_FADE_BLOCKS = 16f;

    private static final float VALLEY_WIDTHS =
            Float.parseFloat(System.getProperty("terradiff.valleyWidths", "2.5"));

    private static final float DELTA_VALLEY_WIDTHS = 1.3f;

    private static final float MIN_CHANNEL_BLOCKS =
            Float.parseFloat(System.getProperty("terradiff.minChannelBlocks", "5.0"));

    private static final float MIN_CARVE_GATES =
            Float.parseFloat(System.getProperty("terradiff.minCarveGates", "3.0"));

    private static final float BANK_SLOPE_LIMIT =
            Float.parseFloat(System.getProperty("terradiff.bankSlope", "999"));

    private static final float SOFT_MIN_BLOCKS =
            Float.parseFloat(System.getProperty("terradiff.softMin", "1.5"));

    private static final float CARVE_REACH =
            Float.parseFloat(System.getProperty("terradiff.carveReach", "1.0"));

    private static final float CHANNEL_FLAT_FRACTION =
            Float.parseFloat(System.getProperty("terradiff.channelFlat", "0.55"));

    private static final boolean SMOOTH_CARVE =
            "true".equals(System.getProperty("terradiff.smoothCarve"));

    private static final float WIDTH_VARIATION = 0.25f;

    private static final float FALL_VALLEY_WIDTHS = 1.15f;
    private static final float FALL_WIDTH_FACTOR = 0.62f;

    private static final float FALL_FULL_BLOCKS = 6f;

    private static final float BANK_ROUGH_BLOCKS =
            Float.parseFloat(System.getProperty("terradiff.bankRough", "0.9"));

    private static final float RIVER_MAX_ELEV_M = 2400f;
    private static final float RIVER_FADE_M = 400f;

    private static final boolean BLOCK_GATE =
            !"false".equals(System.getProperty("terradiff.blockGate"));

    private static final float SILL_CUT_BLOCKS =
            Float.parseFloat(System.getProperty("terradiff.sillCut", "0"));

    private static final float SILL_MIN_VALLEY =
            Float.parseFloat(System.getProperty("terradiff.sillValley", "0.5"));

    private static final float MOUTH_BAR_BLOCKS =
            Float.parseFloat(System.getProperty("terradiff.mouthBar", "2"));

    private static final float MOUTH_FADE_BLOCKS =
            Float.parseFloat(System.getProperty("terradiff.mouthFade", "6"));

    // How far from the centreline water may be drawn, in channel half-widths
    private static final float WATER_REACH_WIDTHS =
            Float.parseFloat(System.getProperty("terradiff.waterReach", "0"));

    private static final float MOUTH_LIP_BLOCKS =
            Float.parseFloat(System.getProperty("terradiff.mouthLip", "1"));

    private static final float MOUTH_FLOOR_BLOCKS =
            Float.parseFloat(System.getProperty("terradiff.mouthFloor", "0"));

    private static final float PERCH_FULL_BLOCKS =
            Float.parseFloat(System.getProperty("terradiff.perchFull", "20"));

    private static final float PERCH_FADE_BLOCKS =
            Float.parseFloat(System.getProperty("terradiff.perchFade", "13"));

    private static final float LAKE_DEPTH_GAIN = 1.9f;
    private static final float LAKE_MIN_BED_BLOCKS = 4f;
    private static final float LAKE_MAX_BED_BLOCKS = 17f;

    public static final class Sample {
        public float bedElevM;
        public float waterSurfaceM;

        public boolean hasWater() {
            return waterSurfaceM > NO_WATER;
        }
    }

    private static final ThreadLocal<Sample> SCRATCH = ThreadLocal.withInitial(Sample::new);

    private WaterNetwork() {
    }

    public static boolean isWaterBody(int worldX, int worldZ, float elevM, float precipMm,
                                      float channelDistBlocks, float channelMagnitude,
                                      float channelWaterM, float channelWidthM,
                                      float channelDepthM, float fallBlocks,
                                      float lakeSurfaceM, float lakeDeepM, float deltaWeight,
                                      float metersPerBlock, int scale) {
        Sample s = SCRATCH.get();

        sample(worldX, worldZ, elevM, elevM, precipMm, channelDistBlocks, channelMagnitude,
                channelWaterM, channelWaterM, channelWidthM, channelDepthM, fallBlocks,
                lakeSurfaceM, lakeDeepM, deltaWeight, metersPerBlock, scale, s);
        return s.hasWater();
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
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    private static float smoothstep(float t) {
        return t * t * (3f - 2f * t);
    }

    private static float fadeOut(float t) {
        return 1f - smoothstep(clamp01(t));
    }

    private static int drawnBlock(float meters, float metersPerBlock) {
        return (int) Math.floor(Math.floor(meters) / metersPerBlock);
    }

    private static float snapToBlock(float meters, float metersPerBlock) {
        return (float) Math.ceil(Math.floor(meters / metersPerBlock) * metersPerBlock);
    }

    public static void sample(int worldX, int worldZ, float elevM, float smoothElevM,
                              float precipMm,
                              float channelDistBlocks, float channelMagnitude,
                              float channelWaterM, float channelWaterSmoothM,
                              float channelWidthM, float channelDepthM, float fallBlocks,
                              float lakeSurfaceM, float lakeDeepM, float deltaWeight,
                              float metersPerBlock, int scale, Sample out) {
        out.bedElevM = elevM;
        out.waterSurfaceM = NO_WATER;

        float mouthFadeM = MOUTH_FADE_BLOCKS * metersPerBlock;
        boolean submarine = elevM < 0f;
        float seaFade = submarine ? clamp01(1f + elevM / mouthFadeM) : 1f;
        if (submarine && (seaFade <= 0f || MOUTH_FADE_BLOCKS <= 0f)) {
            return;
        }

        float levelM = channelWaterM;
        float surfaceM = SEA_LEVEL_M;
        if (!Float.isNaN(levelM) && levelM > SEA_LEVEL_M) {
            surfaceM = snapToBlock(levelM, metersPerBlock);
        }

        float lowland = fadeOut((elevM - RIVER_MAX_ELEV_M) / RIVER_FADE_M);

        float distBlocks = channelDistBlocks;
        float magnitude = channelMagnitude;

        float fall = clamp01(fallBlocks / FALL_FULL_BLOCKS);

        float widthBlocks = Math.max(MIN_CHANNEL_BLOCKS, channelWidthM / metersPerBlock);
        widthBlocks *= 1f + (FALL_WIDTH_FACTOR - 1f) * fall;
        float widen = WIDTH_VARIATION * Math.max(0f, WIDTH_VAR.GetNoise(worldX, worldZ));
        float halfWidth = 0.5f * widthBlocks * (1f + widen);

        float valleyWidths = VALLEY_WIDTHS
                + (DELTA_VALLEY_WIDTHS - VALLEY_WIDTHS) * clamp01(deltaWeight);

        valleyWidths += (FALL_VALLEY_WIDTHS - valleyWidths) * fall;

        float inner = CHANNEL_FLAT_FRACTION * halfWidth;
        float wallRun = (Math.max(MIN_CARVE_GATES * 0.5f * metersPerBlock, channelDepthM)
                / metersPerBlock) * 1.5f / Math.max(0.05f, BANK_SLOPE_LIMIT);
        float outer = Math.max(halfWidth * CARVE_REACH, inner + Math.max(1.0e-3f, wallRun));
        float channel = distBlocks < 0f ? 0f
                : (distBlocks <= inner ? 1f
                        : fadeOut((distBlocks - inner) / (outer - inner)));

        String part = System.getProperty("terradiff.carvePart", "both");
        if (Boolean.getBoolean("terradiff.noCarve") || part.equals("valley")) {
            channel = 0f;
        }
        float valley = distBlocks < 0f ? 0f : fadeOut(distBlocks / (halfWidth * valleyWidths));
        if (Boolean.getBoolean("terradiff.noCarve")
                || "channel".equals(System.getProperty("terradiff.carvePart"))) {
            valley = 0f;
        }

        float perchFade = PERCH_FADE_BLOCKS <= 0f ? 1f
                : fadeOut((elevM - surfaceM - PERCH_FULL_BLOCKS * metersPerBlock)
                        / (PERCH_FADE_BLOCKS * metersPerBlock));
        channel *= perchFade;
        valley *= perchFade;

        boolean inBasin = lakeSurfaceM > NO_WATER;
        float basinM = lakeSurfaceM;

        float basinWet = inBasin ? 1f : 0f;
        float wet = Math.max(valley, basinWet);

        String gb = System.getProperty("terradiff.groundBlend", "normal");
        if (gb.equals("off")) {
            wet = 0f;
        } else if (gb.equals("full")) {
            wet = wet > 0.001f ? 1f : 0f;
        }
        float groundM = elevM + (smoothElevM - elevM) * clamp01(wet);

        float riverDepth = Math.max(MIN_CARVE_GATES * 0.5f * metersPerBlock, channelDepthM);

        if (submarine) {
            out.bedElevM = elevM - riverDepth * channel * seaFade;
            return;
        }

        float carveM = (SMOOTH_CARVE && !Float.isNaN(channelWaterSmoothM)
                && channelWaterSmoothM > SEA_LEVEL_M)
                ? channelWaterSmoothM
                : surfaceM;
        float bed = groundM + (carveM - groundM) * valley * lowland;

        bed -= riverDepth * channel * lowland;

        if (!"off".equals(System.getProperty("terradiff.needClamp"))) {
            float need = surfaceM - MIN_CARVE_GATES * 0.5f * metersPerBlock;
            float k = Math.max(1.0e-3f, SOFT_MIN_BLOCKS * metersPerBlock);
            float h = clamp01(0.5f + 0.5f * (bed - need) / k);
            float soft = bed + (need - bed) * h - k * h * (1f - h);
            bed += (soft - bed) * channel * lowland;
        }

        // A river that has come all the way down to sea level can still be left short of the sea
        if (MOUTH_BAR_BLOCKS > 0f && surfaceM <= SEA_LEVEL_M && valley > 0f
                && groundM < MOUTH_BAR_BLOCKS * metersPerBlock) {
            float throughBar = groundM + (SEA_LEVEL_M - metersPerBlock - groundM) * valley;
            if (throughBar < bed) {
                bed = throughBar;
            }
        }

        float ramp = valley * (1f - channel);
        float roughened = bed
                + BANK_ROUGH.GetNoise(worldX, worldZ) * BANK_ROUGH_BLOCKS * metersPerBlock * ramp;

        if (bed >= surfaceM && !"off".equals(System.getProperty("terradiff.shelfClamp"))) {
            roughened = Math.max(roughened, surfaceM);
        }
        bed = roughened;

        if (bed > groundM) {
            bed = groundM;
        }

        if (inBasin && lakeDeepM > 0.01f) {
            float here = Math.max(0f, basinM - groundM);
            float t = smoothstep(clamp01(here / lakeDeepM));
            float target = Math.min(LAKE_MAX_BED_BLOCKS * metersPerBlock,
                    Math.max(LAKE_MIN_BED_BLOCKS * metersPerBlock, LAKE_DEPTH_GAIN * lakeDeepM));
            float bowl = basinM - target * t;
            float shaped = bed + (bowl - bed) * t;

            bed = Math.min(shaped, Math.max(bed, basinM - 0.6f * metersPerBlock));
        }

        if (SILL_CUT_BLOCKS > 0f && surfaceM > NO_WATER && valley >= SILL_MIN_VALLEY
                && bed > surfaceM && bed - surfaceM <= SILL_CUT_BLOCKS * metersPerBlock) {
            bed = surfaceM - 0.5f * metersPerBlock;
        }

        if (MOUTH_FLOOR_BLOCKS > 0f) {
            float lip = MOUTH_LIP_BLOCKS * metersPerBlock;
            float deep = MOUTH_FLOOR_BLOCKS * metersPerBlock;
            float floor = SEA_LEVEL_M + lip - (lip + deep) * clamp01(channel);
            if (bed < floor) {
                bed = floor;
            }
        }

        out.bedElevM = bed;

        boolean drawsAWholeBlock = BLOCK_GATE
                ? drawnBlock(surfaceM, metersPerBlock) > drawnBlock(bed, metersPerBlock)
                : surfaceM - bed >= 0.5f * metersPerBlock;
        boolean withinReach = WATER_REACH_WIDTHS <= 0f
                || (distBlocks >= 0f && distBlocks <= halfWidth * WATER_REACH_WIDTHS);
        if (drawsAWholeBlock && withinReach) {
            out.waterSurfaceM = surfaceM;
        }

        if (inBasin && bed < basinM
                && (!out.hasWater() || basinM > out.waterSurfaceM)) {
            out.waterSurfaceM = basinM;
        }
    }
}
