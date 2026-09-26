package com.cloudedhorizons;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@Mod(
        modid = CloudedHorizons.MOD_ID,
        name = CloudedHorizons.MOD_NAME,
        version = Tags.VERSION,
        acceptedMinecraftVersions = "[1.7.10]",
        dependencies = "required-after:modularui2")
public final class CloudedHorizons {

    public static final String MOD_ID = "cloudedhorizons";
    public static final String MOD_NAME = "Clouded Horizons";
    public static final Logger LOG = LogManager.getLogger(MOD_ID);

    @SidedProxy(
            clientSide = "com.cloudedhorizons.ClientProxy",
            serverSide = "com.cloudedhorizons.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }
}
