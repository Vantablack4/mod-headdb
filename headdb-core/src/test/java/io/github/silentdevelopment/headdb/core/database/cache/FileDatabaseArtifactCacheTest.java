package io.github.silentdevelopment.headdb.core.database.cache;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.silentdevelopment.headdb.core.compression.ZstdArtifactDecoder;
import io.github.silentdevelopment.headdb.core.database.parse.GsonCatalogIndexParser;
import io.github.silentdevelopment.headdb.core.database.parse.GsonRevocationsIndexParser;
import io.github.silentdevelopment.headdb.core.remote.ArtifactSelector;
import io.github.silentdevelopment.headdb.core.remote.RemoteManifest;
import io.github.silentdevelopment.headdb.core.remote.RemoteResourceId;
import io.github.silentdevelopment.headdb.core.remote.parse.GsonRemoteManifestParser;
import io.github.silentdevelopment.headdb.core.test.TestDistribution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

final class FileDatabaseArtifactCacheTest {

    @TempDir
    private Path tempDir;

    @Test
    void returnsEmptyWhenCacheDoesNotExist() throws Exception {
        assertTrue(cache().load().isEmpty());
    }

    @Test
    void savesAndLoadsArtifacts() throws Exception {
        FileDatabaseArtifactCache cache = cache();
        CachedDatabaseArtifacts artifacts = artifacts();
        cache.save(artifacts);
        CachedDatabaseArtifacts loaded = cache.load().orElseThrow();
        assertEquals(artifacts.manifestJson(), loaded.manifestJson());
        assertEquals("heads", loaded.manifest().id());
        assertEquals("catalog-index", loaded.catalogIndex().selection().artifact().id());
        assertEquals("revocations-index", loaded.revocationsIndex().selection().artifact().id());
        assertArrayEquals(artifacts.catalogIndex().bytes(), loaded.catalogIndex().bytes());
        assertArrayEquals(artifacts.revocationsIndex().bytes(), loaded.revocationsIndex().bytes());
        assertArrayEquals(artifacts.catalogParts().getFirst().bytes(), loaded.catalogParts().getFirst().bytes());
        assertArrayEquals(artifacts.revocationParts().getFirst().bytes(), loaded.revocationParts().getFirst().bytes());
    }

    @Test
    void replacesCacheDirectoryAtomically() throws Exception {
        FileDatabaseArtifactCache cache = cache();
        CachedDatabaseArtifacts artifacts = artifacts();

        Files.createDirectories(tempDir.resolve("artifacts"));
        Files.writeString(tempDir.resolve("catalog.json.zst"), "legacy");
        Files.writeString(tempDir.resolve("revocations.jsonl.zst"), "legacy");

        cache.save(artifacts);

        assertTrue(Files.isRegularFile(tempDir.resolve("manifest.json")));
        assertTrue(Files.isDirectory(tempDir.resolve("artifacts")));
        assertFalse(Files.exists(tempDir.resolve("catalog.json.zst")));
        assertFalse(Files.exists(tempDir.resolve("revocations.jsonl.zst")));
        assertFalse(Files.exists(tempDir.resolveSibling(tempDir.getFileName() + ".tmp")));
        assertTrue(cache.load().isPresent());
    }

    @Test
    void restoresBackupWhenCacheDirectoryIsMissing() throws Exception {
        FileDatabaseArtifactCache cache = cache();
        CachedDatabaseArtifacts artifacts = artifacts();

        cache.save(artifacts);

        Path backup = tempDir.resolveSibling(tempDir.getFileName() + ".backup");
        Files.move(tempDir, backup);

        assertTrue(cache.load().isPresent());
        assertTrue(Files.exists(tempDir));
        assertFalse(Files.exists(backup));
    }

    private FileDatabaseArtifactCache cache() {
        return new FileDatabaseArtifactCache(tempDir, new GsonRemoteManifestParser(), new ArtifactSelector("primary"), new ZstdArtifactDecoder(), new GsonCatalogIndexParser(), new GsonRevocationsIndexParser());
    }

    private static CachedDatabaseArtifacts artifacts() {
        TestDistribution distribution = TestDistribution.create();
        GsonRemoteManifestParser parser = new GsonRemoteManifestParser();
        ArtifactSelector selector = new ArtifactSelector("primary");
        RemoteManifest manifest = parser.parse(distribution.manifestJson);
        CachedArtifact catalogIndex = new CachedArtifact(selector.selectCatalogIndex(manifest), distribution.catalogIndex);
        CachedArtifact catalogPart = new CachedArtifact(selector.selectPart(manifest, RemoteResourceId.CATALOG, new GsonCatalogIndexParser().parse(new ZstdArtifactDecoder().decodeString(distribution.catalogIndex)).artifacts().getFirst()), distribution.catalogPart);
        CachedArtifact revocationsIndex = new CachedArtifact(selector.selectRevocationsIndex(manifest), distribution.revocationsIndex);
        CachedArtifact revocationsPart = new CachedArtifact(selector.selectPart(manifest, RemoteResourceId.REVOCATIONS, new GsonRevocationsIndexParser().parse(new ZstdArtifactDecoder().decodeString(distribution.revocationsIndex)).artifacts().getFirst()), distribution.revocationsPart);
        return new CachedDatabaseArtifacts(distribution.manifestJson, manifest, catalogIndex, java.util.List.of(catalogPart), revocationsIndex, java.util.List.of(revocationsPart));
    }
}
