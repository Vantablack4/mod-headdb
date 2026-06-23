package io.github.silentdevelopment.headdb.query;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.model.HeadTexture;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class HeadQueryResultTest {

    @Test
    void detectsNextPage() {
        Head head = head(1);
        HeadQueryResult result = new HeadQueryResult(List.of(head), 10, 0, 1);

        assertTrue(result.hasNextPage());
    }

    @Test
    void detectsLastPage() {
        Head head = head(1);
        HeadQueryResult result = new HeadQueryResult(List.of(head), 1, 0, 50);

        assertFalse(result.hasNextPage());
    }

    @Test
    void rejectsNegativeTotal() {
        assertThrows(IllegalArgumentException.class, () -> new HeadQueryResult(List.of(), -1, 0, 50));
    }

    @Test
    void rejectsNegativeOffset() {
        assertThrows(IllegalArgumentException.class, () -> new HeadQueryResult(List.of(), 0, -1, 50));
    }

    @Test
    void rejectsInvalidLimit() {
        assertThrows(IllegalArgumentException.class, () -> new HeadQueryResult(List.of(), 0, 0, 0));
    }

    @Test
    void rejectsMoreHeadsThanLimit() {
        assertThrows(IllegalArgumentException.class, () -> new HeadQueryResult(List.of(head(1), head(2)), 2, 0, 1));
    }

    private static Head head(int id) {
        return new Head(HeadId.remote(id), "Head " + id, new HeadTexture("texture" + id), "animals", Set.of(), Set.of());
    }
}