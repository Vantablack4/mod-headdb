package io.github.silentdevelopment.headdb.core.remote.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class JdkRemoteHttpClient implements RemoteHttpClient {

    private static final int BUFFER_SIZE = 8192;
    private static final int DEFAULT_MAX_RESPONSE_BYTES = 64 * 1024 * 1024;

    private final HttpClient client;
    private final Duration timeout;
    private final int maxResponseBytes;

    public JdkRemoteHttpClient() {
        this(HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(10)).build(), Duration.ofSeconds(30), DEFAULT_MAX_RESPONSE_BYTES);
    }

    public JdkRemoteHttpClient(@NotNull HttpClient client, @NotNull Duration timeout, int maxResponseBytes) {
        this.client = Objects.requireNonNull(client, "client");
        this.timeout = Objects.requireNonNull(timeout, "timeout");

        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("HTTP timeout must be positive.");
        }

        if (maxResponseBytes <= 0) {
            throw new IllegalArgumentException("Maximum response bytes must be positive.");
        }

        this.maxResponseBytes = maxResponseBytes;
    }

    @Override
    public @NotNull String getText(@NotNull URI uri) throws IOException, InterruptedException {
        return new String(getBytes(uri), StandardCharsets.UTF_8);
    }

    @Override
    public byte @NotNull [] getBytes(@NotNull URI uri) throws IOException, InterruptedException {
        Objects.requireNonNull(uri, "uri");

        if (!uri.isAbsolute()) {
            throw new IllegalArgumentException("HTTP URI must be absolute.");
        }

        HttpRequest request = HttpRequest.newBuilder(uri).GET().timeout(timeout).header("Accept", "*/*").build();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new IOException("HTTP request failed with status " + status + " for " + uri);
        }

        return readLimited(response.body(), uri);
    }

    private byte @NotNull [] readLimited(@NotNull InputStream input, @NotNull URI uri) throws IOException {
        Objects.requireNonNull(input, "input");

        try (InputStream stream = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[BUFFER_SIZE];

            while (true) {
                int read = stream.read(buffer);
                if (read == -1) {
                    break;
                }

                if (output.size() + read > maxResponseBytes) {
                    throw new IOException("HTTP response exceeds maximum size of " + maxResponseBytes + " bytes for " + uri);
                }

                output.write(buffer, 0, read);
            }

            return output.toByteArray();
        }
    }

}