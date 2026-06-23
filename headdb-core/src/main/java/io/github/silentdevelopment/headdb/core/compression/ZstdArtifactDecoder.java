package io.github.silentdevelopment.headdb.core.compression;

import com.github.luben.zstd.ZstdInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class ZstdArtifactDecoder {

    private static final int BUFFER_SIZE = 8192;
    private static final int DEFAULT_MAX_DECOMPRESSED_BYTES = 128 * 1024 * 1024;

    private final int maxDecompressedBytes;

    public ZstdArtifactDecoder() {
        this(DEFAULT_MAX_DECOMPRESSED_BYTES);
    }

    public ZstdArtifactDecoder(int maxDecompressedBytes) {
        if (maxDecompressedBytes <= 0) {
            throw new IllegalArgumentException("Maximum decompressed bytes must be positive.");
        }

        this.maxDecompressedBytes = maxDecompressedBytes;
    }

    public byte @NotNull [] decode(byte @NotNull [] compressed) {
        Objects.requireNonNull(compressed, "compressed");

        if (compressed.length == 0) {
            throw new IllegalArgumentException("Compressed artifact cannot be empty.");
        }

        try {
            return decodeChecked(compressed);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to decode zstd artifact.", exception);
        }
    }

    public byte @NotNull [] decode(@NotNull Path path) throws IOException {
        Objects.requireNonNull(path, "path");

        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Compressed artifact path must be a regular file: " + path);
        }

        return decode(Files.readAllBytes(path));
    }

    public @NotNull String decodeString(byte @NotNull [] compressed) {
        return new String(decode(compressed), StandardCharsets.UTF_8);
    }

    public @NotNull String decodeString(@NotNull Path path) throws IOException {
        return new String(decode(path), StandardCharsets.UTF_8);
    }

    private byte @NotNull [] decodeChecked(byte @NotNull [] compressed) throws IOException {
        try (ZstdInputStream input = new ZstdInputStream(new ByteArrayInputStream(compressed)); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[BUFFER_SIZE];

            while (true) {
                int read = input.read(buffer);
                if (read == -1) {
                    break;
                }

                if (output.size() + read > maxDecompressedBytes) {
                    throw new IllegalArgumentException("Decoded zstd artifact exceeds maximum size of " + maxDecompressedBytes + " bytes.");
                }

                output.write(buffer, 0, read);
            }

            return output.toByteArray();
        }
    }
}