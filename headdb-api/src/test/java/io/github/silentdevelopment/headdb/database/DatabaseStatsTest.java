package io.github.silentdevelopment.headdb.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class DatabaseStatsTest {

    @Test
    void createsEmptyStats() {
        DatabaseStats stats = DatabaseStats.empty();

        assertEquals(0, stats.heads());
        assertEquals(0, stats.categories());
        assertEquals(0, stats.tags());
        assertEquals(0, stats.collections());
        assertEquals(0, stats.revocations());
    }

    @Test
    void acceptsNonNegativeValues() {
        DatabaseStats stats = new DatabaseStats(10, 2, 3, 4, 1);

        assertEquals(10, stats.heads());
        assertEquals(2, stats.categories());
        assertEquals(3, stats.tags());
        assertEquals(4, stats.collections());
        assertEquals(1, stats.revocations());
    }

    @Test
    void rejectsNegativeValues() {
        assertThrows(IllegalArgumentException.class, () -> new DatabaseStats(-1, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new DatabaseStats(0, -1, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new DatabaseStats(0, 0, -1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new DatabaseStats(0, 0, 0, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new DatabaseStats(0, 0, 0, 0, -1));
    }
}