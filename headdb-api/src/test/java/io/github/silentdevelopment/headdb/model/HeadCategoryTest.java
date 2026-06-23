package io.github.silentdevelopment.headdb.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class HeadCategoryTest {

    @Test
    void normalizesCategory() {
        HeadCategory category = new HeadCategory(" Animals ", " Animals ", " Animal heads ");

        assertEquals("animals", category.id());
        assertEquals("Animals", category.name());
        assertEquals("Animal heads", category.description());
    }

    @Test
    void acceptsNullDescriptionAsEmpty() {
        HeadCategory category = new HeadCategory("animals", "Animals", null);

        assertEquals("", category.description());
    }

    @Test
    void rejectsEmptyId() {
        assertThrows(IllegalArgumentException.class, () -> new HeadCategory("", "Animals", ""));
        assertThrows(IllegalArgumentException.class, () -> new HeadCategory("   ", "Animals", ""));
    }

    @Test
    void rejectsEmptyName() {
        assertThrows(IllegalArgumentException.class, () -> new HeadCategory("animals", "", ""));
        assertThrows(IllegalArgumentException.class, () -> new HeadCategory("animals", "   ", ""));
    }
}