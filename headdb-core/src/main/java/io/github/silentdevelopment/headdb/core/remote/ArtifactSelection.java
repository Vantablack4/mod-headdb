package io.github.silentdevelopment.headdb.core.remote;

import java.net.URI;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public record ArtifactSelection(@NotNull RemoteMirror mirror, @NotNull String resourceId, @NotNull RemoteArtifact artifact, @NotNull URI url) {

    public ArtifactSelection {
        Objects.requireNonNull(mirror, "mirror");
        Objects.requireNonNull(resourceId, "resourceId");
        Objects.requireNonNull(artifact, "artifact");
        Objects.requireNonNull(url, "url");
        resourceId = resourceId.trim();
        if (resourceId.isEmpty()) {
            throw new IllegalArgumentException("Selected resource ID cannot be empty.");
        }
        if (!url.isAbsolute()) {
            throw new IllegalArgumentException("Selected artifact URL must be absolute.");
        }
    }
}
