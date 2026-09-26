package com.cloudedhorizons.client;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.CustomModularScreen;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;

import com.cloudedhorizons.CloudedHorizons;

/**
 * Cloud editor screen (ModularUI2). For now a small panel that shows the current cloud settings; it will grow into
 * the cloud editor.
 */
public final class CloudEditorScreen extends CustomModularScreen {

    public CloudEditorScreen() {
        super(CloudedHorizons.MOD_ID);
    }

    @Override
    public ModularPanel buildUI(ModularGuiContext context) {
        ModularPanel panel = ModularPanel.defaultPanel("cloud_editor", 176, 80);
        panel.child(IKey.str("Cloud Editor").asWidget().top(7).left(7));
        panel.child(IKey.dynamic(() -> "Height: " + CloudRenderer.getBaseY()).asWidget().top(24).left(7));
        panel.child(IKey.dynamic(() -> "Morph speed: " + CloudRenderer.getMorphSpeed()).asWidget().top(36).left(7));
        return panel;
    }
}
