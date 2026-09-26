package com.cloudedhorizons;

import java.io.File;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

public final class Config {

    private static final String CATEGORY_GENERAL = Configuration.CATEGORY_GENERAL;
    private static final String CATEGORY_CLOUDS = "clouds";

    public static boolean enabled = true;
    /** World Y of the cloud volume's bottom face. */
    public static double cloudHeight = 160.0D;
    /** Height of the cloud field in voxels; the field is cut off below and above it. */
    public static int fieldHeight = 24;
    /** Noise time-axis units per second of game time. */
    public static double morphSpeed = 0.02D;

    private static Configuration configuration;

    private Config() {}

    public static void synchronize(File configFile) {
        configuration = new Configuration(configFile);
        try {
            configuration.load();
            enabled = configuration.getBoolean(
                    "enabled",
                    CATEGORY_GENERAL,
                    enabled,
                    "Whether Clouded Horizons features are enabled.");
            cloudHeight = cloudHeightProperty().getDouble();
            morphSpeed = morphSpeedProperty().getDouble();
            fieldHeight = fieldHeightProperty().getInt();
        } finally {
            if (configuration.hasChanged()) {
                configuration.save();
            }
        }
    }

    /** Writes the runtime-adjustable cloud settings back to the config file. */
    public static void saveClouds() {
        if (configuration == null) {
            return;
        }
        cloudHeightProperty().set(cloudHeight);
        morphSpeedProperty().set(morphSpeed);
        fieldHeightProperty().set(fieldHeight);
        configuration.save();
    }

    private static Property cloudHeightProperty() {
        return configuration.get(CATEGORY_CLOUDS, "height", 160.0D, "World Y of the bottom of the cloud layer.");
    }

    private static Property fieldHeightProperty() {
        return configuration.get(
                CATEGORY_CLOUDS,
                "fieldHeight",
                24,
                "Height of the cloud field in voxels. Clouds are cut off below and above it.");
    }

    private static Property morphSpeedProperty() {
        return configuration.get(
                CATEGORY_CLOUDS,
                "morphSpeed",
                0.02D,
                "How fast the cloud field morphs, in noise time units per second. 0 freezes the clouds.");
    }
}
