package com.cloudedhorizons;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public final class Config {

    private static final String CATEGORY_GENERAL = Configuration.CATEGORY_GENERAL;

    public static boolean enabled = true;

    private Config() {}

    public static void synchronize(File configFile) {
        Configuration configuration = new Configuration(configFile);

        try {
            configuration.load();
            enabled = configuration.getBoolean(
                    "enabled",
                    CATEGORY_GENERAL,
                    enabled,
                    "Whether Clouded Horizons features are enabled.");
        } finally {
            if (configuration.hasChanged()) {
                configuration.save();
            }
        }
    }
}
