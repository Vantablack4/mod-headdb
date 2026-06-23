package io.github.silentdevelopment.headdb.core.database;

import io.github.silentdevelopment.headdb.database.HeadDatabase;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class HeadDatabases {

    private HeadDatabases() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated.");
    }

    public static @NotNull HeadDatabase empty() {
        return new DefaultHeadDatabase();
    }

    public static @NotNull HeadDatabase fromSnapshot(@NotNull DatabaseSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        return new DefaultHeadDatabase(snapshot);
    }
}