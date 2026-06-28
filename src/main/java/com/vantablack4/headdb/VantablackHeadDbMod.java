package com.vantablack4.headdb;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VantablackHeadDbMod implements ModInitializer {
    public static final String MOD_ID = "mod_headdb";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Vantablack HeadDB initialized");
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.SERVER) {
            return;
        }

        HeadDbConfig config = HeadDbConfig.load();
        HeadDbDatabaseService databaseService = HeadDbDatabaseService.create(config, LOGGER);
        databaseService.start();
        new HeadDbCommands(config, databaseService, new FabricHeadItemFactory()).register();
    }
}
