package com.cloudedhorizons.client;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.CustomModularScreen;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.value.DoubleValue;
import com.cleanroommc.modularui.value.IntValue;
import com.cleanroommc.modularui.widgets.SliderWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

import com.cloudedhorizons.CloudedHorizons;

/**
 * Cloud editor screen (ModularUI2). Each parameter applies to the clouds as soon as it is typed. All sizes are in
 * cloud voxels.
 */
public final class CloudEditorScreen extends CustomModularScreen {

    /** Field height limits in voxels; must match CloudRenderer.MAX_LAYERS. */
    private static final int MIN_HEIGHT = 1;
    private static final int MAX_HEIGHT = 128;
    /** Noise scale limits in voxels. The text field accepts the full range, the slider a practical testing range. */
    private static final double MIN_SCALE = 0.1D;
    private static final double MAX_SCALE = 100000.0D;
    private static final double SLIDER_MIN_SCALE = 1.0D;
    private static final double SLIDER_MAX_SCALE = 256.0D;
    /** Edge fade limits in voxels. */
    private static final double MIN_FADE = 0.0D;
    private static final double MAX_FADE = 128.0D;
    private static final double SLIDER_MAX_FADE = 32.0D;
    /** Noise multiplier / offset limits: the text field accepts a wide range, the slider a practical one. */
    private static final double MIN_MUL = -100.0D;
    private static final double MAX_MUL = 100.0D;
    private static final double SLIDER_MIN_MUL = 0.0D;
    private static final double SLIDER_MAX_MUL = 4.0D;
    private static final double MIN_OFFSET = -100.0D;
    private static final double MAX_OFFSET = 100.0D;
    private static final double SLIDER_MIN_OFFSET = -1.0D;
    private static final double SLIDER_MAX_OFFSET = 1.0D;
    /** Cutoff limits; the noise is roughly -1..1, so values outside that are all or nothing. */
    private static final double MIN_CUTOFF = -1.0D;
    private static final double MAX_CUTOFF = 1.0D;

    private static final int ROW_HEIGHT = 18;
    private static final int FIRST_ROW = 22;

    private int rows;

    public CloudEditorScreen() {
        super(CloudedHorizons.MOD_ID);
    }

    @Override
    public ModularPanel buildUI(ModularGuiContext context) {
        rows = 0;
        ModularPanel panel = ModularPanel.defaultPanel("cloud_editor", 220, FIRST_ROW + 8 * ROW_HEIGHT + 6);
        panel.child(IKey.str("Cloud Editor").asWidget().top(7).left(7));

        addRow(
                panel,
                "Height",
                intField(CloudRenderer::getFieldHeight, CloudRenderer::setFieldHeight),
                slider(
                        MIN_HEIGHT,
                        MAX_HEIGHT,
                        CloudRenderer::getFieldHeight,
                        v -> CloudRenderer.setFieldHeight((int) Math.round(v))));
        addRow(
                panel,
                "Cutoff",
                doubleField(CloudRenderer::getCutoff, CloudRenderer::setCutoff, MIN_CUTOFF, MAX_CUTOFF),
                slider(MIN_CUTOFF, MAX_CUTOFF, CloudRenderer::getCutoff, v -> CloudRenderer.setCutoff(round2(v))));
        addRow(
                panel,
                "Multiplier",
                doubleField(CloudRenderer::getNoiseMultiplier, CloudRenderer::setNoiseMultiplier, MIN_MUL, MAX_MUL),
                slider(
                        SLIDER_MIN_MUL,
                        SLIDER_MAX_MUL,
                        CloudRenderer::getNoiseMultiplier,
                        v -> CloudRenderer.setNoiseMultiplier(round2(v))));
        addRow(
                panel,
                "Offset",
                doubleField(CloudRenderer::getNoiseOffset, CloudRenderer::setNoiseOffset, MIN_OFFSET, MAX_OFFSET),
                slider(
                        SLIDER_MIN_OFFSET,
                        SLIDER_MAX_OFFSET,
                        CloudRenderer::getNoiseOffset,
                        v -> CloudRenderer.setNoiseOffset(round2(v))));
        addRow(
                panel,
                "Edge fade",
                doubleField(CloudRenderer::getEdgeFade, CloudRenderer::setEdgeFade, MIN_FADE, MAX_FADE),
                slider(MIN_FADE, SLIDER_MAX_FADE, CloudRenderer::getEdgeFade, v -> CloudRenderer.setEdgeFade(round2(v))));
        addScaleRow(panel, "Scale X", CloudRenderer::getNoiseScaleX, CloudRenderer::setNoiseScaleX);
        addScaleRow(panel, "Scale Y", CloudRenderer::getNoiseScaleY, CloudRenderer::setNoiseScaleY);
        addScaleRow(panel, "Scale Z", CloudRenderer::getNoiseScaleZ, CloudRenderer::setNoiseScaleZ);
        return panel;
    }

