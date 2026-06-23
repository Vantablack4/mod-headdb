package io.github.silentdevelopment.headdb.model;

import java.util.Locale;
import java.util.Objects;

public record HeadTexture(String hash) {

    public HeadTexture {
        Objects.requireNonNull(hash, "hash");

        hash = hash.trim().toLowerCase(Locale.ROOT);

        if (hash.isEmpty()) {
            throw new IllegalArgumentException("Head texture hash cannot be empty.");
        }
    }

}