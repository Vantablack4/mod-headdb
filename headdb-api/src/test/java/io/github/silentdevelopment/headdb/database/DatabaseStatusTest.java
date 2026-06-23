package io.github.silentdevelopment.headdb.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

final class DatabaseStatusTest {

    @Test
    void createsNotLoadedStatus() {
        DatabaseStatus status = DatabaseStatus.notLoaded();

        assertEquals(DatabaseState.NOT_LOADED, status.state());
        assertEquals(DatabaseSource.NONE, status.source());
        assertEquals(DatabaseStats.empty(), status.stats());
        assertNull(status.loadedAt());
        assertNull(status.manifestId());
        assertNull(status.artifactId());
        assertNull(status.lastError());
        assertFalse(status.available());
    }

    @Test
    void createsLoadingStatus() {
        DatabaseStatus status = DatabaseStatus.loading(DatabaseSource.REMOTE);

        assertEquals(DatabaseState.LOADING, status.state());
        assertEquals(DatabaseSource.REMOTE, status.source());
        assertFalse(status.available());
    }

    @Test
    void createsLoadedStatus() {
        Instant loadedAt = Instant.parse("2026-01-01T00:00:00Z");
        DatabaseStats stats = new DatabaseStats(10, 2, 3, 4, 1);
        DatabaseStatus status = DatabaseStatus.loaded(DatabaseSource.REMOTE, stats, loadedAt, "heads-database", "full");

        assertEquals(DatabaseState.LOADED, status.state());
        assertEquals(DatabaseSource.REMOTE, status.source());
        assertEquals(stats, status.stats());
        assertEquals(loadedAt, status.loadedAt());
        assertEquals("heads-database", status.manifestId());
        assertEquals("full", status.artifactId());
        assertNull(status.lastError());
        assertTrue(status.available());
    }

    @Test
    void createsFailedStatus() {
        DatabaseStatus status = DatabaseStatus.failed("Network failed");

        assertEquals(DatabaseState.FAILED, status.state());
        assertEquals(DatabaseSource.NONE, status.source());
        assertEquals("Network failed", status.lastError());
        assertFalse(status.available());
    }

    @Test
    void trimsError() {
        DatabaseStatus status = DatabaseStatus.failed(" Network failed ");

        assertEquals("Network failed", status.lastError());
    }

    @Test
    void rejectsEmptyError() {
        assertThrows(IllegalArgumentException.class, () -> DatabaseStatus.failed(""));
        assertThrows(IllegalArgumentException.class, () -> DatabaseStatus.failed("   "));
    }

    @Test
    void addsLastErrorToExistingStatus() {
        DatabaseStatus status = DatabaseStatus.notLoaded().withLastError("Remote unavailable");

        assertEquals("Remote unavailable", status.lastError());
    }

    @Test
    void rejectsEmptyManifestOrArtifactId() {
        Instant loadedAt = Instant.parse("2026-01-01T00:00:00Z");
        DatabaseStats stats = DatabaseStats.empty();

        assertThrows(IllegalArgumentException.class, () -> DatabaseStatus.loaded(DatabaseSource.REMOTE, stats, loadedAt, "", "full"));
        assertThrows(IllegalArgumentException.class, () -> DatabaseStatus.loaded(DatabaseSource.REMOTE, stats, loadedAt, "heads-database", ""));
    }
}