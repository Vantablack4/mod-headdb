package io.github.silentdevelopment.headdb.core.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.silentdevelopment.headdb.core.database.revocation.HeadRevocation;
import io.github.silentdevelopment.headdb.core.database.revocation.RevocationSet;
import io.github.silentdevelopment.headdb.database.DatabaseSource;
import io.github.silentdevelopment.headdb.database.DatabaseState;
import io.github.silentdevelopment.headdb.database.DatabaseStatus;
import io.github.silentdevelopment.headdb.database.DatabaseStats;
import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadCategory;
import io.github.silentdevelopment.headdb.model.HeadCollection;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.model.HeadTag;
import io.github.silentdevelopment.headdb.model.HeadTexture;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class DatabaseSnapshotTest {

    @Test
    void buildsIndexesAndStats() {
        DatabaseSnapshot snapshot = snapshot();

        assertEquals(head(1), snapshot.headIndex().get(HeadId.remote(1)));
        assertEquals(category("animals"), snapshot.categoryIndex().get("animals"));
        assertEquals(tag("cute"), snapshot.tagIndex().get("cute"));
        assertEquals(collection("christmas"), snapshot.collectionIndex().get("christmas"));
        assertEquals(new DatabaseStats(3, 2, 3, 2, 1), snapshot.stats());
    }

    @Test
    void exposesLoadedStatus() {
        DatabaseSnapshot snapshot = snapshot();
        DatabaseStatus status = snapshot.status();

        assertEquals(DatabaseState.LOADED, status.state());
        assertEquals(DatabaseSource.REMOTE, status.source());
        assertEquals(snapshot.stats(), status.stats());
        assertEquals(Instant.parse("2026-01-01T00:00:00Z"), status.loadedAt());
        assertEquals("heads-database", status.manifestId());
        assertEquals("full", status.artifactId());
    }

    @Test
    void rejectsDuplicateHeadIds() {
        List<Head> heads = List.of(head(1), head(1));

        assertThrows(IllegalArgumentException.class, () -> new DatabaseSnapshot("manifest", "artifact", DatabaseSource.REMOTE, Instant.EPOCH, heads, categories(), tags(), collections(), revocations()));
    }

    @Test
    void rejectsDuplicateCategoryIds() {
        List<HeadCategory> categories = List.of(category("animals"), category("ANIMALS"));

        assertThrows(IllegalArgumentException.class, () -> new DatabaseSnapshot("manifest", "artifact", DatabaseSource.REMOTE, Instant.EPOCH, List.of(head(1)), categories, tags(), collections(), revocations()));
    }

    @Test
    void rejectsDuplicateTagIds() {
        List<HeadTag> tags = List.of(tag("cute"), tag("CUTE"));

        assertThrows(IllegalArgumentException.class, () -> new DatabaseSnapshot("manifest", "artifact", DatabaseSource.REMOTE, Instant.EPOCH, List.of(head(1)), categories(), tags, collections(), revocations()));
    }

    @Test
    void rejectsDuplicateCollectionIds() {
        List<HeadCollection> collections = List.of(collection("christmas"), collection("CHRISTMAS"));

        assertThrows(IllegalArgumentException.class, () -> new DatabaseSnapshot("manifest", "artifact", DatabaseSource.REMOTE, Instant.EPOCH, List.of(head(1)), categories(), tags(), collections, revocations()));
    }

    @Test
    void rejectsUnknownCategoryReference() {
        Head head = new Head(HeadId.remote(1), "Unknown Category", new HeadTexture("texture1"), "missing", Set.of("cute"), Set.of("christmas"));

        assertThrows(IllegalArgumentException.class, () -> new DatabaseSnapshot("manifest", "artifact", DatabaseSource.REMOTE, Instant.EPOCH, List.of(head), categories(), tags(), collections(), revocations()));
    }

    @Test
    void rejectsUnknownTagReference() {
        Head head = new Head(HeadId.remote(1), "Unknown Tag", new HeadTexture("texture1"), "animals", Set.of("missing"), Set.of("christmas"));

        assertThrows(IllegalArgumentException.class, () -> new DatabaseSnapshot("manifest", "artifact", DatabaseSource.REMOTE, Instant.EPOCH, List.of(head), categories(), tags(), collections(), revocations()));
    }

    @Test
    void rejectsUnknownCollectionReference() {
        Head head = new Head(HeadId.remote(1), "Unknown Collection", new HeadTexture("texture1"), "animals", Set.of("cute"), Set.of("missing"));

        assertThrows(IllegalArgumentException.class, () -> new DatabaseSnapshot("manifest", "artifact", DatabaseSource.REMOTE, Instant.EPOCH, List.of(head), categories(), tags(), collections(), revocations()));
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void rejectsNullRevocationSet() {
        assertThrows(NullPointerException.class, () -> new DatabaseSnapshot("manifest", "artifact", DatabaseSource.REMOTE, Instant.EPOCH, heads(), categories(), tags(), collections(), null));
    }

    @Test
    void rejectsEmptyManifestOrArtifactId() {
        assertThrows(IllegalArgumentException.class, () -> new DatabaseSnapshot("", "artifact", DatabaseSource.REMOTE, Instant.EPOCH, heads(), categories(), tags(), collections(), revocations()));
        assertThrows(IllegalArgumentException.class, () -> new DatabaseSnapshot("manifest", "", DatabaseSource.REMOTE, Instant.EPOCH, heads(), categories(), tags(), collections(), revocations()));
    }

    private static DatabaseSnapshot snapshot() {
        return new DatabaseSnapshot("heads-database", "full", DatabaseSource.REMOTE, Instant.parse("2026-01-01T00:00:00Z"), heads(), categories(), tags(), collections(), revocations());
    }

    private static RevocationSet revocations() {
        return new RevocationSet(List.of(new HeadRevocation(HeadId.remote(99), "test", Instant.EPOCH)));
    }

    private static List<Head> heads() {
        return List.of(head(1), head(2), head(3));
    }

    private static Head head(int id) {
        if (id == 1) {
            return new Head(HeadId.remote(1), "Cute Cat", new HeadTexture("texture1"), "animals", Set.of("cute", "pet"), Set.of("christmas"));
        }

        if (id == 2) {
            return new Head(HeadId.remote(2), "Dragon Block", new HeadTexture("texture2"), "blocks", Set.of("fire"), Set.of("halloween"));
        }

        return new Head(HeadId.remote(3), "Cat Icon", new HeadTexture("texture3"), "animals", Set.of("cute"), Set.of());
    }

    private static List<HeadCategory> categories() {
        return List.of(category("animals"), category("blocks"));
    }

    private static HeadCategory category(String id) {
        return new HeadCategory(id, id, id + " description");
    }

    private static List<HeadTag> tags() {
        return List.of(tag("cute"), tag("pet"), tag("fire"));
    }

    private static HeadTag tag(String id) {
        return new HeadTag(id, id, id + " description");
    }

    private static List<HeadCollection> collections() {
        return List.of(collection("christmas"), collection("halloween"));
    }

    private static HeadCollection collection(String id) {
        return new HeadCollection(id, id, id + " description");
    }
}