package io.github.silentdevelopment.headdb.paper.command.format;

import io.github.silentdevelopment.headdb.database.DatabaseStats;
import io.github.silentdevelopment.headdb.database.DatabaseStatus;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import io.github.silentdevelopment.headdb.paper.runtime.RefreshState;
import io.github.silentdevelopment.headdb.paper.runtime.PluginRuntime;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

public final class StatusFormatter {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.systemDefault());

    private StatusFormatter() {
    }

    public static @NotNull List<Component> format(@NotNull PluginRuntime runtime, @NotNull CommandSender sender) {
        Objects.requireNonNull(runtime, "runtime");
        Objects.requireNonNull(sender, "sender");

        DatabaseStatus status = runtime.database().status();
        DatabaseStats stats = runtime.database().stats();
        RefreshState refresh = runtime.refreshState();

        return List.of(
                Component.empty(),
                Component.text("> ", NamedTextColor.GRAY).append(Component.text("HeadDB Status", NamedTextColor.RED)),
                databaseStatusLine(status, sender),
                line("Source", status.source()),
                line("Manifest ID", value(status.manifestId())),
                line("Catalog ID", value(status.artifactId())),
                line("Loaded at", formatInstant(status.loadedAt())),
                Component.empty(),
                line("Heads", stats.heads()),
                line("Categories", stats.categories()),
                line("Tags", stats.tags()),
                line("Collections", stats.collections()),
                line("Revocations", stats.revocations()),
                Component.empty(),
                line("Refresh running", yesNo(refresh.running())),
                line("Last failure", formatFailure(refresh.lastFailureMessage())),
                Component.empty()
        );
    }

    private static @NotNull Component databaseStatusLine(@NotNull DatabaseStatus status, @NotNull CommandSender sender) {
        Component line = Component.text("Status: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(status.state()), statusColor(status)));

        if (isLoaded(status)) {
            return line;
        }

        if (!Permissions.has(sender, Permissions.REFRESH)) {
            return line;
        }

        return line.append(Component.text("  ")).append(refreshButton());
    }

    private static boolean isLoaded(@NotNull DatabaseStatus status) {
        return "LOADED".equalsIgnoreCase(String.valueOf(status.state()));
    }

    private static @NotNull NamedTextColor statusColor(@NotNull DatabaseStatus status) {
        if (isLoaded(status)) {
            return NamedTextColor.GOLD;
        }

        return NamedTextColor.RED;
    }

    private static @NotNull Component refreshButton() {
        return Component.text("[ ", NamedTextColor.DARK_GRAY)
                .append(Component.text("REFRESH", NamedTextColor.GOLD).clickEvent(ClickEvent.runCommand("/hdb refresh")).hoverEvent(HoverEvent.showText(Component.text("Click to refresh the HeadDB database.", NamedTextColor.GRAY))))
                .append(Component.text(" ]", NamedTextColor.DARK_GRAY));
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

    private static @NotNull String formatFailure(@Nullable String message) {
        if (message == null || message.isBlank()) {
            return "none";
        }

        return message;
    }

    private static @NotNull String value(@Nullable Object value) {
        if (value == null) {
            return "none";
        }

        return String.valueOf(value);
    }
}