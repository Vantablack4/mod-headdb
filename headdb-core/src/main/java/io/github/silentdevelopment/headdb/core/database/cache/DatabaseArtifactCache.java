package io.github.silentdevelopment.headdb.core.database.cache;

import java.io.IOException;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public interface DatabaseArtifactCache {

    @NotNull Optional<CachedDatabaseArtifacts> load() throws IOException;

    void save(@NotNull CachedDatabaseArtifacts artifacts) throws IOException;
}