package io.github.silentdevelopment.headdb.core.remote.parse;

import io.github.silentdevelopment.headdb.core.remote.RemoteManifest;
import org.jetbrains.annotations.NotNull;

public interface RemoteManifestParser {

    @NotNull RemoteManifest parse(@NotNull String json);
}