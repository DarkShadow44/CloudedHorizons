package com.cloudedhorizons.client;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.CustomModularScreen;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.value.IntValue;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

import com.cloudedhorizons.CloudedHorizons;

/**
 * Cloud editor screen (ModularUI2). Each parameter applies to the clouds as soon as it is typed.
 */
public final class CloudEditorScreen extends CustomModularScreen {

    /** Field height limits in voxels; must match CloudRenderer.MAX_LAYERS. */
    private static final int MIN_HEIGHT = 1;
    private static final int MAX_HEIGHT = 128;

    public CloudEditorScreen() {
        super(CloudedHorizons.MOD_ID);
    }

    @Override
    public ModularPanel buildUI(ModularGuiContext context) {
        ModularPanel panel = ModularPanel.defaultPanel("cloud_editor", 176, 60);
        panel.child(IKey.str("Cloud Editor").asWidget().top(7).left(7));

        panel.child(IKey.str("Height").asWidget().top(26).left(7));
        panel.child(new LiveIntField(CloudRenderer::getFieldHeight, CloudRenderer::setFieldHeight)
                .top(22).left(60).size(80, 14));
        return panel;
    }

    /**
     * Integer field that applies its value on every edit. MUI2's text field normally only writes its value when it
     * loses focus or Enter is pressed, so while focused this parses the text each update and applies it when it is a
     * valid number in range that differs from the current value.
     */
    private static final class LiveIntField extends TextFieldWidget {

        private final IntSupplier getter;
        private final IntConsumer setter;

        LiveIntField(IntSupplier getter, IntConsumer setter) {
            this.getter = getter;
            this.setter = setter;
            setNumbers(MIN_HEIGHT, MAX_HEIGHT);
            value(new IntValue.Dynamic(getter, setter));
        }

        @Override
        public void onUpdate() {
            super.onUpdate();
            if (!isFocused()) {
                return;
            }
            try {
                int v = Integer.parseInt(getText().trim());
                if (v >= MIN_HEIGHT && v <= MAX_HEIGHT && v != getter.getAsInt()) {
                    setter.accept(v);
                }
            } catch (NumberFormatException ignored) {
                // Partial input such as "": keep the last valid value.
            }
        }
    }
}
