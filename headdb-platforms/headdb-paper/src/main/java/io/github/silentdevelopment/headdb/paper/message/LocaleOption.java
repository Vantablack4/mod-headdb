package io.github.silentdevelopment.headdb.paper.message;

import io.github.silentdevelopment.hermes.id.LocaleId;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record LocaleOption(@NotNull LocaleId id, @NotNull String name, @NotNull String nativeName) {

    public LocaleOption {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(nativeName, "nativeName");
    }

    public @NotNull String displayName() {
        if (name.equals(nativeName)) {
            return name;
        }

        return nativeName + " (" + name + ")";
    }

}