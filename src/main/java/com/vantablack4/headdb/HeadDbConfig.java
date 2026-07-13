package com.vantablack4.headdb;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

public record HeadDbConfig(
    URI manifestUri,
    String preferredMirrorId,
    Path cacheDirectory,
    boolean loadCachedOnStartup,
    boolean refreshOnStartup,
    int searchResultLimit
) {
    private static final String DEFAULT_MANIFEST_URI = "https://data.headsdb.com/manifest.json";
    private static final String DEFAULT_PREFERRED_MIRROR_ID = "primary";
    private static final int DEFAULT_SEARCH_RESULT_LIMIT = 10;

    public static HeadDbConfig load() {
        Path configDirectory = FabricLoader.getInstance().getConfigDir().resolve(VantablackHeadDbMod.MOD_ID);
        Path configFile = configDirectory.resolve("config.properties");
        Properties properties = new Properties();

        try {
            Files.createDirectories(configDirectory);
            if (Files.isRegularFile(configFile)) {
                try (Reader reader = Files.newBufferedReader(configFile)) {
                    properties.load(reader);
                }
            } else {
                writeDefaultConfig(configFile);
            }
        } catch (IOException exception) {
            VantablackHeadDbMod.LOGGER.warn("Unable to read HeadDB config, using defaults", exception);
        }

        URI manifestUri = URI.create(text(properties, "remote.manifest-url", DEFAULT_MANIFEST_URI));
        String preferredMirrorId = text(properties, "remote.preferred-mirror-id", DEFAULT_PREFERRED_MIRROR_ID);
        Path cacheDirectory = configDirectory.resolve(text(properties, "remote.cache-directory", "cache")).normalize();
        boolean loadCachedOnStartup = bool(properties, "startup.load-cache", true);
        boolean refreshOnStartup = bool(properties, "startup.refresh-remote", true);
        int searchResultLimit = boundedInt(properties, "commands.search-result-limit", DEFAULT_SEARCH_RESULT_LIMIT, 1, 50);

        return new HeadDbConfig(
            manifestUri,
            preferredMirrorId,
            cacheDirectory,
            loadCachedOnStartup,
            refreshOnStartup,
            searchResultLimit
        );
    }

    private static void writeDefaultConfig(Path configFile) throws IOException {
        Properties defaults = new Properties();
        defaults.setProperty("remote.manifest-url", DEFAULT_MANIFEST_URI);
        defaults.setProperty("remote.preferred-mirror-id", DEFAULT_PREFERRED_MIRROR_ID);
        defaults.setProperty("remote.cache-directory", "cache");
        defaults.setProperty("startup.load-cache", "true");
        defaults.setProperty("startup.refresh-remote", "true");
        defaults.setProperty("commands.search-result-limit", Integer.toString(DEFAULT_SEARCH_RESULT_LIMIT));
        try (Writer writer = Files.newBufferedWriter(configFile)) {
            defaults.store(writer, "Vantablack HeadDB configuration");
        }
    }

    private static String text(Properties properties, String key, String fallback) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private static boolean bool(Properties properties, String key, boolean fallback) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "true", "yes", "1", "on" -> true;
            case "false", "no", "0", "off" -> false;
            default -> fallback;
        };
    }

    private static int boundedInt(Properties properties, String key, int fallback, int min, int max) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return Math.max(min, Math.min(max, parsed));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
