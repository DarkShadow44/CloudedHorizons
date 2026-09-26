package com.cloudedhorizons.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import com.cleanroommc.modularui.factory.ClientGUI;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;

/** Client key bindings. They appear in the vanilla Controls menu, where they can be rebound. */
public final class KeyBindings {

    public static final KeyBinding OPEN_EDITOR = new KeyBinding(
            "key.cloudedhorizons.editor",
            Keyboard.KEY_K,
            "key.categories.cloudedhorizons");

    private KeyBindings() {}

    public static void register() {
        ClientRegistry.registerKeyBinding(OPEN_EDITOR);
        FMLCommonHandler.instance().bus().register(new KeyBindings());
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (OPEN_EDITOR.isPressed() && Minecraft.getMinecraft().currentScreen == null) {
            ClientGUI.open(new CloudEditorScreen());
        }
    }
}
