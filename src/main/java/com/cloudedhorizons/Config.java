package com.cloudedhorizons;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.minecraftforge.common.config.ConfigCategory;
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
    /** Maximum number of noise layers; must match MAX_NOISE_LAYERS in generate.fsh. */
    public static final int MAX_NOISE_LAYERS = 8;
    /** Noise layers, summed before the cutoff test. Always holds at least one layer. */
    public static final List<NoiseLayer> noiseLayers = new ArrayList<>();
    /** Threshold applied to the summed layers: a voxel is solid where the sum is above it. */
    public static double cutoff = 0.3D;
    /** Distance in voxels from the field's top and bottom over which clouds thin out; 0 = hard cut. */
    public static double edgeFade = 0.0D;
    /** Width below the cutoff, in summed-noise units, of the translucent shell around the clouds; 0 = hard cut. */
    public static double softness = 0.1D;
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
            edgeFade = edgeFadeProperty().getDouble();
            softness = softnessProperty().getDouble();
            loadNoiseLayers();
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
        edgeFadeProperty().set(edgeFade);
        softnessProperty().set(softness);
        saveNoiseLayers();
        configuration.save();
    }

    /**
     * Reads the layers from categories {@code clouds.layer1..N}. Configs from before layers existed kept a single
     * layer's settings directly in {@code clouds}; those become layer 1 and the old keys are removed.
     */
    private static void loadNoiseLayers() {
        ConfigCategory clouds = configuration.getCategory(CATEGORY_CLOUDS);
        NoiseLayer legacy = new NoiseLayer();
        legacy.scaleX = takeLegacy(clouds, "noiseScaleX", legacy.scaleX);
        legacy.scaleY = takeLegacy(clouds, "noiseScaleY", legacy.scaleY);
        legacy.scaleZ = takeLegacy(clouds, "noiseScaleZ", legacy.scaleZ);
        legacy.multiplier = takeLegacy(clouds, "noiseMultiplier", legacy.multiplier);
        legacy.offset = takeLegacy(clouds, "noiseOffset", legacy.offset);

        int count = Math.max(1, Math.min(MAX_NOISE_LAYERS, layerCountProperty().getInt()));
        noiseLayers.clear();
        for (int i = 0; i < count; i++) {
            Property[] p = layerProperties(i, i == 0 ? legacy : new NoiseLayer());
            NoiseLayer layer = new NoiseLayer();
            layer.scaleX = p[0].getDouble();
            layer.scaleY = p[1].getDouble();
            layer.scaleZ = p[2].getDouble();
            layer.multiplier = p[3].getDouble();
            layer.offset = p[4].getDouble();
            noiseLayers.add(layer);
        }
    }

    private static double takeLegacy(ConfigCategory clouds, String key, double fallback) {
        Property property = clouds.remove(key);
        return property != null ? property.getDouble(fallback) : fallback;
    }

    private static void saveNoiseLayers() {
        layerCountProperty().set(noiseLayers.size());
        for (int i = 0; i < noiseLayers.size(); i++) {
            NoiseLayer layer = noiseLayers.get(i);
            Property[] p = layerProperties(i, new NoiseLayer());
            p[0].set(layer.scaleX);
            p[1].set(layer.scaleY);
            p[2].set(layer.scaleZ);
            p[3].set(layer.multiplier);
            p[4].set(layer.offset);
        }
        // Drop categories of removed layers.
        for (int i = noiseLayers.size(); i < MAX_NOISE_LAYERS; i++) {
            String category = layerCategory(i);
            if (configuration.hasCategory(category)) {
                configuration.removeCategory(configuration.getCategory(category));
            }
        }
    }

    /** Properties of layer {@code index}: scaleX, scaleY, scaleZ, multiplier, offset. */
    private static Property[] layerProperties(int index, NoiseLayer defaults) {
        String c = layerCategory(index);
        return new Property[] {
                configuration.get(c, "scaleX", defaults.scaleX, "Noise feature size along X, in voxels."),
                configuration.get(c, "scaleY", defaults.scaleY, "Noise feature size along Y, in voxels."),
                configuration.get(c, "scaleZ", defaults.scaleZ, "Noise feature size along Z, in voxels."),
                configuration.get(c, "multiplier", defaults.multiplier, "The layer's noise is multiplied by this."),
                configuration.get(c, "offset", defaults.offset, "Added to the layer's noise after the multiplier."), };
    }

    private static String layerCategory(int index) {
        return CATEGORY_CLOUDS + Configuration.CATEGORY_SPLITTER + "layer" + (index + 1);
    }

    private static Property layerCountProperty() {
        return configuration.get(
                CATEGORY_CLOUDS,
                "layerCount",
                1,
                "Number of noise layers (1.." + MAX_NOISE_LAYERS + "). Their values are summed before the cutoff test.");
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
                "Threshold for the summed noise layers. Voxels above it are cloud; higher values mean fewer clouds.");
    }

    private static Property softnessProperty() {
        return configuration.get(
                CATEGORY_CLOUDS,
                "softness",
                0.1D,
                "Width below the cutoff of the translucent shell around the clouds, in summed-noise units. "
                        + "0 means no shell.");
    }

    private static Property edgeFadeProperty() {
        return configuration.get(
                CATEGORY_CLOUDS,
                "edgeFade",
                0.0D,
                "Distance in voxels from the top and bottom of the cloud field over which clouds thin out and round off. "
                        + "0 cuts them off flat.");
    }

    private static Property morphSpeedProperty() {
        return configuration.get(
                CATEGORY_CLOUDS,
                "morphSpeed",
                0.02D,
                "How fast the cloud field morphs, in noise time units per second. 0 freezes the clouds.");
    }
}
