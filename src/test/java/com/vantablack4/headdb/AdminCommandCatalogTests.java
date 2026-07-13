package com.vantablack4.headdb;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

final class AdminCommandCatalogTests {
    @Test
    void publishesCanonicalAuditedHeadDbCommandsAndExpiringAliases() throws Exception {
        JsonObject catalog;
        try (var stream = getClass().getResourceAsStream("/vantablack/admin-commands.json")) {
            assertThat(stream).isNotNull();
            catalog = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }

        assertThat(catalog.get("schemaVersion").getAsInt()).isEqualTo(1);
        assertThat(catalog.get("owner").getAsString()).isEqualTo("mod-headdb");
        JsonArray commands = catalog.getAsJsonArray("commands");
        assertThat(commands).hasSize(5);
        assertThat(commands.asList().stream()
            .map(element -> element.getAsJsonObject().get("literal").getAsString()))
            .containsExactlyInAnyOrderElementsOf(Set.of(
                "agivehead",
                "agiveplayerhead",
                "arefreshheaddb",
                "averifyheaddb",
                "aheaddbstatus"
            ))
            .allMatch(literal -> literal.matches("^a[a-z][a-z0-9]*$"));
        assertThat(commands.asList()).allSatisfy(element -> {
            JsonObject command = element.getAsJsonObject();
            assertThat(command.get("permission").getAsString()).startsWith("vantablack.command.a");
            assertThat(command.getAsJsonObject("audit").get("recipientPermission").getAsString())
                .isEqualTo("vantablack.audit.admin.receive");
            assertThat(command.getAsJsonArray("legacyAliases")).hasSize(2);
            assertThat(command.getAsJsonArray("legacyAliases").asList()).allSatisfy(aliasElement ->
                assertThat(aliasElement.getAsJsonObject().get("removeAfter").getAsString()).isEqualTo("0.3.0"));
            assertThat(command.getAsJsonArray("legacyAliases").asList().stream()
                .map(alias -> alias.getAsJsonObject().get("literal").getAsString().split(" ")[0]))
                .containsExactlyInAnyOrder("hdb", "headdb");
        });
    }
}
