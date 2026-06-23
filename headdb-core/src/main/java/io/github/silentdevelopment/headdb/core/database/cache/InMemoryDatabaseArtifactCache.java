package io.github.silentdevelopment.headdb.core.database.cache;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;

public final class InMemoryDatabaseArtifactCache implements DatabaseArtifactCache {

    private final AtomicReference<CachedDatabaseArtifacts> artifacts = new AtomicReference<>();

    @Override
    public @NotNull Optional<CachedDatabaseArtifacts> load() throws IOException {
        return Optional.ofNullable(artifacts.get());
    }

    @Override
    public void save(@NotNull CachedDatabaseArtifacts artifacts) throws IOException {
        this.artifacts.set(Objects.requireNonNull(artifacts, "artifacts"));
    }
}