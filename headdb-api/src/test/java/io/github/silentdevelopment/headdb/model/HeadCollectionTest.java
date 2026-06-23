package io.github.silentdevelopment.headdb.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class HeadCollectionTest {

    @Test
    void normalizesCollection() {
        HeadCollection collection = new HeadCollection(" Christmas ", " Christmas ", " Christmas heads ");

        assertEquals("christmas", collection.id());
        assertEquals("Christmas", collection.name());
        assertEquals("Christmas heads", collection.description());
    }

    @Test
    void acceptsNullDescriptionAsEmpty() {
        HeadCollection collection = new HeadCollection("christmas", "Christmas", null);

        assertEquals("", collection.description());
    }

    @Test
    void rejectsEmptyId() {
        assertThrows(IllegalArgumentException.class, () -> new HeadCollection("", "Christmas", ""));
        assertThrows(IllegalArgumentException.class, () -> new HeadCollection("   ", "Christmas", ""));
    }

    @Test
    void rejectsEmptyName() {
        assertThrows(IllegalArgumentException.class, () -> new HeadCollection("christmas", "", ""));
        assertThrows(IllegalArgumentException.class, () -> new HeadCollection("christmas", "   ", ""));
    }
}