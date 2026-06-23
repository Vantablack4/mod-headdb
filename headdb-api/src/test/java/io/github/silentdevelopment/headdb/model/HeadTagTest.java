package io.github.silentdevelopment.headdb.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class HeadTagTest {

    @Test
    void normalizesTag() {
        HeadTag tag = new HeadTag(" Cute ", " Cute ", " Cute heads ");

        assertEquals("cute", tag.id());
        assertEquals("Cute", tag.name());
        assertEquals("Cute heads", tag.description());
    }

    @Test
    void acceptsNullDescriptionAsEmpty() {
        HeadTag tag = new HeadTag("cute", "Cute", null);

        assertEquals("", tag.description());
    }

    @Test
    void rejectsEmptyId() {
        assertThrows(IllegalArgumentException.class, () -> new HeadTag("", "Cute", ""));
        assertThrows(IllegalArgumentException.class, () -> new HeadTag("   ", "Cute", ""));
    }

    @Test
    void rejectsEmptyName() {
        assertThrows(IllegalArgumentException.class, () -> new HeadTag("cute", "", ""));
        assertThrows(IllegalArgumentException.class, () -> new HeadTag("cute", "   ", ""));
    }
}