package io.github.silentdevelopment.headdb.paper.command.format;

import io.github.silentdevelopment.headdb.model.Head;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public final class HeadInfoFormatter {

    private static final int MAX_DISPLAYED_IDS = 12;

    private HeadInfoFormatter() {}

    public static @NotNull List<Component> format(@NotNull Head head) {
        Objects.requireNonNull(head, "head");

        return List.of(
                Component.empty(),
                Component.text("> ", NamedTextColor.GRAY).append(Component.text("Head Info", NamedTextColor.GOLD)),
                line("Name", head.name()),
                line("ID", head.id().toString()),
                line("Source", head.id().source()),
                line("Category", head.category()),
                line("Tags", join(head.tags())),
                line("Collections", join(head.collections())),
                Component.text("Texture: ", NamedTextColor.GRAY).append(Component.text(head.texture().hash(), NamedTextColor.GOLD).clickEvent(ClickEvent.copyToClipboard(head.texture().hash())).hoverEvent(HoverEvent.showText(Component.text("Click to copy.", NamedTextColor.GRAY)))),
                Component.empty()
        );
    }

    private static @NotNull Component line(@NotNull String key, @NotNull Object value) {
        return Component.text(key + ": ", NamedTextColor.GRAY).append(Component.text(String.valueOf(value), NamedTextColor.GOLD));
    }

    private static @NotNull String join(@NotNull Collection<String> values) {
        if (values.isEmpty()) {
            return "none";
        }

        StringJoiner joiner = new StringJoiner(", ");
        int index = 0;

        for (String value : values) {
            if (index >= MAX_DISPLAYED_IDS) {
                joiner.add("+" + (values.size() - MAX_DISPLAYED_IDS) + " more");
                break;
            }

            joiner.add(value);
            index++;
        }

        return joiner.toString();
    }
}