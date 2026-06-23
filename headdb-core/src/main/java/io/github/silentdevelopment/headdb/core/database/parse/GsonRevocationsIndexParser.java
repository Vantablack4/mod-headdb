package io.github.silentdevelopment.headdb.core.database.parse;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import io.github.silentdevelopment.headdb.core.remote.RemoteArtifact;
import io.github.silentdevelopment.headdb.core.remote.RemoteContentTypes;
import io.github.silentdevelopment.headdb.core.remote.RemoteIntegrity;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class GsonRevocationsIndexParser {

    private final Gson gson;

    public GsonRevocationsIndexParser() {
        this(new Gson());
    }

    public GsonRevocationsIndexParser(@NotNull Gson gson) {
        this.gson = Objects.requireNonNull(gson, "gson");
    }

    public @NotNull ParsedRevocationsIndex parse(@NotNull String json) {
        Objects.requireNonNull(json, "json");
        String normalized = json.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Revocations index JSON cannot be empty.");
        }
        RevocationsIndexDto dto;
        try {
            dto = gson.fromJson(normalized, RevocationsIndexDto.class);
        } catch (JsonSyntaxException exception) {
            throw new IllegalArgumentException("Revocations index JSON is invalid.", exception);
        }
        if (dto == null) {
            throw new IllegalArgumentException("Revocations index JSON cannot be null.");
        }
        return dto.toIndex();
    }

    private record RevocationsIndexDto(Integer schema, String id, List<PartDto> artifacts) {

        private @NotNull ParsedRevocationsIndex toIndex() {
            Objects.requireNonNull(schema, "schema");
            Objects.requireNonNull(artifacts, "artifacts");
            return new ParsedRevocationsIndex(schema, id, artifacts.stream().map(part -> part.toArtifact(RemoteContentTypes.REVOCATIONS_PART)).toList());
        }
    }

    private record PartDto(Integer index, String id, String path, String contentType, String compression, IntegrityDto integrity) {

        private @NotNull RemoteArtifact toArtifact(@NotNull String expectedContentType) {
            Objects.requireNonNull(index, "index");
            Objects.requireNonNull(integrity, "integrity");
            if (index < 0) {
                throw new IllegalArgumentException("Part index cannot be negative.");
            }
            if (!Objects.equals(contentType, expectedContentType)) {
                throw new IllegalArgumentException("Unexpected part content type: " + contentType);
            }
            return new RemoteArtifact(index, id, path, contentType, compression, integrity.toIntegrity());
        }
    }

    private record IntegrityDto(String algorithm, String digest, Long bytes) {

        private @NotNull RemoteIntegrity toIntegrity() {
            Objects.requireNonNull(bytes, "bytes");
            return new RemoteIntegrity(algorithm, digest, bytes);
        }
    }
}
