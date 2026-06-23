package io.github.silentdevelopment.headdb.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class HeadTextureTest {

    @Test
    void normalizesTextureHash() {
        HeadTexture texture = new HeadTexture(" ABCDEF123 ");

        assertEquals("abcdef123", texture.hash());
    }

    @Test
    void rejectsEmptyTextureHash() {
        assertThrows(IllegalArgumentException.class, () -> new HeadTexture(""));
        assertThrows(IllegalArgumentException.class, () -> new HeadTexture("   "));
    }

    @Test
    void rejectsNullTextureHash() {
        assertThrows(NullPointerException.class, () -> new HeadTexture(null));
    }
}