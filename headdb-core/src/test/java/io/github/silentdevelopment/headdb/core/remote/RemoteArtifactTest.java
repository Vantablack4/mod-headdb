package io.github.silentdevelopment.headdb.core.remote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class RemoteArtifactTest {

    private static final String SHA256 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    @Test
    void createsArtifact() {
        RemoteArtifact artifact = artifact(" full ", " artifacts/full.json.zst ", SHA256.toUpperCase(), 128);
        assertEquals("full", artifact.id());
        assertEquals("artifacts/full.json.zst", artifact.path());
        assertEquals("application/vnd.heads.catalog-part+json", artifact.contentType());
        assertEquals("zstd", artifact.compression());
        assertEquals(SHA256, artifact.sha256());
        assertEquals(128, artifact.size());
    }

    @Test
    void rejectsUnsafePath() {
        assertThrows(IllegalArgumentException.class, () -> artifact("full", "/artifacts/full.json.zst", SHA256, 128));
        assertThrows(IllegalArgumentException.class, () -> artifact("full", "../full.json.zst", SHA256, 128));
        assertThrows(IllegalArgumentException.class, () -> artifact("full", "https://mirror.headsdb.com/full.json.zst", SHA256, 128));
    }

    @Test
    void rejectsInvalidIntegrity() {
        assertThrows(IllegalArgumentException.class, () -> artifact("full", "artifacts/full.json.zst", "abc", 128));
        assertThrows(IllegalArgumentException.class, () -> artifact("full", "artifacts/full.json.zst", "gggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggg", 128));
        assertThrows(IllegalArgumentException.class, () -> artifact("full", "artifacts/full.json.zst", SHA256, -1));
    }

    @Test
    void rejectsInvalidCompression() {
        assertThrows(IllegalArgumentException.class, () -> new RemoteArtifact("full", "artifacts/full.json.zst", "application/vnd.heads.catalog-part+json", "gzip", new RemoteIntegrity("sha256", SHA256, 128)));
    }

    @Test
    void rejectsNullValues() {
        assertThrows(NullPointerException.class, () -> new RemoteArtifact(null, "artifacts/full.json.zst", "application/vnd.heads.catalog-part+json", "zstd", new RemoteIntegrity("sha256", SHA256, 128)));
        assertThrows(NullPointerException.class, () -> new RemoteArtifact("full", null, "application/vnd.heads.catalog-part+json", "zstd", new RemoteIntegrity("sha256", SHA256, 128)));
        assertThrows(NullPointerException.class, () -> new RemoteArtifact("full", "artifacts/full.json.zst", null, "zstd", new RemoteIntegrity("sha256", SHA256, 128)));
        assertThrows(NullPointerException.class, () -> new RemoteArtifact("full", "artifacts/full.json.zst", "application/vnd.heads.catalog-part+json", null, new RemoteIntegrity("sha256", SHA256, 128)));
    }

    private static RemoteArtifact artifact(String id, String path, String sha256, long bytes) {
        return new RemoteArtifact(id, path, "application/vnd.heads.catalog-part+json", "zstd", new RemoteIntegrity("sha256", sha256, bytes));
    }
}
