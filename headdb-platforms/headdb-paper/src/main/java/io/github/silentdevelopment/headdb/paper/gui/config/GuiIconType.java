package io.github.silentdevelopment.headdb.paper.gui.config;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;

public enum GuiIconType {

    ITEM,
    HEAD;

    public static @NotNull GuiIconType parse(@NotNull String value) {
        Objects.requireNonNull(value, "value");

        String normalized = value.trim().toUpperCase(Locale.ROOT);

        if (normalized.isBlank()) {
            throw new IllegalArgumentException("GUI icon type cannot be blank.");
        }

        return GuiIconType.valueOf(normalized);
    }
}