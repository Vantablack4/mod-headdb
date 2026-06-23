package io.github.silentdevelopment.headdb.paper.local.override;

import io.github.silentdevelopment.headdb.model.Head;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class HeadOverrideMerger {

    public @NotNull Head merge(@NotNull Head base, @NotNull RemoteHeadOverride override) {
        Objects.requireNonNull(base, "base");
        Objects.requireNonNull(override, "override");

        if (!base.id().equals(override.headId())) {
            throw new IllegalArgumentException("Override ID does not match base head ID.");
        }

        return new Head(
                base.id(),
                override.name() == null ? base.name() : override.name(),
                base.texture(),
                override.category() == null ? base.category() : override.category(),
                tags(base, override),
                collections(base, override)
        );
    }

    private @NotNull Set<String> tags(@NotNull Head base, @NotNull RemoteHeadOverride override) {
        if (override.replaceTags() != null) {
            return override.replaceTags();
        }

        Set<String> result = new LinkedHashSet<>(base.tags());
        result.addAll(override.addTags());
        result.removeAll(override.removeTags());
        return Set.copyOf(result);
    }

    private @NotNull Set<String> collections(@NotNull Head base, @NotNull RemoteHeadOverride override) {
        Set<String> result = new LinkedHashSet<>(base.collections());
        result.addAll(override.addCollections());
        result.removeAll(override.removeCollections());
        return Set.copyOf(result);
    }
}
