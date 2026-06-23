package io.github.silentdevelopment.headdb.core.hash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class Sha256VerifierTest {

    private final Sha256Verifier verifier = new Sha256Verifier();

    @TempDir
    private Path tempDir;

    @Test
    void hashesBytes() {
        String hash = verifier.hash("hello".getBytes(StandardCharsets.UTF_8));

        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", hash);
    }

    @Test
    void hashesFile() throws Exception {
        Path file = tempDir.resolve("artifact.zst");
        Files.writeString(file, "hello", StandardCharsets.UTF_8);

        String hash = verifier.hash(file);

        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", hash);
    }

    @Test
    void verifiesBytes() {
        byte[] bytes = "hello".getBytes(StandardCharsets.UTF_8);

        assertTrue(verifier.verify(bytes, "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"));
        assertTrue(verifier.verify(bytes, " 2CF24DBA5FB0A30E26E83B2AC5B9E29E1B161E5C1FA7425E73043362938B9824 "));
    }

    @Test
    void rejectsMismatchedBytes() {
        byte[] bytes = "hello".getBytes(StandardCharsets.UTF_8);

        assertFalse(verifier.verify(bytes, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"));
    }

    @Test
    void verifiesFile() throws Exception {
        Path file = tempDir.resolve("artifact.zst");
        Files.writeString(file, "hello", StandardCharsets.UTF_8);

        assertTrue(verifier.verify(file, "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"));
    }

    @Test
    void rejectsInvalidShaLength() {
        byte[] bytes = "hello".getBytes(StandardCharsets.UTF_8);

        assertThrows(IllegalArgumentException.class, () -> verifier.verify(bytes, "abc"));
    }

    @Test
    void rejectsNonHexSha() {
        byte[] bytes = "hello".getBytes(StandardCharsets.UTF_8);

        assertThrows(IllegalArgumentException.class, () -> verifier.verify(bytes, "gggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggg"));
    }

    @Test
    void rejectsEmptySha() {
        byte[] bytes = "hello".getBytes(StandardCharsets.UTF_8);

        assertThrows(IllegalArgumentException.class, () -> verifier.verify(bytes, ""));
        assertThrows(IllegalArgumentException.class, () -> verifier.verify(bytes, "   "));
    }

    @Test
    void rejectsDirectoryHashing() {
        assertThrows(IllegalArgumentException.class, () -> verifier.hash(tempDir));
    }

    @Test
    void rejectsNullValues() {
        byte[] bytes = "hello".getBytes(StandardCharsets.UTF_8);

        assertThrows(NullPointerException.class, () -> verifier.hash((byte[]) null));
        assertThrows(NullPointerException.class, () -> verifier.hash((Path) null));
        assertThrows(NullPointerException.class, () -> verifier.verify((byte[]) null, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"));
        assertThrows(NullPointerException.class, () -> verifier.verify(bytes, null));
        assertThrows(NullPointerException.class, () -> verifier.verify((Path) null, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"));
    }
}