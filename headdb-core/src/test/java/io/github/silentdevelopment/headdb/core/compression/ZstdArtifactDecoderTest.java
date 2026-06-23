package io.github.silentdevelopment.headdb.core.compression;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.luben.zstd.Zstd;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ZstdArtifactDecoderTest {

    private final ZstdArtifactDecoder decoder = new ZstdArtifactDecoder();

    @TempDir
    private Path tempDir;

    @Test
    void decodesBytes() {
        byte[] original = "{\"id\":\"heads-database\"}".getBytes(StandardCharsets.UTF_8);
        byte[] compressed = Zstd.compress(original);

        byte[] decoded = decoder.decode(compressed);

        assertArrayEquals(original, decoded);
    }

    @Test
    void decodesString() {
        String original = "{\"id\":\"heads-database\"}";
        byte[] compressed = Zstd.compress(original.getBytes(StandardCharsets.UTF_8));

        String decoded = decoder.decodeString(compressed);

        assertEquals(original, decoded);
    }

    @Test
    void decodesFile() throws Exception {
        String original = "{\"id\":\"heads-database\"}";
        byte[] compressed = Zstd.compress(original.getBytes(StandardCharsets.UTF_8));
        Path file = tempDir.resolve("manifest.json.zst");

        Files.write(file, compressed);

        String decoded = decoder.decodeString(file);

        assertEquals(original, decoded);
    }

    @Test
    void rejectsEmptyCompressedBytes() {
        assertThrows(IllegalArgumentException.class, () -> decoder.decode(new byte[0]));
    }

    @Test
    void rejectsInvalidCompressedBytes() {
        byte[] invalid = "not-zstd".getBytes(StandardCharsets.UTF_8);

        assertThrows(IllegalArgumentException.class, () -> decoder.decode(invalid));
    }

    @Test
    void rejectsExceededMaxDecompressedSize() {
        ZstdArtifactDecoder limitedDecoder = new ZstdArtifactDecoder(5);
        byte[] compressed = Zstd.compress("larger-than-five".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalArgumentException.class, () -> limitedDecoder.decode(compressed));
    }

    @Test
    void rejectsInvalidMaxDecompressedSize() {
        assertThrows(IllegalArgumentException.class, () -> new ZstdArtifactDecoder(0));
        assertThrows(IllegalArgumentException.class, () -> new ZstdArtifactDecoder(-1));
    }

    @Test
    void rejectsDirectoryPath() {
        assertThrows(IllegalArgumentException.class, () -> decoder.decode(tempDir));
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void rejectsNullValues() {
        assertThrows(NullPointerException.class, () -> decoder.decode((byte[]) null));
        assertThrows(NullPointerException.class, () -> decoder.decode((Path) null));
        assertThrows(NullPointerException.class, () -> decoder.decodeString((byte[]) null));
        assertThrows(NullPointerException.class, () -> decoder.decodeString((Path) null));
    }

}