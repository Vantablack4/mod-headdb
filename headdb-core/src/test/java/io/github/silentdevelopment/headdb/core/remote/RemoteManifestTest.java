package io.github.silentdevelopment.headdb.core.remote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class RemoteManifestTest {

    @Test
    void createsManifest() {
        RemoteManifest manifest = manifest();
        assertEquals("heads", manifest.id());
        assertEquals(1, manifest.schema());
        assertEquals(1, manifest.revision());
        assertEquals(Instant.parse("2026-01-01T00:00:00Z"), manifest.timestamp());
        assertEquals(1, manifest.mirrors().size());
        assertEquals(2, manifest.resources().size());
    }

    @Test
    void findsMirrorAndResourceById() {
        RemoteManifest manifest = manifest();
        assertEquals("primary", manifest.mirror(" primary ").orElseThrow().id());
        assertEquals("catalog-index", manifest.resource("catalog").orElseThrow().index().id());
        assertTrue(manifest.mirror("missing").isEmpty());
    }

    @Test
    void rejectsMissingRequiredResources() {
        assertThrows(IllegalArgumentException.class, () -> new RemoteManifest(1, "heads", 1, Instant.EPOCH, List.of(mirror("primary")), Map.of(RemoteResourceId.CATALOG, new RemoteResource(catalogIndex()))));
        assertThrows(IllegalArgumentException.class, () -> new RemoteManifest(1, "heads", 1, Instant.EPOCH, List.of(mirror("primary")), Map.of(RemoteResourceId.REVOCATIONS, new RemoteResource(revocationsIndex()))));
    }

    @Test
    void rejectsInvalidManifest() {
        assertThrows(IllegalArgumentException.class, () -> new RemoteManifest(2, "heads", 1, Instant.EPOCH, List.of(mirror("primary")), resources()));
        assertThrows(IllegalArgumentException.class, () -> new RemoteManifest(1, "", 1, Instant.EPOCH, List.of(mirror("primary")), resources()));
        assertThrows(IllegalArgumentException.class, () -> new RemoteManifest(1, "heads", -1, Instant.EPOCH, List.of(mirror("primary")), resources()));
        assertThrows(IllegalArgumentException.class, () -> new RemoteManifest(1, "heads", 1, Instant.EPOCH, List.of(), resources()));
    }

    @Test
    void rejectsNullValues() {
        assertThrows(NullPointerException.class, () -> new RemoteManifest(1, null, 1, Instant.EPOCH, List.of(mirror("primary")), resources()));
        assertThrows(NullPointerException.class, () -> new RemoteManifest(1, "heads", 1, null, List.of(mirror("primary")), resources()));
        assertThrows(NullPointerException.class, () -> new RemoteManifest(1, "heads", 1, Instant.EPOCH, null, resources()));
        assertThrows(NullPointerException.class, () -> new RemoteManifest(1, "heads", 1, Instant.EPOCH, List.of(mirror("primary")), null));
    }

    private static RemoteManifest manifest() {
        return new RemoteManifest(1, " heads ", 1, Instant.parse("2026-01-01T00:00:00Z"), List.of(mirror("primary")), resources());
    }

    private static Map<String, RemoteResource> resources() {
        return Map.of(RemoteResourceId.CATALOG, new RemoteResource(catalogIndex()), RemoteResourceId.REVOCATIONS, new RemoteResource(revocationsIndex()));
    }

    private static RemoteMirror mirror(String id) {
        return new RemoteMirror(id, 0, URI.create("https://mirror.headsdb.com/"));
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
