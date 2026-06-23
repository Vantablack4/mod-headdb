package io.github.silentdevelopment.headdb.core.remote.http;

import java.io.IOException;
import java.net.URI;
import org.jetbrains.annotations.NotNull;

public interface RemoteHttpClient {

    @NotNull String getText(@NotNull URI uri) throws IOException, InterruptedException;

    byte @NotNull [] getBytes(@NotNull URI uri) throws IOException, InterruptedException;
}