package io.github.silentdevelopment.headdb.core.remote;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public record RemoteManifest(int schema, @NotNull String id, long revision, @NotNull Instant timestamp, @NotNull List<RemoteMirror> mirrors, @NotNull Map<String, RemoteResource> resources) {

    public static final int SUPPORTED_SCHEMA = 1;

    public RemoteManifest {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(timestamp, "timestamp");
        Objects.requireNonNull(mirrors, "mirrors");
        Objects.requireNonNull(resources, "resources");
        id = id.trim();
        if (schema != SUPPORTED_SCHEMA) {
            throw new IllegalArgumentException("Unsupported remote manifest schema: " + schema);
        }
        if (id.isEmpty()) {
            throw new IllegalArgumentException("Remote manifest ID cannot be empty.");
        }
        if (revision < 0) {
            throw new IllegalArgumentException("Remote manifest revision cannot be negative.");
        }
        mirrors = mirrors.stream().sorted(Comparator.comparingInt(RemoteMirror::priority)).toList();
        if (mirrors.isEmpty()) {
            throw new IllegalArgumentException("Remote manifest must contain at least one mirror.");
        }
        resources = copyResources(resources);
        if (!resources.containsKey(RemoteResourceId.CATALOG)) {
            throw new IllegalArgumentException("Remote manifest is missing required catalog resource.");
        }
        if (!resources.containsKey(RemoteResourceId.REVOCATIONS)) {
            throw new IllegalArgumentException("Remote manifest is missing required revocations resource.");
        }
    }

    public @NotNull Optional<RemoteMirror> mirror(@NotNull String id) {
        Objects.requireNonNull(id, "id");
        String normalized = id.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Remote mirror ID cannot be empty.");
        }
        return mirrors.stream().filter(mirror -> mirror.id().equals(normalized)).findFirst();
    }

    public @NotNull Optional<RemoteResource> resource(@NotNull String id) {
        Objects.requireNonNull(id, "id");
        String normalized = id.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Remote resource ID cannot be empty.");
        }
        return Optional.ofNullable(resources.get(normalized));
    }

    public @NotNull RemoteResource catalogResource() {
        return resource(RemoteResourceId.CATALOG).orElseThrow(() -> new IllegalArgumentException("Remote manifest is missing required catalog resource."));
    }

    public @NotNull RemoteResource revocationsResource() {
        return resource(RemoteResourceId.REVOCATIONS).orElseThrow(() -> new IllegalArgumentException("Remote manifest is missing required revocations resource."));
    }

    private static @NotNull Map<String, RemoteResource> copyResources(@NotNull Map<String, RemoteResource> resources) {
        Map<String, RemoteResource> copy = new LinkedHashMap<>();
        for (Map.Entry<String, RemoteResource> entry : resources.entrySet()) {
            Objects.requireNonNull(entry.getKey(), "resource key");
            Objects.requireNonNull(entry.getValue(), "resource value");
            String key = entry.getKey().trim();
            if (key.isEmpty()) {
                throw new IllegalArgumentException("Remote resource ID cannot be empty.");
            }
            if (copy.put(key, entry.getValue()) != null) {
                throw new IllegalArgumentException("Duplicate remote resource ID: " + key);
            }
        }
        return Map.copyOf(copy);
    }
}
