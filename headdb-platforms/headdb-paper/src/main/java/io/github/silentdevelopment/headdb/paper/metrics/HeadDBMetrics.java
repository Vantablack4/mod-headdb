package io.github.silentdevelopment.headdb.paper.metrics;

import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class HeadDBMetrics {

    /*
     * Replace this after registering HeadDB on bStats.
     */
    private static final int BSTATS_PLUGIN_ID = 9152;
    private static Metrics metrics;

    private HeadDBMetrics() {}

    public static void register(@NotNull HeadDBPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        metrics = new Metrics(plugin, BSTATS_PLUGIN_ID);
    }

    public static void unregister(@NotNull HeadDBPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        metrics.shutdown();
    }

}