package com.github.xandergos.terraindiffusionmc.pipeline;

import static com.github.xandergos.terraindiffusionmc.pipeline.TerralithBiomeIds.*;

public final class TerralithClassifier {
    public static final short NONE = 0;

    private static final float UPLAND_M = 200f;
    private static final float HIGHLAND_M = 700f;
    private static final float MONTANE_M = 1500f;
    private static final float ALPINE_M = 2500f;

    private static final FastNoiseLite VARIANT_BROAD = makeFnl(0x7A1E, 1f / 3000f, 2, 2f, 0.5f);

    private static final FastNoiseLite VARIANT_LOCAL = makeFnl(0x5C0D, 1f / 1200f, 2, 2f, 0.5f);

    private static final FastNoiseLite SPECIAL = makeFnl(0x9E37, 1f / 1800f, 3, 2f, 0.5f);

    private static final FastNoiseLite VOLCANIC = makeFnl(0x1F55, 1f / 5000f, 2, 2f, 0.5f);

    private static final FastNoiseLite SHOWPIECE_PICK = makeFnl(0x2B7F, 1f / 14000f, 2, 2f, 0.5f);

    private static final float Q_TOP_04 = 0.702f;
    private static final float Q_TOP_10 = 0.632f;
    private static final float Q_TOP_20 = 0.598f;
    private static final float Q_TOP_25 = 0.579f;
    private static final float Q_TOP_33 = 0.547f;
    private static final float Q_MEDIAN = 0.500f;
    private static final float Q_BOT_33 = 0.453f;
    private static final float Q_BOT_25 = 0.421f;

    private static final float SNOW_MAX_C = -5f;

    private static final float ARID_MIN_C = 30f;

    private static final float PLATEAU_MAX_SLOPE = 0.18f;

    private static final float SPECIAL_THRESHOLD = Q_TOP_10;

    private static final float VOLCANIC_THRESHOLD = Q_TOP_04;

    private static final float SHOWPIECE_THRESHOLD = 0.665f;

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

    private TerralithClassifier() {
    }

    private static float v01(FastNoiseLite noise, TerrainSample s) {
        return (noise.GetNoise(s.worldX, s.worldZ) + 1f) * 0.5f;
    }

    private static boolean isSpecial(TerrainSample s) {
        return v01(SPECIAL, s) > SPECIAL_THRESHOLD;
    }

    private static boolean isVolcanic(TerrainSample s) {
        return v01(VOLCANIC, s) > VOLCANIC_THRESHOLD;
    }

    private static boolean isSnowy(TerrainSample s) {
        return s.hasSnow && s.temp <= SNOW_MAX_C;
    }

    private static boolean isHotArid(TerrainSample s) {
        return summerMaxC(s) > ARID_MIN_C;
    }

    private static float summerMaxC(TerrainSample s) {
        return s.temp + 1.414f * s.tStd;
    }

    public static short pick(TerrainSample s) {
        if (s.isOcean) {
            return ocean(s);
        }

        short coast = beach(s);
        if (coast != NONE) {
            return coast;
        }

        if (s.slopeBare) {
            return cliff(s);
        }

        if (s.altM > HIGHLAND_M && s.slope < PLATEAU_MAX_SLOPE) {
            short tableland = plateau(s);
            if (tableland != NONE) {
                return tableland;
            }
        }

        if (s.altM > ALPINE_M) return alpine(s);
        if (s.altM > MONTANE_M) return montane(s);
        if (s.altM > HIGHLAND_M) return highland(s);
        if (s.altM > UPLAND_M) return upland(s);
        return lowland(s);
    }

