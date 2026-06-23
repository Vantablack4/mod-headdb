package io.github.silentdevelopment.headdb.core.database.parse;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.model.HeadTexture;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

public final class GsonCatalogPartParser {

    private final Gson gson;

    public GsonCatalogPartParser() {
        this(new Gson());
    }

    public GsonCatalogPartParser(@NotNull Gson gson) {
        this.gson = Objects.requireNonNull(gson, "gson");
    }

    public @NotNull List<Head> parse(@NotNull String json) {
        Objects.requireNonNull(json, "json");
        String normalized = json.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Catalog part JSON cannot be empty.");
        }
        CatalogPartDto dto;
        try {
            dto = gson.fromJson(normalized, CatalogPartDto.class);
        } catch (JsonSyntaxException exception) {
            throw new IllegalArgumentException("Catalog part JSON is invalid.", exception);
        }
        if (dto == null) {
            throw new IllegalArgumentException("Catalog part JSON cannot be null.");
        }
        return dto.toHeads();
    }

    private record CatalogPartDto(Integer schema, List<HeadDto> heads) {

        private @NotNull List<Head> toHeads() {
            Objects.requireNonNull(schema, "schema");
            Objects.requireNonNull(heads, "heads");
            if (schema != 1) {
                throw new IllegalArgumentException("Unsupported catalog part schema: " + schema);
            }
            return heads.stream().map(HeadDto::toHead).toList();
        }
    }

    private record HeadDto(Integer id, String name, String texture, String category, Set<String> tags, Set<String> collections) {

        private @NotNull Head toHead() {
            Objects.requireNonNull(id, "id");
            Set<String> parsedTags = Set.of();
            if (tags != null) {
                parsedTags = tags;
            }
            Set<String> parsedCollections = Set.of();
            if (collections != null) {
                parsedCollections = collections;
            }
            return new Head(HeadId.remote(id), name, new HeadTexture(texture), category, parsedTags, parsedCollections);
        }
    }
}
