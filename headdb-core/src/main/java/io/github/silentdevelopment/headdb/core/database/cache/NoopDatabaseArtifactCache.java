package io.github.silentdevelopment.headdb.core.database.cache;

import java.io.IOException;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public final class NoopDatabaseArtifactCache implements DatabaseArtifactCache {

    @Override
    public @NotNull Optional<CachedDatabaseArtifacts> load() throws IOException {
        return Optional.empty();
    }

    @Override
    public void save(@NotNull CachedDatabaseArtifacts artifacts) throws IOException {
    }
}