    private static short plateau(TerrainSample s) {
        if (isVolcanic(s) || (s.aridity < 0.12f && isHotArid(s))) {
            return NONE;
        }

        boolean open = s.treesNone || s.treesSparse;

        if (isSnowy(s)) {
            if (!open) return SIBERIAN_GROVE;

            if (isSpecial(s)) {
                return v01(VARIANT_LOCAL, s) > Q_MEDIAN ? SNOWY_BADLANDS : GLACIAL_CHASM;
            }
            return v01(VARIANT_BROAD, s) > Q_TOP_33 ? ICE_MARSH : SNOWY_SHIELD;
        }

        if (s.aridity < 0.18f) {
            if (s.hot || s.warm) {
                return isSpecial(s) ? WARPED_MESA : WHITE_MESA;
            }
            return ARID_HIGHLANDS;
        }

        if ((s.hot || s.warm) && open && s.aridity < 0.45f) {
            return SAVANNA_BADLANDS;
        }

        short showpiece = showpiece(s, false);
        if (showpiece != NONE) {
            return showpiece;
        }

        if (s.temp <= SNOW_MAX_C) {
            return open ? GRAVEL_DESERT : SIBERIAN_TAIGA;
        }

        if (open) {
            if (s.cold || s.cool) {
                return v01(VARIANT_BROAD, s) > Q_TOP_33 ? HIGHLANDS : BiomeClassifier.MEADOW;
            }
            if (s.temperate) {
                return v01(VARIANT_BROAD, s) > Q_MEDIAN ? BiomeClassifier.MEADOW : HIGHLANDS;
            }
            return HIGHLANDS;
        }

        if (s.cold || s.cool) {
            return v01(VARIANT_BROAD, s) > Q_MEDIAN ? SHIELD : FORESTED_HIGHLANDS;
        }
        return v01(VARIANT_BROAD, s) > Q_TOP_33 ? TEMPERATE_HIGHLANDS : FORESTED_HIGHLANDS;
    }

    private static short ocean(TerrainSample s) {
        if (s.warm || s.hot) {
            if (s.elev < -600f) {
                return DEEP_WARM_OCEAN;
            }

            if (s.elev > -120f) {
                return BiomeClassifier.OCEAN;
            }
        }
        return NONE;
    }

    private static short beach(TerrainSample s) {
        boolean shore = s.elev >= 0f && s.elev < 8f && s.slope < 0.20f;
        if (shore && (s.frozen || s.cold || s.cool)) {
            return GRAVEL_BEACH;
        }
        return NONE;
    }

    private static short cliff(TerrainSample s) {
        if (s.frozen || s.hasSnow) {
            if (isSpecial(s) && s.altM > MONTANE_M) return GLACIAL_CHASM;

            return s.altM > 3200f ? NONE : FROZEN_CLIFFS;
        }

        if ((s.hot || s.warm) && s.aridity < 0.12f) {
            return s.altM > 900f ? BRYCE_CANYON : DESERT_CANYON;
        }

        if (s.hot && s.treeMoisture > 0.8f) {
            return isSpecial(s) ? AMETHYST_CANYON : ROCKY_JUNGLE;
        }

        if (isVolcanic(s) && (s.warm || s.hot)) {
            return BASALT_CLIFFS;
        }

        if (s.altM > MONTANE_M) return WINDSWEPT_SPIRES;
        if (s.cool || s.cold) return STONY_SPIRES;

        float v = v01(VARIANT_LOCAL, s);
        if (v < Q_BOT_33) return GRANITE_CLIFFS;
        if (v < Q_TOP_33) return YOSEMITE_CLIFFS;
        return WHITE_CLIFFS;
    }

    private static short alpine(TerrainSample s) {
        if (isVolcanic(s) && !s.frozen) {
            float v = v01(VARIANT_LOCAL, s);
            if (v > Q_TOP_10) return CALDERA;
            if (v > Q_TOP_33) return VOLCANIC_CRATER;
            return VOLCANIC_PEAKS;
        }

        if (s.hasSnow) {
            return NONE;
        }

        if (s.aridity < 0.18f) {
            return PAINTED_MOUNTAINS;
        }

        float v = v01(VARIANT_BROAD, s);

        if (s.temp <= SNOW_MAX_C) {
            return v > Q_TOP_33 ? EMERALD_PEAKS : SCARLET_MOUNTAINS;
        }
        if (s.cool || s.temperate) {
            return ROCKY_MOUNTAINS;
        }
        return PAINTED_MOUNTAINS;
    }

