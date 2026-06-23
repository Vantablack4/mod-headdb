package io.github.silentdevelopment.headdb.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.model.HeadSource;
import org.junit.jupiter.api.Test;

import java.util.Set;

final class HeadQueryTest {

    @Test
    void createsAllQuery() {
        HeadQuery query = HeadQuery.all();

        assertEquals("", query.text());
        assertNull(query.source());
        assertEquals(Set.of(), query.ids());
        assertEquals(Set.of(), query.categories());
        assertNull(query.category());
        assertEquals(Set.of(), query.tags());
        assertEquals(Set.of(), query.collections());
        assertEquals(HeadSort.ID, query.sort());
        assertEquals(SortDirection.ASCENDING, query.direction());
        assertEquals(0, query.offset());
        assertEquals(50, query.limit());
    }

    @Test
    void createsTextQuery() {
        HeadQuery query = HeadQuery.text(" dragon ");

        assertEquals("dragon", query.text());
        assertEquals(Set.of(), query.ids());
        assertEquals(Set.of(), query.categories());
        assertEquals(HeadSort.RELEVANCE, query.sort());
        assertEquals(SortDirection.DESCENDING, query.direction());
    }

    @Test
    void normalizesFilters() {
        HeadQuery query = new HeadQuery(
                " Dragon ",
                HeadSource.REMOTE,
                Set.of(HeadId.remote(123)),
                " Monsters ",
                Set.of(" Fire ", " Boss "),
                Set.of(" Events "),
                HeadSort.NAME,
                SortDirection.ASCENDING,
                10,
                25
        );

        assertEquals("Dragon", query.text());
        assertEquals(HeadSource.REMOTE, query.source());
        assertEquals(Set.of(HeadId.remote(123)), query.ids());
        assertEquals(Set.of("monsters"), query.categories());
        assertEquals("monsters", query.category());
        assertEquals(Set.of("fire", "boss"), query.tags());
        assertEquals(Set.of("events"), query.collections());
        assertEquals(10, query.offset());
        assertEquals(25, query.limit());
    }

    @Test
    void normalizesMultipleCategories() {
        HeadQuery query = new HeadQuery(
                "",
                null,
                Set.of(),
                Set.of(" Monsters ", " Animals "),
                Set.of(),
                Set.of(),
                HeadSort.ID,
                SortDirection.ASCENDING,
                0,
                50
        );

        assertEquals(Set.of("monsters", "animals"), query.categories());
    }

    @Test
    void builderAddsIds() {
        HeadQuery query = HeadQuery.builder()
                .id(HeadId.remote(123))
                .id(HeadId.custom("spawn-dragon"))
                .build();

        assertEquals(Set.of(HeadId.remote(123), HeadId.custom("spawn-dragon")), query.ids());
    }

    @Test
    void builderAddsCategories() {
        HeadQuery query = HeadQuery.builder()
                .categories(Set.of(" Monsters ", " Animals "))
                .build();

        assertEquals(Set.of("monsters", "animals"), query.categories());
    }

    @Test
    void builderSingleCategoryClearsPreviousCategories() {
        HeadQuery query = HeadQuery.builder()
                .categories(Set.of("monsters", "animals"))
                .category("events")
                .build();

        assertEquals(Set.of("events"), query.categories());
        assertEquals("events", query.category());
    }

    @Test
    void rejectsNegativeOffset() {
        assertThrows(IllegalArgumentException.class, () -> new HeadQuery(
                "",
                null,
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                HeadSort.ID,
                SortDirection.ASCENDING,
                -1,
                50
        ));
    }

    @Test
    void rejectsInvalidLimit() {
        assertThrows(IllegalArgumentException.class, () -> new HeadQuery(
                "",
                null,
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                HeadSort.ID,
                SortDirection.ASCENDING,
                0,
                0
        ));

        assertThrows(IllegalArgumentException.class, () -> new HeadQuery(
                "",
                null,
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                HeadSort.ID,
                SortDirection.ASCENDING,
                0,
                -1
        ));

        assertThrows(IllegalArgumentException.class, () -> new HeadQuery(
                "",
                null,
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                HeadSort.ID,
                SortDirection.ASCENDING,
                0,
                501
        ));
    }

    @Test
    void rejectsEmptyCategory() {
        assertThrows(IllegalArgumentException.class, () -> new HeadQuery(
                "",
                null,
                Set.of(),
                "",
                Set.of(),
                Set.of(),
                HeadSort.ID,
                SortDirection.ASCENDING,
                0,
                50
        ));

        assertThrows(IllegalArgumentException.class, () -> new HeadQuery(
                "",
                null,
                Set.of(),
                "   ",
                Set.of(),
                Set.of(),
                HeadSort.ID,
                SortDirection.ASCENDING,
                0,
                50
        ));

        assertThrows(IllegalArgumentException.class, () -> new HeadQuery(
                "",
                null,
                Set.of(),
                Set.of(""),
                Set.of(),
                Set.of(),
                HeadSort.ID,
                SortDirection.ASCENDING,
                0,
                50
        ));

        assertThrows(IllegalArgumentException.class, () -> new HeadQuery(
                "",
                null,
                Set.of(),
                Set.of("   "),
                Set.of(),
                Set.of(),
                HeadSort.ID,
                SortDirection.ASCENDING,
                0,
                50
        ));
    }

    @Test
    void rejectsEmptyTag() {
        assertThrows(IllegalArgumentException.class, () -> new HeadQuery(
                "",
                null,
                Set.of(),
                Set.of(),
                Set.of(""),
                Set.of(),
                HeadSort.ID,
                SortDirection.ASCENDING,
                0,
                50
        ));

        assertThrows(IllegalArgumentException.class, () -> new HeadQuery(
                "",
                null,
                Set.of(),
                Set.of(),
                Set.of("   "),
                Set.of(),
                HeadSort.ID,
                SortDirection.ASCENDING,
                0,
                50
        ));
    }

    @Test
    void rejectsEmptyCollection() {
        assertThrows(IllegalArgumentException.class, () -> new HeadQuery(
                "",
                null,
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(""),
                HeadSort.ID,
                SortDirection.ASCENDING,
                0,
                50
        ));

        assertThrows(IllegalArgumentException.class, () -> new HeadQuery(
                "",
                null,
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of("   "),
                HeadSort.ID,
                SortDirection.ASCENDING,
                0,
                50
        ));
    }
}