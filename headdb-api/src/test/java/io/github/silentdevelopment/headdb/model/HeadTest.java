package io.github.silentdevelopment.headdb.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;
import org.junit.jupiter.api.Test;

final class HeadTest {

    @Test
    void normalizesHeadFields() {
        Head head = new Head(
                HeadId.remote(123),
                " Dragon Head ",
                new HeadTexture("ABC123"),
                " Monsters ",
                Set.of(" Fire ", " Boss "),
                Set.of(" Events ")
        );

        assertEquals("Dragon Head", head.name());
        assertEquals("monsters", head.category());
        assertEquals(Set.of("fire", "boss"), head.tags());
        assertEquals(Set.of("events"), head.collections());
    }

    @Test
    void rejectsEmptyName() {
        assertThrows(IllegalArgumentException.class, () -> new Head(HeadId.remote(1), "", new HeadTexture("abc"), "animals", Set.of(), Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new Head(HeadId.remote(1), "   ", new HeadTexture("abc"), "animals", Set.of(), Set.of()));
    }

    @Test
    void rejectsEmptyCategory() {
        assertThrows(IllegalArgumentException.class, () -> new Head(HeadId.remote(1), "Test", new HeadTexture("abc"), "", Set.of(), Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new Head(HeadId.remote(1), "Test", new HeadTexture("abc"), "   ", Set.of(), Set.of()));
    }

    @Test
    void rejectsEmptyTag() {
        assertThrows(IllegalArgumentException.class, () -> new Head(HeadId.remote(1), "Test", new HeadTexture("abc"), "animals", Set.of(""), Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new Head(HeadId.remote(1), "Test", new HeadTexture("abc"), "animals", Set.of("   "), Set.of()));
    }

    @Test
    void rejectsEmptyCollection() {
        assertThrows(IllegalArgumentException.class, () -> new Head(HeadId.remote(1), "Test", new HeadTexture("abc"), "animals", Set.of(), Set.of("")));
        assertThrows(IllegalArgumentException.class, () -> new Head(HeadId.remote(1), "Test", new HeadTexture("abc"), "animals", Set.of(), Set.of("   ")));
    }

    @Test
    void rejectsNullRequiredFields() {
        assertThrows(NullPointerException.class, () -> new Head(null, "Test", new HeadTexture("abc"), "animals", Set.of(), Set.of()));
        assertThrows(NullPointerException.class, () -> new Head(HeadId.remote(1), null, new HeadTexture("abc"), "animals", Set.of(), Set.of()));
        assertThrows(NullPointerException.class, () -> new Head(HeadId.remote(1), "Test", null, "animals", Set.of(), Set.of()));
        assertThrows(NullPointerException.class, () -> new Head(HeadId.remote(1), "Test", new HeadTexture("abc"), null, Set.of(), Set.of()));
        assertThrows(NullPointerException.class, () -> new Head(HeadId.remote(1), "Test", new HeadTexture("abc"), "animals", null, Set.of()));
        assertThrows(NullPointerException.class, () -> new Head(HeadId.remote(1), "Test", new HeadTexture("abc"), "animals", Set.of(), null));
    }
}