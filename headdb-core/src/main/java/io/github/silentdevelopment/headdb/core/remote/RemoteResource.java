package io.github.silentdevelopment.headdb.core.remote;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public record RemoteResource(@NotNull RemoteArtifact index) {

    public RemoteResource {
        Objects.requireNonNull(index, "index");
    }
}
