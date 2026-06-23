package io.github.silentdevelopment.headdb.paper.runtime;

import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.config.PluginConfig;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class StartupChecks {

    private StartupChecks() {}

    public static void run(@NotNull HeadDBPlugin plugin, @NotNull PluginConfig config, @NotNull PluginRuntime runtime) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(runtime, "runtime");

        plugin.getSLF4JLogger().debug("Running HeadDB startup diagnostics.");

        checkManifestUri(config);
        checkCacheDirectory(plugin, config);
        checkRuntime(runtime);

        plugin.getSLF4JLogger().debug("HeadDB startup diagnostics passed.");
    }

    private static void checkManifestUri(@NotNull PluginConfig config) {
        URI manifestUri = config.remoteManifestUri();

        if (!manifestUri.isAbsolute()) {
            throw new IllegalStateException("remote.manifest-url must be absolute: " + manifestUri);
        }

        String scheme = manifestUri.getScheme();
        if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
            throw new IllegalStateException("remote.manifest-url must use http or https: " + manifestUri);
        }
    }

    private static void checkCacheDirectory(@NotNull HeadDBPlugin plugin, @NotNull PluginConfig config) {
        try {
            Path dataDirectory = plugin.getDataFolder().toPath().toAbsolutePath().normalize();
            Path cacheDirectory = config.cacheDirectory(dataDirectory).toAbsolutePath().normalize();

            if (!cacheDirectory.startsWith(dataDirectory)) {
                throw new IllegalStateException("cache.directory must stay inside the plugin data folder: " + cacheDirectory);
            }

            Files.createDirectories(cacheDirectory);

            Path probe = cacheDirectory.resolve(".headdb-write-test");
            Files.writeString(probe, "ok", StandardCharsets.UTF_8);
            Files.deleteIfExists(probe);
        } catch (Exception exception) {
            throw new IllegalStateException("HeadDB cache directory is not usable.", exception);
        }
    }

    private static void checkRuntime(@NotNull PluginRuntime runtime) {
        Objects.requireNonNull(runtime.database(), "database");
        Objects.requireNonNull(runtime.refreshState(), "refreshState");

        if (runtime.closed()) {
            throw new IllegalStateException("Runtime is already closed.");
        }
    }
}