    private static short montane(TerrainSample s) {
        if (isVolcanic(s) && !s.frozen && !s.cold) {
            return v01(VARIANT_LOCAL, s) > Q_TOP_33 ? VOLCANIC_CRATER : VOLCANIC_PEAKS;
        }

        if (isSpecial(s) && (s.cool || s.cold) && s.precip > 500f && !s.slopeMedium) {
            return YELLOWSTONE;
        }

        if (s.temp >= 18f && (s.treesDense || s.treesRainforest)) {
            return JUNGLE_MOUNTAINS;
        }

        if ((s.cool || s.temperate) && s.precip > 1800f && s.treeMoisture > 0.8f) {
            return CLOUD_FOREST;
        }

        if (isSnowy(s)) {
            return s.treesNone ? NONE : ALPINE_GROVE;
        }

        if (s.aridity < 0.20f) {
            return s.hot || s.warm ? PAINTED_MOUNTAINS : ARID_HIGHLANDS;
        }

        if (s.temp <= SNOW_MAX_C) {
            return s.treesNone ? ROCKY_MOUNTAINS : SIBERIAN_GROVE;
        }
        return s.treesNone ? ROCKY_MOUNTAINS : HAZE_MOUNTAIN;
    }

    private static short highland(TerrainSample s) {
        if (isVolcanic(s) && s.aridity < 0.35f && (s.hot || s.warm)) {
            return ASHEN_SAVANNA;
        }

        if (isSnowy(s)) {
            return s.treesNone ? SNOWY_SHIELD : SIBERIAN_GROVE;
        }

        if (s.aridity < 0.18f) {
            if (s.temp <= SNOW_MAX_C) return GRAVEL_DESERT;
            if (s.hot || s.warm) {
                float v = v01(VARIANT_LOCAL, s);
                if (isSpecial(s)) return WARPED_MESA;
                return v > Q_TOP_33 ? WHITE_MESA : ARID_HIGHLANDS;
            }

            return ARID_HIGHLANDS;
        }

        if ((s.hot || s.warm) && (s.treesNone || s.treesSparse) && s.aridity < 0.45f) {
            if (s.slopeMedium) return SAVANNA_SLOPES;
            return isSpecial(s) ? SAVANNA_BADLANDS : FRACTURED_SAVANNA;
        }

        if ((s.hot || s.warm) && (s.treesDense || s.treesRainforest)) {
            return isSpecial(s) ? AMETHYST_RAINFOREST : ROCKY_JUNGLE;
        }

        short showpiece = showpiece(s, false);
        if (showpiece != NONE) {
            return showpiece;
        }

        if (s.temp <= SNOW_MAX_C) {
            return s.treesNone ? SNOWY_SHIELD : SIBERIAN_TAIGA;
        }
        if (s.cold || s.cool) {
            if (s.treesNone) return ALPINE_HIGHLANDS;
            return v01(VARIANT_BROAD, s) > Q_MEDIAN ? SHIELD : FORESTED_HIGHLANDS;
        }

        if (s.treesNone) {
            return v01(VARIANT_BROAD, s) > Q_MEDIAN ? HIGHLANDS : ALPINE_HIGHLANDS;
        }
        if (s.treesSparse) {
            return TEMPERATE_HIGHLANDS;
        }
        return FORESTED_HIGHLANDS;
    }

    private static short upland(TerrainSample s) {
        if (isSnowy(s)) {
            if (s.treesNone) return isSpecial(s) ? SNOWY_BADLANDS : SNOWY_SHIELD;
            return v01(VARIANT_LOCAL, s) > Q_TOP_20 ? SNOWY_MAPLE_FOREST : WINTRY_FOREST;
        }

        if (s.aridity < 0.15f) {
            if (s.temp <= SNOW_MAX_C) return GRAVEL_DESERT;

            if (isSpecial(s) && (s.hot || s.warm)) {
                return v01(VARIANT_LOCAL, s) > Q_MEDIAN ? WARPED_MESA : WHITE_MESA;
            }
            if (isHotArid(s)) {
                if (s.slopeMedium) return DESERT_SPIRES;

                if (s.precip > 150f) return LUSH_DESERT;
                return SANDSTONE_VALLEY;
            }

            return ARID_HIGHLANDS;
        }

        if (s.treesNone || (s.treesSparse && s.aridity < 0.40f)) {
            if (s.temp <= SNOW_MAX_C) {
                return v01(VARIANT_LOCAL, s) > Q_MEDIAN ? COLD_SHRUBLAND : ROCKY_SHRUBLAND;
            }
            if (isHotArid(s)) {
                if (s.slopeMedium) return SAVANNA_SLOPES;
                if (isSpecial(s)) return BRUSHLAND;
                return v01(VARIANT_BROAD, s) > Q_MEDIAN ? HOT_SHRUBLAND : SHRUBLAND;
            }
            return STEPPE;
        }

        if (s.hot && (s.treesDense || s.treesRainforest)) {
            return s.slopeMedium ? ROCKY_JUNGLE : TROPICAL_JUNGLE;
        }

        short showpiece = showpiece(s, false);
        if (showpiece != NONE) {
            return showpiece;
        }

        if (s.temp <= SNOW_MAX_C) {
            return v01(VARIANT_BROAD, s) > Q_MEDIAN ? SIBERIAN_TAIGA : WINTRY_FOREST;
        }
        if (s.cold || s.cool) {
            return v01(VARIANT_BROAD, s) > Q_MEDIAN ? BIRCH_TAIGA : SHIELD;
        }
        if (s.temperate) {
            return v01(VARIANT_BROAD, s) > Q_TOP_20 ? SHIELD_CLEARING : YOSEMITE_LOWLANDS;
        }
        return NONE;
    }

