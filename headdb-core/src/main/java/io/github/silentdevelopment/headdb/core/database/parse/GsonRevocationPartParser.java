package io.github.silentdevelopment.headdb.core.database.parse;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import io.github.silentdevelopment.headdb.core.database.revocation.HeadRevocation;
import io.github.silentdevelopment.headdb.model.HeadId;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class GsonRevocationPartParser {

    private final Gson gson;

    public GsonRevocationPartParser() {
        this(new Gson());
    }

    public GsonRevocationPartParser(@NotNull Gson gson) {
        this.gson = Objects.requireNonNull(gson, "gson");
    }

    public @NotNull List<HeadRevocation> parse(@NotNull String json) {
        Objects.requireNonNull(json, "json");
        String normalized = json.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Revocations part JSON cannot be empty.");
        }
        RevocationsPartDto dto;
        try {
            dto = gson.fromJson(normalized, RevocationsPartDto.class);
        } catch (JsonSyntaxException exception) {
            throw new IllegalArgumentException("Revocations part JSON is invalid.", exception);
        }
        if (dto == null) {
            throw new IllegalArgumentException("Revocations part JSON cannot be null.");
        }
        return dto.toRevocations();
    }

    private record RevocationsPartDto(Integer schema, List<RevocationDto> revocations) {

        private @NotNull List<HeadRevocation> toRevocations() {
            Objects.requireNonNull(schema, "schema");
            Objects.requireNonNull(revocations, "revocations");
            if (schema != 1) {
                throw new IllegalArgumentException("Unsupported revocations part schema: " + schema);
            }
            return revocations.stream().map(RevocationDto::toRevocation).toList();
        }
    }

    private record RevocationDto(Integer id, String reason, String revokedAt) {

        private @NotNull HeadRevocation toRevocation() {
            if (id == null) {
                throw new IllegalArgumentException("id is required");
            }
            Instant parsedRevokedAt = null;
            if (revokedAt != null && !revokedAt.isBlank()) {
                parsedRevokedAt = Instant.parse(revokedAt.trim());
            }
            return new HeadRevocation(HeadId.remote(id), reason, parsedRevokedAt);
        }
    }
}
