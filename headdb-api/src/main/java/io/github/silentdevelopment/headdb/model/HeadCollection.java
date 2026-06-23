package io.github.silentdevelopment.headdb.model;

import java.util.Locale;
import java.util.Objects;

public record HeadCollection(String id, String name, String description) {

    public HeadCollection {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");

        id = id.trim().toLowerCase(Locale.ROOT);
        name = name.trim();

        if (id.isEmpty()) {
            throw new IllegalArgumentException("Collection ID cannot be empty.");
        }

        if (name.isEmpty()) {
            throw new IllegalArgumentException("Collection name cannot be empty.");
        }

        if (description == null) {
            description = "";
        } else {
            description = description.trim();
        }
    }

}