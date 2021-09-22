package io.github.mariusdkm.ultrasonic;

import io.github.mariusdkm.ultrasonic.api.Pathing;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.wagyourtail.jsmacros.client.JsMacros;

public class UltraSonic implements ModInitializer {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final boolean isJsMacrosPresent = FabricLoader.getInstance().isModLoaded("jsmacros");

    @Override
    public void onInitialize() {
        LOGGER.atLevel(Level.DEBUG);
        if (isJsMacrosPresent) {
            LOGGER.info("Jsmacros is present");
            JsMacros.core.libraryRegistry.addLibrary(Pathing.class);
        } else {
            LOGGER.info("Jsmacros is not present");
        }
    }
}