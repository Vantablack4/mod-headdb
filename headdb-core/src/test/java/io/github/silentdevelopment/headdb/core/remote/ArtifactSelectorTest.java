package io.github.silentdevelopment.headdb.core.remote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class ArtifactSelectorTest {

    @Test
    void selectsCatalogIndexFromPreferredMirror() {
        ArtifactSelector selector = new ArtifactSelector("primary");
        ArtifactSelection selection = selector.selectCatalogIndex(manifest());
        assertEquals("primary", selection.mirror().id());
        assertEquals("catalog-index", selection.artifact().id());
        assertEquals(URI.create("https://mirror.headsdb.com/artifacts/catalog/index.json.zst"), selection.url());
    }

    @Test
    void selectsRevocationsIndexFromPreferredMirror() {
        ArtifactSelector selector = new ArtifactSelector("primary");
        ArtifactSelection selection = selector.selectRevocationsIndex(manifest());
        assertEquals("primary", selection.mirror().id());
        assertEquals("revocations-index", selection.artifact().id());
        assertEquals(URI.create("https://mirror.headsdb.com/artifacts/revocations/index.json.zst"), selection.url());
    }

    @Test
    void fallsBackToFirstMirrorWhenPreferredMirrorMissing() {
        ArtifactSelector selector = new ArtifactSelector("missing");
        ArtifactSelection selection = selector.selectCatalogIndex(manifest());
        assertEquals("primary", selection.mirror().id());
        assertEquals(URI.create("https://mirror.headsdb.com/artifacts/catalog/index.json.zst"), selection.url());
    }

    @Test
    void rejectsMissingResource() {
        ArtifactSelector selector = new ArtifactSelector("primary");
        assertThrows(IllegalArgumentException.class, () -> selector.selectResourceIndex(manifest(), "missing"));
    }

    @Test
    void rejectsEmptyPreferredMirrorId() {
        assertThrows(IllegalArgumentException.class, () -> new ArtifactSelector(""));
        assertThrows(IllegalArgumentException.class, () -> new ArtifactSelector("   "));
    }

    @Test
    void rejectsNullValues() {
        ArtifactSelector selector = new ArtifactSelector("primary");
        assertThrows(NullPointerException.class, () -> new ArtifactSelector(null));
        assertThrows(NullPointerException.class, () -> selector.selectCatalogIndex(null));
        assertThrows(NullPointerException.class, () -> selector.selectRevocationsIndex(null));
        assertThrows(NullPointerException.class, () -> selector.selectResourceIndex(null, RemoteResourceId.CATALOG));
        assertThrows(NullPointerException.class, () -> selector.selectResourceIndex(manifest(), null));
        assertThrows(NullPointerException.class, () -> selector.selectPart(manifest(), RemoteResourceId.CATALOG, null));
    }

    private static RemoteManifest manifest() {
        return new RemoteManifest(1, "heads", 1, Instant.parse("2026-01-01T00:00:00Z"), List.of(mirror("primary", "https://mirror.headsdb.com/"), mirror("backup", "https://backup.headsdb.com/")), Map.of(RemoteResourceId.CATALOG, new RemoteResource(catalogIndex()), RemoteResourceId.REVOCATIONS, new RemoteResource(revocationsIndex())));
    }

    private static RemoteMirror mirror(String id, String url) {
        return new RemoteMirror(id, 0, URI.create(url));
    }

    private static RemoteArtifact catalogIndex() {
        return new RemoteArtifact("catalog-index", "artifacts/catalog/index.json.zst", RemoteContentTypes.CATALOG_INDEX, "zstd", new RemoteIntegrity("sha256", sha("a"), 128));
    }

    private static RemoteArtifact revocationsIndex() {
        return new RemoteArtifact("revocations-index", "artifacts/revocations/index.json.zst", RemoteContentTypes.REVOCATIONS_INDEX, "zstd", new RemoteIntegrity("sha256", sha("b"), 64));
    }

    private static String sha(String character) {
        return character.repeat(64);
    }
}
