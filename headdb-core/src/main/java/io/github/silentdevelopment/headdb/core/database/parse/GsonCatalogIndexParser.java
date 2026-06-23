package io.github.silentdevelopment.headdb.core.database.parse;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import io.github.silentdevelopment.headdb.core.remote.RemoteArtifact;
import io.github.silentdevelopment.headdb.core.remote.RemoteContentTypes;
import io.github.silentdevelopment.headdb.core.remote.RemoteIntegrity;
import io.github.silentdevelopment.headdb.model.HeadCategory;
import io.github.silentdevelopment.headdb.model.HeadCollection;
import io.github.silentdevelopment.headdb.model.HeadTag;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class GsonCatalogIndexParser {

    private final Gson gson;

    public GsonCatalogIndexParser() {
        this(new Gson());
    }

    public GsonCatalogIndexParser(@NotNull Gson gson) {
        this.gson = Objects.requireNonNull(gson, "gson");
    }

    public @NotNull ParsedCatalogIndex parse(@NotNull String json) {
        Objects.requireNonNull(json, "json");
        String normalized = json.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Catalog index JSON cannot be empty.");
        }
        CatalogIndexDto dto;
        try {
            dto = gson.fromJson(normalized, CatalogIndexDto.class);
        } catch (JsonSyntaxException exception) {
            throw new IllegalArgumentException("Catalog index JSON is invalid.", exception);
        }
        if (dto == null) {
            throw new IllegalArgumentException("Catalog index JSON cannot be null.");
        }
        return dto.toIndex();
    }

    private record CatalogIndexDto(Integer schema, String id, List<CategoryDto> categories, List<TagDto> tags, List<CollectionDto> collections, List<PartDto> artifacts) {

        private @NotNull ParsedCatalogIndex toIndex() {
            Objects.requireNonNull(schema, "schema");
            Objects.requireNonNull(categories, "categories");
            Objects.requireNonNull(tags, "tags");
            Objects.requireNonNull(collections, "collections");
            Objects.requireNonNull(artifacts, "artifacts");
            return new ParsedCatalogIndex(schema, id, categories.stream().map(CategoryDto::toCategory).toList(), tags.stream().map(TagDto::toTag).toList(), collections.stream().map(CollectionDto::toCollection).toList(), artifacts.stream().map(part -> part.toArtifact(RemoteContentTypes.CATALOG_PART)).toList());
        }
    }

    private record CategoryDto(String id, String name, String description) {

        private @NotNull HeadCategory toCategory() {
            return new HeadCategory(id, name, description);
        }
    }

    private record TagDto(String id, String name, String description) {

        private @NotNull HeadTag toTag() {
            return new HeadTag(id, name, description);
        }
    }

    private record CollectionDto(String id, String name, String description) {

        private @NotNull HeadCollection toCollection() {
            return new HeadCollection(id, name, description);
        }
    }

    record PartDto(Integer index, String id, String path, String contentType, String compression, IntegrityDto integrity) {

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

    record IntegrityDto(String algorithm, String digest, Long bytes) {

        private @NotNull RemoteIntegrity toIntegrity() {
            Objects.requireNonNull(bytes, "bytes");
            return new RemoteIntegrity(algorithm, digest, bytes);
        }
    }
}
