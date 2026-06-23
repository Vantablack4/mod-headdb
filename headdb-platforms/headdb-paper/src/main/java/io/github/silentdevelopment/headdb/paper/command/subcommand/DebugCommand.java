package io.github.silentdevelopment.headdb.paper.command.subcommand;

import io.github.silentdevelopment.headdb.database.DatabaseStats;
import io.github.silentdevelopment.headdb.database.DatabaseStatus;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.command.CommandRequirements;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import io.github.silentdevelopment.headdb.paper.runtime.BuildInfo;
import io.github.silentdevelopment.headdb.paper.runtime.RefreshState;
import io.github.silentdevelopment.relay.command.Command;
import io.github.silentdevelopment.relay.paper.command.AbstractPaperCommand;
import io.github.silentdevelopment.relay.paper.command.PaperCommands;
import io.github.silentdevelopment.relay.paper.command.context.PaperCommandContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public final class DebugCommand extends AbstractPaperCommand {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.systemDefault());

    private final HeadDBPlugin plugin;

    public DebugCommand(@NotNull HeadDBPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    protected void handle(@NotNull PaperCommandContext context) {
        DatabaseStatus status = plugin.runtime().database().status();
        DatabaseStats stats = plugin.runtime().database().stats();
        RefreshState refresh = plugin.runtime().refreshState();
        BuildInfo buildInfo = BuildInfo.read(plugin);

        context.reply(Component.empty());
        context.reply(Component.text("> ", NamedTextColor.DARK_GRAY).append(Component.text("Debug", NamedTextColor.RED)));

        context.reply(line("Version", buildInfo.version()));
        context.reply(line("Base version", buildInfo.baseVersion()));
        context.reply(line("Build", value(buildInfo.buildNumber())));
        context.reply(line("Attempt", value(buildInfo.buildAttempt())));
        context.reply(line("Commit", value(buildInfo.commit())));
        context.reply(line("Full commit", value(buildInfo.fullCommit())));
        context.reply(line("Branch", value(buildInfo.branch())));
        context.reply(line("Timestamp", value(buildInfo.buildTime())));

        context.reply(Component.empty());
        context.reply(line("State", status.state()));
        context.reply(line("Source", status.source()));
        context.reply(line("Manifest ID", value(status.manifestId())));
        context.reply(line("Catalog ID", value(status.artifactId())));
        context.reply(line("Loaded at", formatInstant(status.loadedAt())));
        context.reply(line("Last database error", value(status.lastError())));

        context.reply(Component.empty());
        context.reply(line("Heads", stats.heads()));
        context.reply(line("Categories", stats.categories()));
        context.reply(line("Tags", stats.tags()));
        context.reply(line("Collections", stats.collections()));
        context.reply(line("Revocations", stats.revocations()));

        context.reply(Component.empty());
        context.reply(line("Manifest URL", plugin.config().remoteManifestUri()));
        context.reply(line("Preferred mirror", plugin.config().preferredMirrorId()));
        context.reply(line("Load cache on startup", yesNo(plugin.config().loadCacheOnStartup())));
        context.reply(line("Refresh on startup", yesNo(plugin.config().refreshOnStartup())));
        context.reply(line("Cache directory", plugin.config().cacheDirectory(plugin.getDataFolder().toPath()).toAbsolutePath().normalize()));
        context.reply(line("Item cache enabled", yesNo(plugin.config().cacheItemEnabled())));
        context.reply(line("Item cache size", plugin.itemCacheSize()));

        context.reply(Component.empty());
        context.reply(line("Refresh running", yesNo(refresh.running())));
        context.reply(line("Last successful refresh", formatInstant(refresh.lastSuccessfulRefresh())));
        context.reply(line("Last failed refresh", formatInstant(refresh.lastFailedRefresh())));
        context.reply(line("Last refresh failure", value(refresh.lastFailureMessage())));
        context.reply(Component.empty());
    }

    @Override
    protected @NotNull Command buildCommand() {
        return PaperCommands.literal("debug")
                .alias("d")
                .description("Shows detailed runtime diagnostics.")
                .requirement(CommandRequirements.permission(Permissions.DEBUG))
                .noArgs()
                .build();
    }

    private static @NotNull Component line(@NotNull String key, @Nullable Object value) {
        return Component.text(key + ": ", NamedTextColor.GRAY).append(Component.text(String.valueOf(value), NamedTextColor.GOLD));
    }

    private static @NotNull String yesNo(boolean value) {
        if (value) {
            return "yes";
        }

        return "no";
    }

    private static @NotNull String formatInstant(@Nullable Instant instant) {
        if (instant == null) {
            return "never";
        }

        return TIME_FORMAT.format(instant);
    }

    private static @NotNull String value(@Nullable Object value) {
        if (value == null) {
            return "none";
        }

        String string = String.valueOf(value);
        if (string.isBlank()) {
            return "none";
        }

        return string;
    }
}