package io.github.silentdevelopment.headdb.core.remote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import org.junit.jupiter.api.Test;

final class RemoteMirrorTest {

    @Test
    void createsMirror() {
        RemoteMirror mirror = new RemoteMirror(" primary ", URI.create("https://data.headsdb.com/"));

        assertEquals("primary", mirror.id());
        assertEquals(URI.create("https://data.headsdb.com/"), mirror.url());
    }

    @Test
    void resolvesArtifactPath() {
        RemoteMirror mirror = new RemoteMirror("primary", URI.create("https://data.headsdb.com/"));

        assertEquals(URI.create("https://data.headsdb.com/artifacts/full.json.zst"), mirror.resolve("artifacts/full.json.zst"));
    }

    @Test
    void rejectsEmptyMirrorId() {
        assertThrows(IllegalArgumentException.class, () -> new RemoteMirror("", URI.create("https://data.headsdb.com/")));
        assertThrows(IllegalArgumentException.class, () -> new RemoteMirror("   ", URI.create("https://data.headsdb.com/")));
    }

    @Test
    void rejectsRelativeMirrorUrl() {
        assertThrows(IllegalArgumentException.class, () -> new RemoteMirror("primary", URI.create("artifacts/")));
    }

    @Test
    void rejectsEmptyResolvePath() {
        RemoteMirror mirror = new RemoteMirror("primary", URI.create("https://data.headsdb.com/"));

        assertThrows(IllegalArgumentException.class, () -> mirror.resolve(""));
        assertThrows(IllegalArgumentException.class, () -> mirror.resolve("   "));
    }

    @Test
    void rejectsNullValues() {
        assertThrows(NullPointerException.class, () -> new RemoteMirror(null, URI.create("https://data.headsdb.com/")));
        assertThrows(NullPointerException.class, () -> new RemoteMirror("primary", null));
    }
}