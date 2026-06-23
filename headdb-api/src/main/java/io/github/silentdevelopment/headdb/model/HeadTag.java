package io.github.silentdevelopment.headdb.model;

import java.util.Locale;
import java.util.Objects;

public record HeadTag(String id, String name, String description) {

    public HeadTag {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");

        id = id.trim().toLowerCase(Locale.ROOT);
        name = name.trim();

        if (id.isEmpty()) {
            throw new IllegalArgumentException("Tag ID cannot be empty.");
        }

        if (name.isEmpty()) {
            throw new IllegalArgumentException("Tag name cannot be empty.");
        }

        if (description == null) {
            description = "";
        } else {
            description = description.trim();
        }
    }

}