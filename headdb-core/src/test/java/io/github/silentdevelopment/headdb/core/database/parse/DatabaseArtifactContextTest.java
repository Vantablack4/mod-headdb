package io.github.silentdevelopment.headdb.core.database.parse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.silentdevelopment.headdb.core.database.revocation.HeadRevocation;
import io.github.silentdevelopment.headdb.core.database.revocation.RevocationSet;
import io.github.silentdevelopment.headdb.database.DatabaseSource;
import io.github.silentdevelopment.headdb.model.HeadId;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

final class DatabaseArtifactContextTest {

    @Test
    void createsContext() {
        Instant loadedAt = Instant.parse("2026-01-01T00:00:00Z");
        RevocationSet revocations = revocations();
        DatabaseArtifactContext context = new DatabaseArtifactContext(" heads-database ", " full ", DatabaseSource.REMOTE, loadedAt, revocations);

        assertEquals("heads-database", context.manifestId());
        assertEquals("full", context.artifactId());
        assertEquals(DatabaseSource.REMOTE, context.source());
        assertEquals(loadedAt, context.loadedAt());
        assertEquals(revocations, context.revocations());
    }

    @Test
    void rejectsEmptyManifestId() {
        assertThrows(IllegalArgumentException.class, () -> new DatabaseArtifactContext("", "full", DatabaseSource.REMOTE, Instant.EPOCH, RevocationSet.empty()));
        assertThrows(IllegalArgumentException.class, () -> new DatabaseArtifactContext("   ", "full", DatabaseSource.REMOTE, Instant.EPOCH, RevocationSet.empty()));
    }

    @Test
    void rejectsEmptyArtifactId() {
        assertThrows(IllegalArgumentException.class, () -> new DatabaseArtifactContext("heads-database", "", DatabaseSource.REMOTE, Instant.EPOCH, RevocationSet.empty()));
        assertThrows(IllegalArgumentException.class, () -> new DatabaseArtifactContext("heads-database", "   ", DatabaseSource.REMOTE, Instant.EPOCH, RevocationSet.empty()));
    }

    @Test
    void rejectsNullValues() {
        assertThrows(NullPointerException.class, () -> new DatabaseArtifactContext(null, "full", DatabaseSource.REMOTE, Instant.EPOCH, RevocationSet.empty()));
        assertThrows(NullPointerException.class, () -> new DatabaseArtifactContext("heads-database", null, DatabaseSource.REMOTE, Instant.EPOCH, RevocationSet.empty()));
        assertThrows(NullPointerException.class, () -> new DatabaseArtifactContext("heads-database", "full", null, Instant.EPOCH, RevocationSet.empty()));
        assertThrows(NullPointerException.class, () -> new DatabaseArtifactContext("heads-database", "full", DatabaseSource.REMOTE, null, RevocationSet.empty()));
        assertThrows(NullPointerException.class, () -> new DatabaseArtifactContext("heads-database", "full", DatabaseSource.REMOTE, Instant.EPOCH, null));
    }

    private static RevocationSet revocations() {
        return new RevocationSet(List.of(new HeadRevocation(HeadId.remote(99), "test", Instant.EPOCH)));
    }
}