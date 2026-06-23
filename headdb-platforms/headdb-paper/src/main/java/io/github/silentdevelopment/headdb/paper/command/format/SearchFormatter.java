package io.github.silentdevelopment.headdb.paper.command.format;

import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import io.github.silentdevelopment.headdb.paper.search.SearchRequest;
import io.github.silentdevelopment.headdb.query.HeadQueryResult;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public final class SearchFormatter {

    private SearchFormatter() {
    }

    public static @NotNull List<Component> format(@NotNull CommandSender sender, @NotNull SearchRequest request, @NotNull HeadQueryResult result) {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(result, "result");

        List<Component> lines = new ArrayList<>();

        lines.add(Component.empty());
        lines.add(Component.text("> ", NamedTextColor.GRAY).append(Component.text("HeadDB Search", NamedTextColor.RED)));

        if (!request.query().isBlank()) {
            lines.add(line("Query", request.query()));
        }

        String filters = filters(request);

        if (!filters.isBlank()) {
            lines.add(line("Filters", filters));
        }

        lines.add(line("Sort", request.sort() + " " + request.direction()));
        lines.add(line("Results", result.total()));
        lines.add(line("Page", request.page() + " / " + Math.max(1, result.totalPages())));
        lines.add(Component.empty());

        if (result.heads().isEmpty()) {
            lines.add(Component.text("No heads found.", NamedTextColor.GRAY));
            lines.add(Component.empty());
            return List.copyOf(lines);
        }

        int index = ((request.page() - 1) * request.limit()) + 1;

        for (Head head : result.heads()) {
            lines.add(resultLine(sender, index, head));
            index++;
        }

        if (request.page() < result.totalPages()) {
            lines.add(Component.empty());
            lines.add(nextPageHint(sender, request));
        }

        lines.add(Component.empty());

        return List.copyOf(lines);
    }

    private static @NotNull Component resultLine(@NotNull CommandSender sender, int index, @NotNull Head head) {
        String commandId = head.id().display();

        Component line = Component.text(index + ". ", NamedTextColor.DARK_GRAY)
                .append(Component.text(head.name(), NamedTextColor.GOLD))
                .append(Component.text(" ", NamedTextColor.GRAY))
                .append(Component.text("[", NamedTextColor.DARK_GRAY))
                .append(idComponent(sender, head.id().toString(), commandId))
                .append(Component.text("]", NamedTextColor.DARK_GRAY));

        Component actions = actions(sender, commandId);

        if (actions.equals(Component.empty())) {
            return line;
        }

        return line.append(Component.text("  ")).append(actions);
    }

    private static @NotNull Component idComponent(@NotNull CommandSender sender, @NotNull String canonicalId, @NotNull String commandId) {
        Component component = Component.text(canonicalId, NamedTextColor.GRAY);

        if (!(sender instanceof Player)) {
            return component;
        }

        return component.clickEvent(ClickEvent.suggestCommand("/hdb info " + commandId))
                .hoverEvent(HoverEvent.showText(Component.text("Click to suggest /hdb info " + commandId, NamedTextColor.GRAY)));
    }

    private static @NotNull Component actions(@NotNull CommandSender sender, @NotNull String commandId) {
        if (!(sender instanceof Player)) {
            return consoleActions(sender, commandId);
        }

        return playerActions(sender, commandId);
    }

    private static @NotNull Component playerActions(@NotNull CommandSender sender, @NotNull String commandId) {
        List<Component> buttons = new ArrayList<>();

        if (Permissions.has(sender, Permissions.INFO)) {
            buttons.add(actionButton("INFO", "/hdb info " + commandId, "Suggest /hdb info " + commandId));
        }

        if (Permissions.has(sender, Permissions.GIVE)) {
            buttons.add(actionButton("GIVE", "/hdb give " + commandId + " ", "Suggest /hdb give " + commandId));
        }

        return join(buttons, Component.text(" "));
    }

    private static @NotNull Component consoleActions(@NotNull CommandSender sender, @NotNull String commandId) {
        List<Component> hints = new ArrayList<>();

        if (Permissions.has(sender, Permissions.INFO)) {
            hints.add(commandHint("/hdb info " + commandId));
        }

        if (Permissions.has(sender, Permissions.GIVE)) {
            hints.add(commandHint("/hdb give <player> " + commandId));
        }

        return join(hints, Component.text(" | ", NamedTextColor.DARK_GRAY));
    }

    private static @NotNull Component commandHint(@NotNull String command) {
        return Component.text(command, NamedTextColor.DARK_GRAY);
    }

    private static @NotNull Component actionButton(@NotNull String label, @NotNull String suggestion, @NotNull String hover) {
        return Component.text("[ ", NamedTextColor.DARK_GRAY)
                .append(Component.text(label, NamedTextColor.GOLD)
                        .clickEvent(ClickEvent.suggestCommand(suggestion))
                        .hoverEvent(HoverEvent.showText(Component.text(hover, NamedTextColor.GRAY))))
                .append(Component.text(" ]", NamedTextColor.DARK_GRAY));
    }

    private static @NotNull Component join(@NotNull List<Component> components, @NotNull Component separator) {
        if (components.isEmpty()) {
            return Component.empty();
        }

        Component result = components.get(0);

        for (int index = 1; index < components.size(); index++) {
            result = result.append(separator).append(components.get(index));
        }

        return result;
    }

    private static @NotNull Component nextPageHint(@NotNull CommandSender sender, @NotNull SearchRequest request) {
        int nextPage = request.page() + 1;
        String command = "/hdb search " + nextPageCommandHint(request, nextPage);

        Component hint = Component.text(command, NamedTextColor.GOLD);

        if (sender instanceof Player) {
            hint = hint.clickEvent(ClickEvent.suggestCommand(command))
                    .hoverEvent(HoverEvent.showText(Component.text("Click to suggest the next page command.", NamedTextColor.GRAY)));
        }

        return Component.text("Next page: ", NamedTextColor.GRAY).append(hint);
    }

    private static @NotNull String nextPageCommandHint(@NotNull SearchRequest request, int nextPage) {
        StringBuilder builder = new StringBuilder();

        if (!request.query().isBlank()) {
            builder.append(request.query());
        }

        builder.append(" --page ").append(nextPage);

        return builder.toString().trim();
    }

    private static @NotNull Component line(@NotNull String key, @Nullable Object value) {
        return Component.text(key + ": ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(value), NamedTextColor.GOLD));
    }

    private static @NotNull String filters(@NotNull SearchRequest request) {
        StringJoiner joiner = new StringJoiner(", ");

        if (!request.ids().isEmpty()) {
            joiner.add("ids=" + request.ids());
        }

        if (!request.categories().isEmpty()) {
            joiner.add("categories=" + String.join(",", request.categories()));
        }

        if (!request.tags().isEmpty()) {
            joiner.add("tags=" + request.tags());
        }

        if (!request.collections().isEmpty()) {
            joiner.add("collections=" + request.collections());
        }

        return joiner.toString();
    }
}