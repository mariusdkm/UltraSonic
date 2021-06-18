package io.github.mariusdkm.ultrasonic;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

public class UltraSonic implements ModInitializer {
    private static final Logger LOGGER = LogManager.getLogger();
    public static Path configDir;
    public static boolean isJsMacrosPresent = FabricLoader.getInstance().isModLoaded("jsmacros");

    @Override
    public void onInitialize() {

        if (isJsMacrosPresent) {
            LOGGER.info("Jsmacros is present");
        } else {
            LOGGER.info("Jsmacros is not present");
        }
    }
}