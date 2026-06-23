package io.github.silentdevelopment.headdb.core.database.revocation;

import io.github.silentdevelopment.headdb.model.HeadId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class RevocationSet {

    private final List<HeadRevocation> revocations;
    private final Map<HeadId, HeadRevocation> revocationIndex;

    public RevocationSet(@NotNull List<HeadRevocation> revocations) {
        this.revocations = List.copyOf(Objects.requireNonNull(revocations, "revocations"));
        this.revocationIndex = index(this.revocations);
    }

    public static @NotNull RevocationSet empty() {
        return new RevocationSet(List.of());
    }

    public @NotNull List<HeadRevocation> revocations() {
        return revocations;
    }

    public @NotNull Map<HeadId, HeadRevocation> revocationIndex() {
        return revocationIndex;
    }

    public boolean contains(@NotNull HeadId id) {
        Objects.requireNonNull(id, "id");
        return revocationIndex.containsKey(id);
    }

    public int size() {
        return revocations.size();
    }

    private static @NotNull Map<HeadId, HeadRevocation> index(@NotNull List<HeadRevocation> revocations) {
        Map<HeadId, HeadRevocation> index = new LinkedHashMap<>();

        for (HeadRevocation revocation : revocations) {
            HeadRevocation previous = index.put(revocation.id(), revocation);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate revocation ID: " + revocation.id());
            }
        }

        return Map.copyOf(index);
    }
}