    private void addScaleRow(ModularPanel panel, String label, DoubleSupplier getter, DoubleConsumer setter) {
        addRow(
                panel,
                label,
                doubleField(getter, setter, MIN_SCALE, MAX_SCALE),
                slider(SLIDER_MIN_SCALE, SLIDER_MAX_SCALE, getter, v -> setter.accept(round2(v))));
    }

    /** One parameter row: label, text field for exact values, slider for quick testing. Both edit the same value. */
    private void addRow(ModularPanel panel, String label, TextFieldWidget field, SliderWidget slider) {
        int top = FIRST_ROW + rows++ * ROW_HEIGHT;
        panel.child(IKey.str(label).asWidget().top(top + 4).left(7));
        panel.child(field.top(top).left(52).size(44, 14));
        panel.child(slider.top(top).left(100).size(113, 14));
    }

    /** Rounds slider values to two decimals so the linked text field stays short. */
    private static double round2(double v) {
        return Math.round(v * 100.0D) / 100.0D;
    }

    private static SliderWidget slider(double min, double max, DoubleSupplier getter, DoubleConsumer setter) {
        return new SliderWidget().bounds(min, max).value(new DoubleValue.Dynamic(getter, setter));
    }

    private static TextFieldWidget intField(IntSupplier getter, IntConsumer setter) {
        LiveField field = new LiveField(text -> {
            int v = Integer.parseInt(text);
            if (v < MIN_HEIGHT || v > MAX_HEIGHT) return false;
            if (v != getter.getAsInt()) setter.accept(v);
            return true;
        });
        field.setNumbers(MIN_HEIGHT, MAX_HEIGHT);
        field.value(new IntValue.Dynamic(getter, setter));
        return field;
    }

    private static TextFieldWidget doubleField(DoubleSupplier getter, DoubleConsumer setter, double min, double max) {
        LiveField field = new LiveField(text -> {
            double v = Double.parseDouble(text);
            if (!(v >= min && v <= max)) return false;
            if (v != getter.getAsDouble()) setter.accept(v);
            return true;
        });
        field.setNumbersDouble(v -> Math.max(min, Math.min(max, v)));
        field.value(new DoubleValue.Dynamic(getter, setter));
        return field;
    }

    /**
     * Text field that applies its value on every edit. MUI2's text field normally only writes its value when it
     * loses focus or Enter is pressed, so while focused this hands the text to {@code apply} each update. Text that
     * does not parse or is out of range (e.g. partial input such as "" or "1.") keeps the last valid value.
     */
    private static final class LiveField extends TextFieldWidget {

        private final Predicate<String> apply;

        LiveField(Predicate<String> apply) {
            this.apply = apply;
        }

        @Override
        public void onUpdate() {
            super.onUpdate();
            if (!isFocused()) {
                return;
            }
            try {
                apply.test(getText().trim());
            } catch (NumberFormatException ignored) {
                // Partial input: keep the last valid value.
            }
        }
    }
}
