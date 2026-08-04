package com.github.xandergos.terraindiffusionmc.pipeline;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TerralithBiomeIds {
    public static final short DEEP_WARM_OCEAN = 200;
    public static final short GRAVEL_BEACH = 201;
    public static final short WARM_RIVER = 202;

    public static final short FROZEN_CLIFFS = 210;
    public static final short GRANITE_CLIFFS = 211;
    public static final short WHITE_CLIFFS = 212;
    public static final short YOSEMITE_CLIFFS = 213;
    public static final short BASALT_CLIFFS = 214;
    public static final short STONY_SPIRES = 215;
    public static final short WINDSWEPT_SPIRES = 216;
    public static final short DESERT_SPIRES = 217;
    public static final short DESERT_CANYON = 218;
    public static final short BRYCE_CANYON = 219;
    public static final short AMETHYST_CANYON = 220;
    public static final short GLACIAL_CHASM = 221;

    public static final short VOLCANIC_PEAKS = 230;
    public static final short VOLCANIC_CRATER = 231;
    public static final short CALDERA = 232;
    public static final short EMERALD_PEAKS = 233;
    public static final short SCARLET_MOUNTAINS = 234;
    public static final short ROCKY_MOUNTAINS = 235;
    public static final short PAINTED_MOUNTAINS = 236;
    public static final short HAZE_MOUNTAIN = 237;
    public static final short JUNGLE_MOUNTAINS = 238;
    public static final short ALPINE_GROVE = 239;
    public static final short CLOUD_FOREST = 240;
    public static final short YELLOWSTONE = 241;

    public static final short ALPINE_HIGHLANDS = 250;
    public static final short TEMPERATE_HIGHLANDS = 251;
    public static final short FORESTED_HIGHLANDS = 252;
    public static final short ARID_HIGHLANDS = 253;
    public static final short HIGHLANDS = 254;
    public static final short BLOOMING_PLATEAU = 255;
    public static final short SHIELD = 256;
    public static final short SHIELD_CLEARING = 257;
    public static final short SNOWY_SHIELD = 258;
    public static final short SIBERIAN_GROVE = 259;
    public static final short ROCKY_SHRUBLAND = 260;

    public static final short SAVANNA_SLOPES = 270;
    public static final short SAVANNA_BADLANDS = 271;
    public static final short ASHEN_SAVANNA = 272;
    public static final short FRACTURED_SAVANNA = 273;
    public static final short BRUSHLAND = 274;
    public static final short SHRUBLAND = 275;
    public static final short HOT_SHRUBLAND = 276;
    public static final short COLD_SHRUBLAND = 277;
    public static final short STEPPE = 278;

    public static final short ANCIENT_SANDS = 290;
    public static final short LUSH_DESERT = 291;
    public static final short DESERT_OASIS = 292;
    public static final short RED_OASIS = 293;
    public static final short SANDSTONE_VALLEY = 294;
    public static final short WHITE_MESA = 295;
    public static final short WARPED_MESA = 296;
    public static final short GRAVEL_DESERT = 297;
    public static final short SNOWY_BADLANDS = 298;

    public static final short TROPICAL_JUNGLE = 310;
    public static final short ROCKY_JUNGLE = 311;
    public static final short AMETHYST_RAINFOREST = 312;

    public static final short YOSEMITE_LOWLANDS = 320;
    public static final short LUSH_VALLEY = 321;
    public static final short VALLEY_CLEARING = 322;
    public static final short BIRCH_TAIGA = 323;
    public static final short LAVENDER_FOREST = 324;
    public static final short LAVENDER_VALLEY = 325;
    public static final short SAKURA_GROVE = 326;
    public static final short SAKURA_VALLEY = 327;
    public static final short MOONLIGHT_GROVE = 328;
    public static final short MOONLIGHT_VALLEY = 329;
    public static final short BLOOMING_VALLEY = 330;
    public static final short ORCHID_SWAMP = 331;

    public static final short SIBERIAN_TAIGA = 340;
    public static final short WINTRY_FOREST = 341;
    public static final short WINTRY_LOWLANDS = 342;
    public static final short SNOWY_MAPLE_FOREST = 343;
    public static final short SNOWY_CHERRY_GROVE = 344;
    public static final short ICE_MARSH = 345;

    private static final Map<Short, String> PATHS;

    static {
        Map<Short, String> paths = new LinkedHashMap<>();
        paths.put(DEEP_WARM_OCEAN, "deep_warm_ocean");
        paths.put(GRAVEL_BEACH, "gravel_beach");
        paths.put(WARM_RIVER, "warm_river");

        paths.put(FROZEN_CLIFFS, "frozen_cliffs");
        paths.put(GRANITE_CLIFFS, "granite_cliffs");
        paths.put(WHITE_CLIFFS, "white_cliffs");
        paths.put(YOSEMITE_CLIFFS, "yosemite_cliffs");
        paths.put(BASALT_CLIFFS, "basalt_cliffs");
        paths.put(STONY_SPIRES, "stony_spires");
        paths.put(WINDSWEPT_SPIRES, "windswept_spires");
        paths.put(DESERT_SPIRES, "desert_spires");
        paths.put(DESERT_CANYON, "desert_canyon");
        paths.put(BRYCE_CANYON, "bryce_canyon");
        paths.put(AMETHYST_CANYON, "amethyst_canyon");
        paths.put(GLACIAL_CHASM, "glacial_chasm");

        paths.put(VOLCANIC_PEAKS, "volcanic_peaks");
        paths.put(VOLCANIC_CRATER, "volcanic_crater");
        paths.put(CALDERA, "caldera");
        paths.put(EMERALD_PEAKS, "emerald_peaks");
        paths.put(SCARLET_MOUNTAINS, "scarlet_mountains");
        paths.put(ROCKY_MOUNTAINS, "rocky_mountains");
        paths.put(PAINTED_MOUNTAINS, "painted_mountains");
        paths.put(HAZE_MOUNTAIN, "haze_mountain");
        paths.put(JUNGLE_MOUNTAINS, "jungle_mountains");
        paths.put(ALPINE_GROVE, "alpine_grove");
        paths.put(CLOUD_FOREST, "cloud_forest");
        paths.put(YELLOWSTONE, "yellowstone");

        paths.put(ALPINE_HIGHLANDS, "alpine_highlands");
        paths.put(TEMPERATE_HIGHLANDS, "temperate_highlands");
        paths.put(FORESTED_HIGHLANDS, "forested_highlands");
        paths.put(ARID_HIGHLANDS, "arid_highlands");
        paths.put(HIGHLANDS, "highlands");
        paths.put(BLOOMING_PLATEAU, "blooming_plateau");
        paths.put(SHIELD, "shield");
        paths.put(SHIELD_CLEARING, "shield_clearing");
        paths.put(SNOWY_SHIELD, "snowy_shield");
        paths.put(SIBERIAN_GROVE, "siberian_grove");
        paths.put(ROCKY_SHRUBLAND, "rocky_shrubland");

        paths.put(SAVANNA_SLOPES, "savanna_slopes");
        paths.put(SAVANNA_BADLANDS, "savanna_badlands");
        paths.put(ASHEN_SAVANNA, "ashen_savanna");
        paths.put(FRACTURED_SAVANNA, "fractured_savanna");
        paths.put(BRUSHLAND, "brushland");
        paths.put(SHRUBLAND, "shrubland");
        paths.put(HOT_SHRUBLAND, "hot_shrubland");
        paths.put(COLD_SHRUBLAND, "cold_shrubland");
        paths.put(STEPPE, "steppe");

        paths.put(ANCIENT_SANDS, "ancient_sands");
        paths.put(LUSH_DESERT, "lush_desert");
        paths.put(DESERT_OASIS, "desert_oasis");
        paths.put(RED_OASIS, "red_oasis");
        paths.put(SANDSTONE_VALLEY, "sandstone_valley");
        paths.put(WHITE_MESA, "white_mesa");
        paths.put(WARPED_MESA, "warped_mesa");
        paths.put(GRAVEL_DESERT, "gravel_desert");
        paths.put(SNOWY_BADLANDS, "snowy_badlands");

        paths.put(TROPICAL_JUNGLE, "tropical_jungle");
        paths.put(ROCKY_JUNGLE, "rocky_jungle");
        paths.put(AMETHYST_RAINFOREST, "amethyst_rainforest");

        paths.put(YOSEMITE_LOWLANDS, "yosemite_lowlands");
        paths.put(LUSH_VALLEY, "lush_valley");
        paths.put(VALLEY_CLEARING, "valley_clearing");
        paths.put(BIRCH_TAIGA, "birch_taiga");
        paths.put(LAVENDER_FOREST, "lavender_forest");
        paths.put(LAVENDER_VALLEY, "lavender_valley");
        paths.put(SAKURA_GROVE, "sakura_grove");
        paths.put(SAKURA_VALLEY, "sakura_valley");
        paths.put(MOONLIGHT_GROVE, "moonlight_grove");
        paths.put(MOONLIGHT_VALLEY, "moonlight_valley");
        paths.put(BLOOMING_VALLEY, "blooming_valley");
        paths.put(ORCHID_SWAMP, "orchid_swamp");

        paths.put(SIBERIAN_TAIGA, "siberian_taiga");
        paths.put(WINTRY_FOREST, "wintry_forest");
        paths.put(WINTRY_LOWLANDS, "wintry_lowlands");
        paths.put(SNOWY_MAPLE_FOREST, "snowy_maple_forest");
        paths.put(SNOWY_CHERRY_GROVE, "snowy_cherry_grove");
        paths.put(ICE_MARSH, "ice_marsh");

        PATHS = Collections.unmodifiableMap(paths);
    }

    private TerralithBiomeIds() {
    }

    public static Map<Short, String> paths() {
        return PATHS;
    }

    public static boolean isTerralith(short biomeId) {
        return PATHS.containsKey(biomeId);
    }
}
