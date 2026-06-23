package io.github.silentdevelopment.headdb.core.remote.http;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class JdkRemoteHttpClientTest {

    private HttpServer server;
    private URI baseUri;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/text", exchange -> respond(exchange, 200, "{\"id\":\"heads-database\"}".getBytes(StandardCharsets.UTF_8)));
        server.createContext("/bytes", exchange -> respond(exchange, 200, new byte[] {1, 2, 3, 4}));
        server.createContext("/not-found", exchange -> respond(exchange, 404, "not found".getBytes(StandardCharsets.UTF_8)));
        server.createContext("/large", exchange -> respond(exchange, 200, "larger-than-limit".getBytes(StandardCharsets.UTF_8)));
        server.start();

        baseUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/");
    }

    @AfterEach
    void stopServer() {
        if (server == null) {
            return;
        }

        server.stop(0);
    }

    @Test
    void getsTextResponse() throws Exception {
        JdkRemoteHttpClient client = client(1024);

        String text = client.getText(uri("text"));

        assertEquals("{\"id\":\"heads-database\"}", text);
    }

    @Test
    void getsByteResponse() throws Exception {
        JdkRemoteHttpClient client = client(1024);

        byte[] bytes = client.getBytes(uri("bytes"));

        assertArrayEquals(new byte[] {1, 2, 3, 4}, bytes);
    }

    @Test
    void rejectsNonSuccessStatus() {
        JdkRemoteHttpClient client = client(1024);

        assertThrows(IOException.class, () -> client.getBytes(uri("not-found")));
    }

    @Test
    void rejectsOversizedResponse() {
        JdkRemoteHttpClient client = client(5);

        assertThrows(IOException.class, () -> client.getBytes(uri("large")));
    }

    @Test
    void rejectsRelativeUri() {
        JdkRemoteHttpClient client = client(1024);

        assertThrows(IllegalArgumentException.class, () -> client.getBytes(URI.create("manifest.json")));
    }

    @SuppressWarnings({"DataFlowIssue", "resource"})
    @Test
    void rejectsInvalidConstructorArguments() {
        HttpClient httpClient = HttpClient.newHttpClient();

        assertThrows(NullPointerException.class, () -> new JdkRemoteHttpClient(null, Duration.ofSeconds(1), 1024));
        assertThrows(NullPointerException.class, () -> new JdkRemoteHttpClient(httpClient, null, 1024));
        assertThrows(IllegalArgumentException.class, () -> new JdkRemoteHttpClient(httpClient, Duration.ZERO, 1024));
        assertThrows(IllegalArgumentException.class, () -> new JdkRemoteHttpClient(httpClient, Duration.ofSeconds(-1), 1024));
        assertThrows(IllegalArgumentException.class, () -> new JdkRemoteHttpClient(httpClient, Duration.ofSeconds(1), 0));
        assertThrows(IllegalArgumentException.class, () -> new JdkRemoteHttpClient(httpClient, Duration.ofSeconds(1), -1));
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void rejectsNullUri() {
        JdkRemoteHttpClient client = client(1024);

        assertThrows(NullPointerException.class, () -> client.getText(null));
        assertThrows(NullPointerException.class, () -> client.getBytes(null));
    }

    private JdkRemoteHttpClient client(int maxResponseBytes) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

        return new JdkRemoteHttpClient(httpClient, Duration.ofSeconds(2), maxResponseBytes);
    }

    private URI uri(String path) {
        return baseUri.resolve(path);
    }

    private static void respond(HttpExchange exchange, int status, byte[] body) throws IOException {
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}