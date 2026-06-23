package io.github.silentdevelopment.headdb.core.remote;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class ArtifactSelector {

    public static final String DEFAULT_PREFERRED_MIRROR_ID = "primary";

    private final String preferredMirrorId;

    public ArtifactSelector() {
        this(DEFAULT_PREFERRED_MIRROR_ID);
    }

    public ArtifactSelector(@NotNull String preferredMirrorId) {
        Objects.requireNonNull(preferredMirrorId, "preferredMirrorId");
        preferredMirrorId = preferredMirrorId.trim();
        if (preferredMirrorId.isEmpty()) {
            throw new IllegalArgumentException("Preferred mirror ID cannot be empty.");
        }
        this.preferredMirrorId = preferredMirrorId;
    }

    public @NotNull String preferredMirrorId() {
        return preferredMirrorId;
    }

    public @NotNull ArtifactSelection selectCatalogIndex(@NotNull RemoteManifest manifest) {
        return selectResourceIndex(manifest, RemoteResourceId.CATALOG);
    }

    public @NotNull ArtifactSelection selectRevocationsIndex(@NotNull RemoteManifest manifest) {
        return selectResourceIndex(manifest, RemoteResourceId.REVOCATIONS);
    }

    public @NotNull ArtifactSelection selectPart(@NotNull RemoteManifest manifest, @NotNull String resourceId, @NotNull RemoteArtifact part) {
        Objects.requireNonNull(part, "part");
        RemoteMirror mirror = selectMirror(manifest);
        return new ArtifactSelection(mirror, resourceId, part, mirror.resolve(part.path()));
    }

    public @NotNull ArtifactSelection selectResourceIndex(@NotNull RemoteManifest manifest, @NotNull String resourceId) {
        Objects.requireNonNull(manifest, "manifest");
        Objects.requireNonNull(resourceId, "resourceId");
        RemoteMirror mirror = selectMirror(manifest);
        RemoteResource resource = manifest.resource(resourceId).orElseThrow(() -> new IllegalArgumentException("Remote manifest does not contain resource: " + resourceId));
        return new ArtifactSelection(mirror, resourceId, resource.index(), mirror.resolve(resource.index().path()));
    }

    private @NotNull RemoteMirror selectMirror(@NotNull RemoteManifest manifest) {
        Objects.requireNonNull(manifest, "manifest");
        if (manifest.mirrors().isEmpty()) {
            throw new IllegalArgumentException("Remote manifest does not contain any mirrors.");
        }
        return manifest.mirror(preferredMirrorId).orElseGet(() -> manifest.mirrors().getFirst());
    }
}
