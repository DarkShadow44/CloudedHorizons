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
    /** Noise feature size along each axis, in voxels. */
    public static double noiseScaleX = 32.0D;
    public static double noiseScaleY = 32.0D;
    public static double noiseScaleZ = 32.0D;
    /** Noise threshold: a voxel is solid where the noise (roughly -1..1) is above it. */
    public static double cutoff = 0.3D;
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
            cutoff = cutoffProperty().getDouble();
            noiseScaleX = noiseScaleProperty("X").getDouble();
            noiseScaleY = noiseScaleProperty("Y").getDouble();
            noiseScaleZ = noiseScaleProperty("Z").getDouble();
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
        cutoffProperty().set(cutoff);
        noiseScaleProperty("X").set(noiseScaleX);
        noiseScaleProperty("Y").set(noiseScaleY);
        noiseScaleProperty("Z").set(noiseScaleZ);
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

    private static Property cutoffProperty() {
        return configuration.get(
                CATEGORY_CLOUDS,
                "cutoff",
                0.3D,
                "Noise threshold (noise is roughly -1..1). Voxels above it are cloud; higher values mean fewer clouds.");
    }

    private static Property noiseScaleProperty(String axis) {
        return configuration.get(
                CATEGORY_CLOUDS,
                "noiseScale" + axis,
                32.0D,
                "Noise feature size along " + axis + ", in voxels. Larger values stretch the clouds along that axis.");
    }

    private static Property morphSpeedProperty() {
        return configuration.get(
                CATEGORY_CLOUDS,
                "morphSpeed",
                0.02D,
                "How fast the cloud field morphs, in noise time units per second. 0 freezes the clouds.");
    }
}
