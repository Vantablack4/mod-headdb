package io.github.silentdevelopment.headdb.core.remote;

import java.net.URI;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public record RemoteMirror(@NotNull String id, int priority, @NotNull URI url) {

    public RemoteMirror(@NotNull String id, @NotNull URI url) {
        this(id, 0, url);
    }

    public RemoteMirror {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(url, "url");
        id = id.trim();
        if (id.isEmpty()) {
            throw new IllegalArgumentException("Remote mirror ID cannot be empty.");
        }
        if (priority < 0) {
            throw new IllegalArgumentException("Remote mirror priority cannot be negative.");
        }
        if (!url.isAbsolute()) {
            throw new IllegalArgumentException("Remote mirror URL must be absolute.");
        }
    }

    public @NotNull URI resolve(@NotNull String path) {
        Objects.requireNonNull(path, "path");
        RemoteArtifact.validatePath(path);
        return url.resolve(path.trim());
    }
}
