package io.github.silentdevelopment.headdb.core.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.silentdevelopment.headdb.core.database.revocation.HeadRevocation;
import io.github.silentdevelopment.headdb.core.database.revocation.RevocationSet;
import io.github.silentdevelopment.headdb.database.DatabaseSource;
import io.github.silentdevelopment.headdb.database.DatabaseState;
import io.github.silentdevelopment.headdb.database.DatabaseStats;
import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadCategory;
import io.github.silentdevelopment.headdb.model.HeadCollection;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.model.HeadSource;
import io.github.silentdevelopment.headdb.model.HeadTag;
import io.github.silentdevelopment.headdb.model.HeadTexture;
import io.github.silentdevelopment.headdb.query.HeadQuery;
import io.github.silentdevelopment.headdb.query.HeadQueryResult;
import io.github.silentdevelopment.headdb.query.HeadSort;
import io.github.silentdevelopment.headdb.query.SortDirection;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class DefaultHeadDatabaseTest {

    @Test
    void emptyDatabaseReportsNotLoaded() {
        DefaultHeadDatabase database = new DefaultHeadDatabase();

        assertEquals(DatabaseState.NOT_LOADED, database.status().state());
        assertEquals(DatabaseStats.empty(), database.stats());
        assertTrue(database.findById(HeadId.remote(1)).isEmpty());
        assertEquals(0, database.categories().size());
        assertEquals(0, database.tags().size());
        assertEquals(0, database.collections().size());
        assertEquals(0, database.search(HeadQuery.all()).total());
    }

    @Test
    void exposesStatusAndStatsFromSnapshot() {
        DefaultHeadDatabase database = database();

        assertEquals(DatabaseState.LOADED, database.status().state());
        assertEquals(DatabaseSource.REMOTE, database.status().source());
        assertEquals(new DatabaseStats(5, 4, 5, 3, 1), database.stats());
    }

    @Test
    void findsHeadById() {
        DefaultHeadDatabase database = database();

        assertEquals("Cute Cat", database.findById(HeadId.remote(1)).orElseThrow().name());
    }

    @Test
    void metadataLookupsNormalizeInput() {
        DefaultHeadDatabase database = database();

        assertEquals("animals", database.category(" Animals ").orElseThrow().id());
        assertEquals("cute", database.tag(" Cute ").orElseThrow().id());
        assertEquals("christmas", database.collection(" Christmas ").orElseThrow().id());
    }

    @Test
    void searchesByText() {
        DefaultHeadDatabase database = database();
        HeadQuery query = query("cat", null, null, Set.of(), Set.of(), HeadSort.RELEVANCE, SortDirection.ASCENDING, 0, 50);

        HeadQueryResult result = database.search(query);

        assertEquals(2, result.total());
        assertEquals(List.of(HeadId.remote(3), HeadId.remote(1)), ids(result));
    }

    @Test
    void searchesBySource() {
        DefaultHeadDatabase database = database();
        HeadQuery query = query("", HeadSource.CUSTOM, null, Set.of(), Set.of(), HeadSort.ID, SortDirection.ASCENDING, 0, 50);

        HeadQueryResult result = database.search(query);

        assertEquals(1, result.total());
        assertEquals(List.of(HeadId.custom("server-icon")), ids(result));
    }

    @Test
    void searchesByCategory() {
        DefaultHeadDatabase database = database();
        HeadQuery query = query("", null, "animals", Set.of(), Set.of(), HeadSort.ID, SortDirection.ASCENDING, 0, 50);

        HeadQueryResult result = database.search(query);

        assertEquals(2, result.total());
        assertEquals(List.of(HeadId.remote(1), HeadId.remote(3)), ids(result));
    }

    @Test
    void searchesByTags() {
        DefaultHeadDatabase database = database();
        HeadQuery query = query("", null, null, Set.of("cute", "pet"), Set.of(), HeadSort.ID, SortDirection.ASCENDING, 0, 50);

        HeadQueryResult result = database.search(query);

        assertEquals(1, result.total());
        assertEquals(List.of(HeadId.remote(1)), ids(result));
    }

    @Test
    void searchesByCollections() {
        DefaultHeadDatabase database = database();
        HeadQuery query = query("", null, null, Set.of(), Set.of("halloween"), HeadSort.ID, SortDirection.ASCENDING, 0, 50);

        HeadQueryResult result = database.search(query);

        assertEquals(1, result.total());
        assertEquals(List.of(HeadId.remote(2)), ids(result));
    }

    @Test
    void sortsByRemoteNumericId() {
        DefaultHeadDatabase database = database();
        HeadQuery query = query("", null, null, Set.of(), Set.of(), HeadSort.ID, SortDirection.ASCENDING, 0, 50);

        HeadQueryResult result = database.search(query);

        assertEquals(List.of(HeadId.remote(1), HeadId.remote(2), HeadId.remote(3), HeadId.custom("server-icon"), HeadId.player(playerUuid())), ids(result));
    }

    @Test
    void sortsByName() {
        DefaultHeadDatabase database = database();
        HeadQuery query = query("", null, null, Set.of(), Set.of(), HeadSort.NAME, SortDirection.ASCENDING, 0, 50);

        HeadQueryResult result = database.search(query);

        assertEquals(List.of("Cat Icon", "Cute Cat", "Dragon Block", "Player Head", "Server Icon"), names(result));
    }

    @Test
    void sortsByCategory() {
        DefaultHeadDatabase database = database();
        HeadQuery query = query("", null, null, Set.of(), Set.of(), HeadSort.CATEGORY, SortDirection.ASCENDING, 0, 50);

        HeadQueryResult result = database.search(query);

        assertEquals(List.of("animals", "animals", "blocks", "icons", "players"), categories(result));
    }

    @Test
    void paginatesResults() {
        DefaultHeadDatabase database = database();
        HeadQuery query = query("", null, null, Set.of(), Set.of(), HeadSort.ID, SortDirection.ASCENDING, 1, 2);

        HeadQueryResult result = database.search(query);

        assertEquals(5, result.total());
        assertEquals(1, result.offset());
        assertEquals(2, result.limit());
        assertEquals(List.of(HeadId.remote(2), HeadId.remote(3)), ids(result));
        assertTrue(result.hasNextPage());
    }

    @Test
    void replaceSwapsSnapshot() {
        DefaultHeadDatabase database = database();

        database.replace(replacementSnapshot());

        assertEquals(new DatabaseStats(1, 1, 1, 1, 0), database.stats());
        assertEquals("Replacement Head", database.findById(HeadId.remote(10)).orElseThrow().name());
        assertTrue(database.findById(HeadId.remote(1)).isEmpty());
    }

    @Test
    void markFailedKeepsLoadedSnapshotAndStoresError() {
        DefaultHeadDatabase database = database();

        database.markFailed("Remote unavailable");

        assertEquals(DatabaseState.LOADED, database.status().state());
        assertEquals("Remote unavailable", database.status().lastError());
        assertEquals("Cute Cat", database.findById(HeadId.remote(1)).orElseThrow().name());
    }

    @Test
    void markLoadingChangesStatusButKeepsSnapshotReadable() {
        DefaultHeadDatabase database = database();

        database.markLoading(DatabaseSource.REMOTE);

        assertEquals(DatabaseState.LOADING, database.status().state());
        assertEquals("Cute Cat", database.findById(HeadId.remote(1)).orElseThrow().name());
    }

    private static DefaultHeadDatabase database() {
        return new DefaultHeadDatabase(snapshot());
    }

    private static DatabaseSnapshot snapshot() {
        return new DatabaseSnapshot("heads-database", "full", DatabaseSource.REMOTE, Instant.parse("2026-01-01T00:00:00Z"), heads(), categories(), tags(), collections(), revocations());
    }

    private static DatabaseSnapshot replacementSnapshot() {
        return new DatabaseSnapshot("heads-database", "full-2", DatabaseSource.REMOTE, Instant.parse("2026-01-02T00:00:00Z"), List.of(new Head(HeadId.remote(10), "Replacement Head", new HeadTexture("texture10"), "icons", Set.of("utility"), Set.of("system"))), List.of(category("icons")), List.of(tag("utility")), List.of(collection("system")), RevocationSet.empty());
    }

    private static List<Head> heads() {
        return List.of(
                new Head(HeadId.remote(1), "Cute Cat", new HeadTexture("texture1"), "animals", Set.of("cute", "pet"), Set.of("christmas")),
                new Head(HeadId.remote(2), "Dragon Block", new HeadTexture("texture2"), "blocks", Set.of("fire"), Set.of("halloween")),
                new Head(HeadId.remote(3), "Cat Icon", new HeadTexture("texture3"), "animals", Set.of("cute"), Set.of()),
                new Head(HeadId.custom("server-icon"), "Server Icon", new HeadTexture("texture4"), "icons", Set.of("utility"), Set.of()),
                new Head(HeadId.player(playerUuid()), "Player Head", new HeadTexture("texture5"), "players", Set.of("player"), Set.of())
        );
    }

    private static RevocationSet revocations() {
        return new RevocationSet(List.of(new HeadRevocation(HeadId.remote(99), "test", Instant.EPOCH)));
    }

    private static List<HeadCategory> categories() {
        return List.of(category("animals"), category("blocks"), category("icons"), category("players"));
    }

    private static HeadCategory category(String id) {
        return new HeadCategory(id, id, id + " description");
    }

    private static List<HeadTag> tags() {
        return List.of(tag("cute"), tag("pet"), tag("fire"), tag("utility"), tag("player"));
    }

    private static HeadTag tag(String id) {
        return new HeadTag(id, id, id + " description");
    }

    private static List<HeadCollection> collections() {
        return List.of(collection("christmas"), collection("halloween"), collection("system"));
    }

    private static HeadCollection collection(String id) {
        return new HeadCollection(id, id, id + " description");
    }

    private static HeadQuery query(String text, HeadSource source, String category, Set<String> tags, Set<String> collections, HeadSort sort, SortDirection direction, int offset, int limit) {
        return new HeadQuery(text, source, Set.of(), category, tags, collections, sort, direction, offset, limit);
    }

    private static List<HeadId> ids(HeadQueryResult result) {
        return result.heads().stream().map(Head::id).toList();
    }

    private static List<String> names(HeadQueryResult result) {
        return result.heads().stream().map(Head::name).toList();
    }

    private static List<String> categories(HeadQueryResult result) {
        return result.heads().stream().map(Head::category).toList();
    }

    private static java.util.UUID playerUuid() {
        return java.util.UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    }
}