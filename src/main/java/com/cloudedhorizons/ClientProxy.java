package com.cloudedhorizons;

import net.minecraftforge.client.ClientCommandHandler;

import com.cloudedhorizons.client.CloudsCommand;

import cpw.mods.fml.common.event.FMLInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        ClientCommandHandler.instance.registerCommand(new CloudsCommand());
    }
}
