package com.github.xandergos.terraindiffusionmc.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TerralithCompat {
    private static final Logger LOG = LoggerFactory.getLogger(TerralithCompat.class);

    public static final String NAMESPACE = "terralith";

    private static volatile boolean active = false;

    private TerralithCompat() {
    }

    public static boolean isActive() {
        return active;
    }

    public static void setActive(boolean value) {
        if (active == value) {
            return;
        }
        active = value;
        LocalTerrainProvider.clearCache();
        LOG.info("Terralith biome palette {}", value ? "enabled" : "disabled");
    }
}
