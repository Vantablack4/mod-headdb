package io.github.silentdevelopment.headdb.paper.runtime;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

public record BuildInfo(
        @NotNull String version,
        @NotNull String baseVersion,
        @Nullable String buildNumber,
        @Nullable String buildAttempt,
        @Nullable String commit,
        @Nullable String fullCommit,
        @Nullable String branch,
        @Nullable String buildTime
) {

    public static @NotNull BuildInfo read(@NotNull JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");

        String pluginVersion = plugin.getPluginMeta().getVersion();
        Properties properties = readProperties(plugin, "git.properties");

        String buildVersion = valueOrDefault(properties.getProperty("headdb.build.version"), pluginVersion);
        String baseVersion = valueOrDefault(properties.getProperty("headdb.build.base-version"), pluginVersion);
        String buildNumber = blankToNull(properties.getProperty("headdb.build.number"));
        String buildAttempt = blankToNull(properties.getProperty("headdb.build.attempt"));
        String fullCommit = firstPresent(properties.getProperty("headdb.build.commit"), properties.getProperty("git.commit.id.full"));
        String commit = firstPresent(properties.getProperty("git.commit.id.abbrev"), abbreviate(fullCommit));
        String branch = blankToNull(properties.getProperty("git.branch"));
        String buildTime = blankToNull(properties.getProperty("git.build.time"));

        return new BuildInfo(buildVersion, baseVersion, buildNumber, buildAttempt, commit, fullCommit, branch, buildTime);
    }

    public boolean hasGitInfo() {
        return commit != null || fullCommit != null || branch != null || buildTime != null;
    }

    public boolean hasCiBuildInfo() {
        return buildNumber != null || buildAttempt != null || !version.equals(baseVersion);
    }

    private static @NotNull Properties readProperties(@NotNull JavaPlugin plugin, @NotNull String resource) {
        Properties properties = new Properties();

        try (InputStream input = plugin.getResource(resource)) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException ignored) {
            // Build information is optional.
        }

        return properties;
    }

    private static @NotNull String valueOrDefault(@Nullable String value, @NotNull String fallback) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            return fallback;
        }

        return normalized;
    }

    private static @Nullable String firstPresent(@Nullable String first, @Nullable String second) {
        String normalizedFirst = blankToNull(first);
        if (normalizedFirst != null) {
            return normalizedFirst;
        }

        return blankToNull(second);
    }

    private static @Nullable String abbreviate(@Nullable String value) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            return null;
        }

        if (normalized.length() <= 7) {
            return normalized;
        }

        return normalized.substring(0, 7);
    }

    private static @Nullable String blankToNull(@Nullable String value) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("unknown")) {
            return null;
        }

        return value.trim();
    }
}