    private static short lowland(TerrainSample s) {
        if (s.precip > 1200f && s.slope < 0.10f && s.treeMoisture > 0.9f) {
            if (s.temp <= SNOW_MAX_C) return ICE_MARSH;
            if (s.warm || s.hot) return ORCHID_SWAMP;
        }

        if (isSnowy(s)) {
            if (s.treesNone) return isSpecial(s) ? SNOWY_BADLANDS : WINTRY_LOWLANDS;
            if (isSpecial(s)) return SNOWY_CHERRY_GROVE;
            return v01(VARIANT_LOCAL, s) > Q_TOP_20 ? SNOWY_MAPLE_FOREST : WINTRY_FOREST;
        }

        if (s.aridity < 0.12f && isHotArid(s)) {
            if (isSpecial(s)) {
                return v01(VARIANT_LOCAL, s) > Q_MEDIAN ? RED_OASIS : DESERT_OASIS;
            }
            return s.precip > 150f ? SANDSTONE_VALLEY : ANCIENT_SANDS;
        }
        if (s.aridity < 0.15f && s.temp <= SNOW_MAX_C) {
            return GRAVEL_DESERT;
        }

        if (s.hot && (s.treesDense || s.treesRainforest)) {
            return TROPICAL_JUNGLE;
        }

        if (s.treesNone || (s.treesSparse && s.aridity < 0.35f)) {
            if (s.temp <= SNOW_MAX_C) return COLD_SHRUBLAND;
            if (isHotArid(s)) {
                return v01(VARIANT_BROAD, s) > Q_MEDIAN ? HOT_SHRUBLAND : SHRUBLAND;
            }
            return STEPPE;
        }

        short showpiece = showpiece(s, true);
        if (showpiece != NONE) {
            return showpiece;
        }

        if (s.temp <= SNOW_MAX_C) {
            return v01(VARIANT_BROAD, s) > Q_TOP_33 ? WINTRY_LOWLANDS : SIBERIAN_TAIGA;
        }
        if (s.cold || s.cool) {
            return v01(VARIANT_BROAD, s) > Q_MEDIAN ? BIRCH_TAIGA : VALLEY_CLEARING;
        }
        if (s.temperate) {
            return v01(VARIANT_BROAD, s) > Q_MEDIAN ? LUSH_VALLEY : YOSEMITE_LOWLANDS;
        }
        return NONE;
    }

    private static short showpiece(TerrainSample s, boolean valley) {
        if (v01(SPECIAL, s) <= SHOWPIECE_THRESHOLD) return NONE;
        if (!(s.temperate || s.cool)) return NONE;
        if (s.treesNone || s.treeMoisture < 0.5f) return NONE;

        float which = v01(SHOWPIECE_PICK, s);
        if (which < Q_BOT_25) return valley ? LAVENDER_VALLEY : LAVENDER_FOREST;
        if (which < Q_MEDIAN) return valley ? SAKURA_VALLEY : SAKURA_GROVE;
        if (which < Q_TOP_25) return valley ? MOONLIGHT_VALLEY : MOONLIGHT_GROVE;
        return valley ? BLOOMING_VALLEY : BLOOMING_PLATEAU;
    }
}
