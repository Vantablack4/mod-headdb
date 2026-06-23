package io.github.silentdevelopment.headdb.core.remote.parse;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import io.github.silentdevelopment.headdb.core.remote.RemoteArtifact;
import io.github.silentdevelopment.headdb.core.remote.RemoteIntegrity;
import io.github.silentdevelopment.headdb.core.remote.RemoteManifest;
import io.github.silentdevelopment.headdb.core.remote.RemoteMirror;
import io.github.silentdevelopment.headdb.core.remote.RemoteResource;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class GsonRemoteManifestParser implements RemoteManifestParser {

    private final Gson gson;

    public GsonRemoteManifestParser() {
        this(new Gson());
    }

    public GsonRemoteManifestParser(@NotNull Gson gson) {
        this.gson = Objects.requireNonNull(gson, "gson");
    }

    @Override
    public @NotNull RemoteManifest parse(@NotNull String json) {
        Objects.requireNonNull(json, "json");
        String normalized = json.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Remote manifest JSON cannot be empty.");
        }
        ManifestDto dto;
        try {
            dto = gson.fromJson(normalized, ManifestDto.class);
        } catch (JsonSyntaxException exception) {
            throw new IllegalArgumentException("Remote manifest JSON is invalid.", exception);
        }
        if (dto == null) {
            throw new IllegalArgumentException("Remote manifest JSON cannot be null.");
        }
        return dto.toManifest();
    }

    private record ManifestDto(Integer schema, String id, Long revision, String timestamp, List<MirrorDto> mirrors, Map<String, ResourceDto> resources) {

        private @NotNull RemoteManifest toManifest() {
            Objects.requireNonNull(schema, "schema");
            Objects.requireNonNull(timestamp, "timestamp");
            Objects.requireNonNull(mirrors, "mirrors");
            Objects.requireNonNull(resources, "resources");
            long parsedRevision = 0L;
            if (revision != null) {
                parsedRevision = revision;
            }
            List<RemoteMirror> parsedMirrors = mirrors.stream().map(MirrorDto::toMirror).toList();
            Map<String, RemoteResource> parsedResources = new LinkedHashMap<>();
            for (Map.Entry<String, ResourceDto> entry : resources.entrySet()) {
                parsedResources.put(entry.getKey(), entry.getValue().toResource(entry.getKey()));
            }
            return new RemoteManifest(schema, id, parsedRevision, Instant.parse(timestamp), parsedMirrors, parsedResources);
        }
    }

    private record MirrorDto(String id, Integer priority, String url) {

        private @NotNull RemoteMirror toMirror() {
            Objects.requireNonNull(url, "url");
            int parsedPriority = 0;
            if (priority != null) {
                parsedPriority = priority;
            }
            return new RemoteMirror(id, parsedPriority, URI.create(url));
        }
    }

    private record ResourceDto(ArtifactDto index) {

        private @NotNull RemoteResource toResource(@NotNull String id) {
            Objects.requireNonNull(index, "index");
            return new RemoteResource(index.toArtifact(id + "-index"));
        }
    }

    private record ArtifactDto(String path, String contentType, String compression, IntegrityDto integrity) {

        private @NotNull RemoteArtifact toArtifact(@NotNull String id) {
            Objects.requireNonNull(integrity, "integrity");
            return new RemoteArtifact(id, path, contentType, compression, integrity.toIntegrity());
        }
    }

    private record IntegrityDto(String algorithm, String digest, Long bytes) {

        private @NotNull RemoteIntegrity toIntegrity() {
            Objects.requireNonNull(bytes, "bytes");
            return new RemoteIntegrity(algorithm, digest, bytes);
        }
    }
}
