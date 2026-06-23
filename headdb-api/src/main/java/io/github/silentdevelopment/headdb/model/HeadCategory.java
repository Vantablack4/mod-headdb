package io.github.silentdevelopment.headdb.model;

import java.util.Locale;
import java.util.Objects;

public record HeadCategory(String id, String name, String description) {

    public HeadCategory {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");

        id = id.trim().toLowerCase(Locale.ROOT);
        name = name.trim();

        if (id.isEmpty()) {
            throw new IllegalArgumentException("Category ID cannot be empty.");
        }

        if (name.isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be empty.");
        }

        if (description == null) {
            description = "";
        } else {
            description = description.trim();
        }
    }

}