package io.github.silentdevelopment.headdb.paper.gui.common;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class GuiTitles {

    private static final int ADMIN_TITLE_PIXEL_TARGET = 134;
    private static final int SPACE_WIDTH = 4;

    private GuiTitles() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static @NotNull Component title(@NotNull String title, boolean adminMode) {
        Objects.requireNonNull(title, "title");

        String clean = title.trim();
        if (!adminMode) {
            return Component.text(clean, NamedTextColor.RED).decoration(TextDecoration.ITALIC, false);
        }

        int spaces = Math.max(2, (int) Math.ceil((double) (ADMIN_TITLE_PIXEL_TARGET - pixelWidth(clean) - pixelWidth("ADMIN")) / (double) SPACE_WIDTH));
        return Component.text(clean, NamedTextColor.RED)
                .append(Component.text(" ".repeat(spaces)))
                .append(Component.text("ADMIN", NamedTextColor.DARK_RED, TextDecoration.BOLD))
                .decoration(TextDecoration.ITALIC, false);
    }

    public static @NotNull Component title(@NotNull String prefix, @NotNull String detail, boolean adminMode) {
        Objects.requireNonNull(prefix, "prefix");
        Objects.requireNonNull(detail, "detail");

        String clean = prefix.trim() + ": " + shortTitle(detail.trim());
        return title(clean, adminMode);
    }

    private static int pixelWidth(@NotNull String value) {
        int width = 0;
        for (int index = 0; index < value.length(); index++) {
            width += characterWidth(value.charAt(index));
        }
        return width;
    }

    private static int characterWidth(char character) {
        return switch (character) {
            case ' ', '!', ',', '.', ':', ';', '|', 'i', 'l', '\'' -> 2;
            case '[', ']', 'I', 't' -> 4;
            case '"', '(', ')', '*', '<', '>', 'f', 'k' -> 5;
            case '@', '~' -> 7;
            default -> 6;
        };
    }

    private static @NotNull String shortTitle(@NotNull String value) {
        if (value.length() <= 16) {
            return value;
        }

        return value.substring(0, 13) + "...";
    }
}
