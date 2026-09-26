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
    /** Noise scale limits in voxels. */
    private static final double MIN_SCALE = 0.1D;
    private static final double MAX_SCALE = 100000.0D;

    private static final int ROW_HEIGHT = 18;
    private static final int FIRST_ROW = 22;

    private int rows;

    public CloudEditorScreen() {
        super(CloudedHorizons.MOD_ID);
    }

    @Override
    public ModularPanel buildUI(ModularGuiContext context) {
        rows = 0;
        ModularPanel panel = ModularPanel.defaultPanel("cloud_editor", 176, FIRST_ROW + 4 * ROW_HEIGHT + 6);
        panel.child(IKey.str("Cloud Editor").asWidget().top(7).left(7));

        addRow(panel, "Height", intField(CloudRenderer::getFieldHeight, CloudRenderer::setFieldHeight));
        addRow(panel, "Scale X", doubleField(CloudRenderer::getNoiseScaleX, CloudRenderer::setNoiseScaleX));
        addRow(panel, "Scale Y", doubleField(CloudRenderer::getNoiseScaleY, CloudRenderer::setNoiseScaleY));
        addRow(panel, "Scale Z", doubleField(CloudRenderer::getNoiseScaleZ, CloudRenderer::setNoiseScaleZ));
        return panel;
    }

    private void addRow(ModularPanel panel, String label, TextFieldWidget field) {
        int top = FIRST_ROW + rows++ * ROW_HEIGHT;
        panel.child(IKey.str(label).asWidget().top(top + 4).left(7));
        panel.child(field.top(top).left(60).size(80, 14));
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

    private static TextFieldWidget doubleField(DoubleSupplier getter, DoubleConsumer setter) {
        LiveField field = new LiveField(text -> {
            double v = Double.parseDouble(text);
            if (!(v >= MIN_SCALE && v <= MAX_SCALE)) return false;
            if (v != getter.getAsDouble()) setter.accept(v);
            return true;
        });
        field.setNumbersDouble(v -> Math.max(MIN_SCALE, Math.min(MAX_SCALE, v)));
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
