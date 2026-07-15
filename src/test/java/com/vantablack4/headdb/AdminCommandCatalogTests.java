package com.vantablack4.headdb;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Set;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
            assertThat(command.get("reason").getAsString()).isEqualTo("optional");
            assertThat(command.getAsJsonObject("audit").has("recipientPermission")).isFalse();
            assertThat(command.getAsJsonArray("legacyAliases")).hasSize(2);
            assertThat(command.getAsJsonArray("legacyAliases").asList()).allSatisfy(aliasElement ->
                assertThat(aliasElement.getAsJsonObject().get("removeAfter").getAsString()).isEqualTo("0.3.0"));
            assertThat(command.getAsJsonArray("legacyAliases").asList().stream()
                .map(alias -> alias.getAsJsonObject().get("literal").getAsString().split(" ")[0]))
                .containsExactlyInAnyOrder("hdb", "headdb");
        });
    }

    @Test
    void registersCanonicalAdminCommandsWithOptionalTrailingReasons(@TempDir Path tempDirectory) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        HeadDbConfig config = new HeadDbConfig(
            URI.create("https://example.test/manifest.json"),
            "primary",
            tempDirectory,
            false,
            false,
            10
        );
        try (HeadDbDatabaseService databaseService = HeadDbDatabaseService.create(config, VantablackHeadDbMod.LOGGER)) {
            CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
            new HeadDbCommands(config, databaseService, new FabricHeadItemFactory()).register(dispatcher);

            assertOptionalReason(dispatcher.getRoot().getChild("arefreshheaddb"));
            assertOptionalReason(dispatcher.getRoot().getChild("averifyheaddb"));
            assertOptionalReason(dispatcher.getRoot().getChild("aheaddbstatus"));
            assertOptionalReason(dispatcher.getRoot()
                .getChild("agivehead")
                .getChild("id")
                .getChild("target")
                .getChild("amount"));
            assertOptionalReason(dispatcher.getRoot()
                .getChild("agiveplayerhead")
                .getChild("player")
                .getChild("target")
                .getChild("amount"));
        }
    }

    private static void assertOptionalReason(CommandNode<CommandSourceStack> commandNode) {
        assertThat(commandNode.getCommand()).isNotNull();
        assertThat(commandNode.getChild("reason").getCommand()).isNotNull();
    }
}
