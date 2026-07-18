package com.vantablack4.headdb;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.function.Predicate;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.vantablack4.characters.ActiveCharacterSession;
import com.vantablack4.characters.VantablackCharacterServices;
import io.github.silentdevelopment.headdb.database.DatabaseStatus;
import io.github.silentdevelopment.headdb.database.DatabaseStats;
import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.query.HeadQuery;
import io.github.silentdevelopment.headdb.query.HeadQueryResult;
import io.github.silentdevelopment.headdb.query.HeadSort;
import io.github.silentdevelopment.headdb.query.SortDirection;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.permission.v1.PermissionNode;
import net.fabricmc.fabric.api.permission.v1.PermissionPredicates;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class HeadDbCommands {
    static final PermissionNode<Boolean> GIVE_REMOTE_PERMISSION = permission("command.agivehead");
    static final PermissionNode<Boolean> GIVE_PLAYER_PERMISSION = permission("command.agiveplayerhead");
    static final PermissionNode<Boolean> REFRESH_PERMISSION = permission("command.arefreshheaddb");
    static final PermissionNode<Boolean> VERIFY_PERMISSION = permission("command.averifyheaddb");
    static final PermissionNode<Boolean> STATUS_PERMISSION = permission("command.aheaddbstatus");
    private static final String GIVE_REMOTE_LITERAL = "agivehead";
    private static final String GIVE_PLAYER_LITERAL = "agiveplayerhead";
    private static final String REFRESH_LITERAL = "arefreshheaddb";
    private static final String VERIFY_LITERAL = "averifyheaddb";
    private static final String STATUS_LITERAL = "aheaddbstatus";
    private static final String ID_ARGUMENT = "id";
    private static final String QUERY_ARGUMENT = "query";
    private static final String PLAYER_ARGUMENT = "player";
    private static final String TARGET_ARGUMENT = "target";
    private static final String AMOUNT_ARGUMENT = "amount";
    private static final String REASON_ARGUMENT = "reason";
    private static final int MAX_GIVE_AMOUNT = 64;

    private final HeadDbConfig config;
    private final HeadDbDatabaseService databaseService;
    private final FabricHeadItemFactory itemFactory;
    private final HeadDbGuiService guiService;

    public HeadDbCommands(HeadDbConfig config, HeadDbDatabaseService databaseService, FabricHeadItemFactory itemFactory) {
        this.config = config;
        this.databaseService = databaseService;
        this.itemFactory = itemFactory;
        this.guiService = new HeadDbGuiService(databaseService, itemFactory, this::canReceiveHead, this::giveGuiHead);
    }

    public void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(root("hdb"));
        dispatcher.register(root("headdb"));
        dispatcher.register(canonicalRemoteGive());
        dispatcher.register(canonicalPlayerGive());
        dispatcher.register(Commands.literal(REFRESH_LITERAL)
            .requires(require(REFRESH_PERMISSION))
            .executes(context -> refresh(context, false))
            .then(Commands.argument(REASON_ARGUMENT, StringArgumentType.greedyString())
                .executes(context -> refresh(context, false))));
        dispatcher.register(Commands.literal(VERIFY_LITERAL)
            .requires(require(VERIFY_PERMISSION))
            .executes(context -> verify(context, false))
            .then(Commands.argument(REASON_ARGUMENT, StringArgumentType.greedyString())
                .executes(context -> verify(context, false))));
        dispatcher.register(Commands.literal(STATUS_LITERAL)
            .requires(require(STATUS_PERMISSION))
            .executes(context -> status(context, false))
            .then(Commands.argument(REASON_ARGUMENT, StringArgumentType.greedyString())
                .executes(context -> status(context, false))));
    }

    private LiteralArgumentBuilder<CommandSourceStack> canonicalRemoteGive() {
        return Commands.literal(GIVE_REMOTE_LITERAL)
            .requires(require(GIVE_REMOTE_PERMISSION))
            .then(Commands.argument(ID_ARGUMENT, StringArgumentType.word())
                .suggests(this::suggestHeadIds)
                .then(Commands.argument(TARGET_ARGUMENT, StringArgumentType.string())
                    .suggests(this::suggestTargetsIncludingSelf)
                    .then(Commands.argument(AMOUNT_ARGUMENT, IntegerArgumentType.integer(1, MAX_GIVE_AMOUNT))
                        .executes(context -> giveRemoteHeadToTarget(
                            context,
                            getInteger(context, AMOUNT_ARGUMENT),
                            false
                        ))
                        .then(Commands.argument(REASON_ARGUMENT, StringArgumentType.greedyString())
                            .executes(context -> giveRemoteHeadToTarget(
                                context,
                                getInteger(context, AMOUNT_ARGUMENT),
                                false
                            ))))));
    }

    private LiteralArgumentBuilder<CommandSourceStack> canonicalPlayerGive() {
        return Commands.literal(GIVE_PLAYER_LITERAL)
            .requires(require(GIVE_PLAYER_PERMISSION))
            .then(Commands.argument(PLAYER_ARGUMENT, StringArgumentType.word())
                .then(Commands.argument(TARGET_ARGUMENT, StringArgumentType.string())
                    .suggests(this::suggestTargetsIncludingSelf)
                    .then(Commands.argument(AMOUNT_ARGUMENT, IntegerArgumentType.integer(1, MAX_GIVE_AMOUNT))
                        .executes(context -> givePlayerHeadToTarget(
                            context,
                            getInteger(context, AMOUNT_ARGUMENT),
                            false
                        ))
                        .then(Commands.argument(REASON_ARGUMENT, StringArgumentType.greedyString())
                            .executes(context -> givePlayerHeadToTarget(
                                context,
                                getInteger(context, AMOUNT_ARGUMENT),
                                false
                            ))))));
    }

    private LiteralArgumentBuilder<CommandSourceStack> root(String name) {
        return Commands.literal(name)
            .executes(this::openDefaultGui)
            .then(Commands.literal("help")
                .executes(this::sendHelp))
            .then(Commands.literal("status")
                .requires(require(STATUS_PERMISSION))
                .executes(context -> status(context, true)))
            .then(Commands.literal("search")
                .then(Commands.argument(QUERY_ARGUMENT, StringArgumentType.greedyString())
                    .executes(this::openSearchGui)))
            .then(Commands.literal("gui")
                .executes(this::openDefaultGui)
                .then(Commands.argument(QUERY_ARGUMENT, StringArgumentType.greedyString())
                    .executes(this::openSearchGui)))
            .then(Commands.literal("info")
                .then(Commands.argument(ID_ARGUMENT, StringArgumentType.word())
                    .suggests(this::suggestHeadIds)
                    .executes(this::sendInfo)))
            .then(Commands.literal("give")
                .requires(require(GIVE_REMOTE_PERMISSION))
                .then(Commands.argument(ID_ARGUMENT, StringArgumentType.word())
                    .suggests(this::suggestHeadIds)
                    .executes(context -> giveRemoteHead(context, context.getSource().getPlayerOrException(), 1, true))
                    .then(Commands.argument(AMOUNT_ARGUMENT, IntegerArgumentType.integer(1, MAX_GIVE_AMOUNT))
                        .executes(context -> giveRemoteHead(context, context.getSource().getPlayerOrException(), getInteger(context, AMOUNT_ARGUMENT), true)))
                    .then(Commands.argument(TARGET_ARGUMENT, StringArgumentType.string())
                        .suggests(this::suggestTargets)
                        .executes(context -> giveRemoteHeadToTarget(context, 1, true))
                        .then(Commands.argument(AMOUNT_ARGUMENT, IntegerArgumentType.integer(1, MAX_GIVE_AMOUNT))
                            .executes(context -> giveRemoteHeadToTarget(context, getInteger(context, AMOUNT_ARGUMENT), true))))))
            .then(Commands.literal("player")
                .requires(require(GIVE_PLAYER_PERMISSION))
                .then(Commands.argument(PLAYER_ARGUMENT, StringArgumentType.word())
                    .executes(context -> givePlayerHead(context, context.getSource().getPlayerOrException(), 1, true))
                    .then(Commands.argument(AMOUNT_ARGUMENT, IntegerArgumentType.integer(1, MAX_GIVE_AMOUNT))
                        .executes(context -> givePlayerHead(context, context.getSource().getPlayerOrException(), getInteger(context, AMOUNT_ARGUMENT), true)))
                    .then(Commands.argument(TARGET_ARGUMENT, StringArgumentType.string())
                        .suggests(this::suggestTargets)
                        .executes(context -> givePlayerHeadToTarget(context, 1, true))
                        .then(Commands.argument(AMOUNT_ARGUMENT, IntegerArgumentType.integer(1, MAX_GIVE_AMOUNT))
                            .executes(context -> givePlayerHeadToTarget(context, getInteger(context, AMOUNT_ARGUMENT), true))))))
            .then(Commands.literal("refresh")
                .requires(require(REFRESH_PERMISSION))
                .executes(context -> refresh(context, true)))
            .then(Commands.literal("verify")
                .requires(require(VERIFY_PERMISSION))
                .executes(context -> verify(context, true)));
    }

    private int openDefaultGui(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            return sendHelp(context);
        }
        return guiService.openDefault(player);
    }

    private int openSearchGui(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            return sendSearch(context);
        }
        return guiService.openSearch(player, getString(context, QUERY_ARGUMENT));
    }

    private int sendHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSystemMessage(header("HeadDB"));
        source.sendSystemMessage(line("Open", "/hdb"));
        source.sendSystemMessage(line("Search", "/hdb search <query>"));
        source.sendSystemMessage(line("Info", "/hdb info <id>"));
        source.sendSystemMessage(line("Give", "/agivehead <id> <target|self> <amount> [reason]"));
        source.sendSystemMessage(line("Player", "/agiveplayerhead <name|uuid> <target|self> <amount> [reason]"));
        source.sendSystemMessage(line("Admin", "/aheaddbstatus [reason] | /arefreshheaddb [reason] | /averifyheaddb [reason]"));
        return 1;
    }

    private int sendStatus(CommandContext<CommandSourceStack> context) {
        DatabaseStatus status = databaseService.status();
        DatabaseStats stats = status.stats();
        context.getSource().sendSystemMessage(header("HeadDB status"));
        context.getSource().sendSystemMessage(line("State", status.state().name()));
        context.getSource().sendSystemMessage(line("Source", status.source().name()));
        context.getSource().sendSystemMessage(line("Heads", Integer.toString(stats.heads())));
        context.getSource().sendSystemMessage(line("Categories", Integer.toString(stats.categories())));
        if (status.loadedAt() != null) {
            context.getSource().sendSystemMessage(line("Loaded", DateTimeFormatter.ISO_INSTANT.format(status.loadedAt())));
        }
        if (status.manifestId() != null) {
            context.getSource().sendSystemMessage(line("Manifest", status.manifestId()));
        }
        if (status.lastError() != null) {
            context.getSource().sendSystemMessage(error(status.lastError()));
        }
        return 1;
    }

    private int sendSearch(CommandContext<CommandSourceStack> context) {
        if (!databaseAvailable(context.getSource())) {
            return 0;
        }

        String queryText = getString(context, QUERY_ARGUMENT);
        HeadQuery query = HeadQuery.builder()
            .text(queryText)
            .sort(HeadSort.RELEVANCE)
            .direction(SortDirection.DESCENDING)
            .limit(config.searchResultLimit())
            .build();
        HeadQueryResult result = databaseService.database().search(query);

        context.getSource().sendSystemMessage(header("Search: " + queryText));
        context.getSource().sendSystemMessage(line("Matches", result.total() + " total, showing " + result.heads().size()));
        for (Head head : result.heads()) {
            context.getSource().sendSystemMessage(headLine(head));
        }
        if (result.heads().isEmpty()) {
            context.getSource().sendSystemMessage(error("No heads matched that query."));
        }
        return result.heads().isEmpty() ? 0 : 1;
    }

    private int sendInfo(CommandContext<CommandSourceStack> context) {
        if (!databaseAvailable(context.getSource())) {
            return 0;
        }

        Optional<Head> head = findHead(context);
        if (head.isEmpty()) {
            context.getSource().sendSystemMessage(error("Unknown remote head ID."));
            return 0;
        }

        Head value = head.get();
        context.getSource().sendSystemMessage(header(value.name()));
        context.getSource().sendSystemMessage(line("ID", value.id().display()));
        context.getSource().sendSystemMessage(line("Category", value.category()));
        context.getSource().sendSystemMessage(line("Texture", value.texture().hash()));
        if (!value.tags().isEmpty()) {
            context.getSource().sendSystemMessage(line("Tags", String.join(", ", value.tags())));
        }
        if (!value.collections().isEmpty()) {
            context.getSource().sendSystemMessage(line("Collections", String.join(", ", value.collections())));
        }
        return 1;
    }

    private int giveRemoteHead(
        CommandContext<CommandSourceStack> context,
        ServerPlayer target,
        int amount,
        boolean legacy
    ) {
        if (!databaseAvailable(context.getSource())) {
            return 0;
        }

        Optional<Head> head = findHead(context);
        if (head.isEmpty()) {
            context.getSource().sendSystemMessage(error("Unknown remote head ID."));
            return 0;
        }

        warnLegacy(context.getSource(), legacy, GIVE_REMOTE_LITERAL);
        PinnedTarget pinnedTarget = pinTarget(context.getSource(), target);
        if (pinnedTarget == null) return 0;
        Head value = head.get();
        ServerPlayer current = resolvePinned(context.getSource().getServer(), pinnedTarget);
        if (current == null) {
            context.getSource().sendFailure(Component.literal(
                "Target character session changed before the head could be delivered."
            ));
            return 0;
        }
        giveStack(current, itemFactory.remoteHead(value, amount));
        context.getSource().sendSystemMessage(success(
            "Gave " + amount + "x " + value.name() + " to " + displayName(current) + "."
        ));
        ServerPlayer actor = context.getSource().getPlayer();
        if (actor == null || !current.getUUID().equals(actor.getUUID())) {
            current.sendSystemMessage(success("Received " + value.name() + "."));
        }
        return 1;
    }

    private int giveRemoteHeadToTarget(CommandContext<CommandSourceStack> context, int amount, boolean legacy) {
        Optional<ServerPlayer> target = findTarget(context);
        if (target.isEmpty()) {
            return 0;
        }
        return giveRemoteHead(context, target.get(), amount, legacy);
    }

    private int givePlayerHead(
        CommandContext<CommandSourceStack> context,
        ServerPlayer target,
        int amount,
        boolean legacy
    ) {
        String player = getString(context, PLAYER_ARGUMENT);
        warnLegacy(context.getSource(), legacy, GIVE_PLAYER_LITERAL);
        PinnedTarget pinnedTarget = pinTarget(context.getSource(), target);
        if (pinnedTarget == null) return 0;
        ServerPlayer current = resolvePinned(context.getSource().getServer(), pinnedTarget);
        if (current == null) {
            context.getSource().sendFailure(Component.literal(
                "Target character session changed before the head could be delivered."
            ));
            return 0;
        }
        giveStack(current, itemFactory.playerHead(player, amount));
        context.getSource().sendSystemMessage(success(
            "Gave " + amount + "x player head to " + displayName(current) + "."
        ));
        return 1;
    }

    private int givePlayerHeadToTarget(CommandContext<CommandSourceStack> context, int amount, boolean legacy) {
        Optional<ServerPlayer> target = findTarget(context);
        if (target.isEmpty()) {
            return 0;
        }
        return givePlayerHead(context, target.get(), amount, legacy);
    }

    private static String displayName(ServerPlayer player) {
        return player.getDisplayName().getString();
    }

    private void giveStack(ServerPlayer target, ItemStack stack) {
        ItemStack remaining = stack.copy();
        boolean inserted = target.getInventory().add(remaining);
        if (!inserted && !remaining.isEmpty()) {
            target.drop(remaining, false);
        }
        target.inventoryMenu.broadcastChanges();
    }

    private Optional<Head> findHead(CommandContext<CommandSourceStack> context) {
        try {
            HeadId id = HeadIdParser.remote(getString(context, ID_ARGUMENT));
            return databaseService.database().findById(id);
        } catch (IllegalArgumentException exception) {
            context.getSource().sendSystemMessage(error(exception.getMessage()));
            return Optional.empty();
        }
    }

    private Optional<ServerPlayer> findTarget(CommandContext<CommandSourceStack> context) {
        String targetText = getString(context, TARGET_ARGUMENT).trim();
        if (targetText.isEmpty()) {
            context.getSource().sendSystemMessage(error("Target character name cannot be empty."));
            return Optional.empty();
        }

        if (targetText.equalsIgnoreCase("self")) {
            ServerPlayer self = context.getSource().getPlayer();
            if (self == null) {
                context.getSource().sendSystemMessage(error("Console must name an online target character."));
                return Optional.empty();
            }
            return Optional.of(self);
        }
        List<ServerPlayer> matches = context.getSource().getServer().getPlayerList().getPlayers().stream()
            .filter(player -> matchesTarget(player, targetText))
            .toList();
        if (matches.isEmpty()) {
            context.getSource().sendSystemMessage(error("No online character named \"" + targetText + "\"."));
            return Optional.empty();
        }
        if (matches.size() > 1) {
            context.getSource().sendSystemMessage(error("Multiple online characters match \"" + targetText + "\"."));
            return Optional.empty();
        }
        return Optional.of(matches.get(0));
    }

    private java.util.concurrent.CompletableFuture<Suggestions> suggestTargets(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        List<String> names = new ArrayList<>();
        for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
            characterName(player)
                .map(StringArgumentType::escapeIfRequired)
                .ifPresent(name -> addUnique(names, name));
        }
        return SharedSuggestionProvider.suggest(names, builder);
    }

    private java.util.concurrent.CompletableFuture<Suggestions> suggestTargetsIncludingSelf(
        CommandContext<CommandSourceStack> context,
        SuggestionsBuilder builder
    ) {
        List<String> names = new ArrayList<>();
        if (context.getSource().getPlayer() != null) {
            names.add("self");
        }
        for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
            characterName(player)
                .map(StringArgumentType::escapeIfRequired)
                .ifPresent(name -> addUnique(names, name));
        }
        return SharedSuggestionProvider.suggest(names, builder);
    }

    private static boolean matchesTarget(ServerPlayer player, String targetText) {
        return characterName(player)
            .map(name -> name.equalsIgnoreCase(targetText))
            .orElse(false);
    }

    private static Optional<String> characterName(ServerPlayer player) {
        if (player == null) {
            return Optional.empty();
        }
        String name = displayName(player).trim();
        if (name.isBlank() || name.equalsIgnoreCase(player.getGameProfile().name())) {
            return Optional.empty();
        }
        return Optional.of(name);
    }

    private static void addUnique(List<String> values, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!values.contains(value)) {
            values.add(value);
        }
    }

    private int refresh(CommandContext<CommandSourceStack> context, boolean legacy) {
        warnLegacy(context.getSource(), legacy, REFRESH_LITERAL);
        CommandSourceStack source = context.getSource();
        source.sendSystemMessage(line("HeadDB", "Remote refresh started."));
        databaseService.refreshAsync().whenComplete((snapshot, throwable) -> source.getServer().execute(() -> {
            if (throwable != null) {
                source.sendSystemMessage(error(rootCause(throwable)));
                return;
            }
            source.sendSystemMessage(success("HeadDB refreshed with " + snapshot.stats().heads() + " heads."));
        }));
        return 1;
    }

    private int verify(CommandContext<CommandSourceStack> context, boolean legacy) {
        warnLegacy(context.getSource(), legacy, VERIFY_LITERAL);
        CommandSourceStack source = context.getSource();
        source.sendSystemMessage(line("HeadDB", "Remote verification started."));
        databaseService.verifyAsync().whenComplete((snapshot, throwable) -> source.getServer().execute(() -> {
            if (throwable != null) {
                source.sendSystemMessage(error(rootCause(throwable)));
                return;
            }
            source.sendSystemMessage(success(
                "HeadDB remote verification passed with " + snapshot.stats().heads() + " heads."
            ));
        }));
        return 1;
    }

    private int status(CommandContext<CommandSourceStack> context, boolean legacy) {
        warnLegacy(context.getSource(), legacy, STATUS_LITERAL);
        return sendStatus(context);
    }

    private boolean canReceiveHead(ServerPlayer player) {
        return PermissionPredicates.<ServerPlayer>require(GIVE_REMOTE_PERMISSION, false).test(player);
    }

    private void giveGuiHead(ServerPlayer player, Head head) {
        CommandSourceStack source = player.createCommandSourceStack();
        if (!canReceiveHead(player)) {
            player.sendSystemMessage(error("Head grant permission required."));
            return;
        }
        PinnedTarget pinnedTarget = pinTarget(source, player);
        if (pinnedTarget == null) return;
        ServerPlayer current = resolvePinned(player.level().getServer(), pinnedTarget);
        if (current == null) {
            source.sendFailure(Component.literal("Your character session changed before the head could be delivered."));
            return;
        }
        giveStack(current, itemFactory.remoteHead(head, 1));
        current.sendSystemMessage(success("Received " + head.name() + "."));
    }

    private static PinnedTarget pinTarget(CommandSourceStack source, ServerPlayer target) {
        ActiveCharacterSession session = VantablackCharacterServices.activeSession(target.getUUID()).orElse(null);
        if (session == null) {
            source.sendFailure(Component.literal("Target has no active Vantablack character session."));
            return null;
        }
        return PinnedTarget.capture(target, session);
    }

    private static ServerPlayer resolvePinned(MinecraftServer server, PinnedTarget expected) {
        if (server == null) return null;
        ServerPlayer current = server.getPlayerList().getPlayer(expected.playerUuid());
        ActiveCharacterSession session = current == null
            ? null
            : VantablackCharacterServices.activeSession(expected.playerUuid()).orElse(null);
        return current != null && expected.matches(session) ? current : null;
    }

    record PinnedTarget(
        UUID playerUuid,
        UUID accountId,
        UUID characterId,
        int sessionId,
        Instant selectedAt,
        String targetName
    ) {
        static PinnedTarget capture(ServerPlayer target, ActiveCharacterSession session) {
            return new PinnedTarget(
                target.getUUID(), session.accountId(), session.characterId(), session.sessionId(),
                session.selectedAt(), displayName(target)
            );
        }

        boolean matches(ActiveCharacterSession session) {
            return session != null && matches(
                session.playerUuid(), session.accountId(), session.characterId(), session.sessionId(), session.selectedAt()
            );
        }

        boolean matches(UUID player, UUID account, UUID character, int currentSessionId, Instant currentSelectedAt) {
            return playerUuid.equals(player)
                && accountId.equals(account)
                && characterId.equals(character)
                && sessionId == currentSessionId
                && selectedAt.equals(currentSelectedAt);
        }
    }

    private static void warnLegacy(CommandSourceStack source, boolean legacy, String replacement) {
        if (legacy) {
            source.sendSystemMessage(Component.literal(
                "Deprecated admin alias; use /" + replacement + ". This alias is removed in 0.3.0."
            ).withStyle(ChatFormatting.YELLOW));
        }
    }

    private static PermissionNode<Boolean> permission(String path) {
        return PermissionNode.of("vantablack", path);
    }

    private static Predicate<CommandSourceStack> require(PermissionNode<Boolean> permission) {
        return PermissionPredicates.require(permission, false);
    }

    private boolean databaseAvailable(CommandSourceStack source) {
        if (databaseService.status().available()) {
            return true;
        }
        source.sendSystemMessage(error("HeadDB database is not loaded yet. Run /aheaddbstatus or /arefreshheaddb."));
        return false;
    }

    private java.util.concurrent.CompletableFuture<Suggestions> suggestHeadIds(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        if (!databaseService.status().available()) {
            return Suggestions.empty();
        }
        String text = builder.getRemaining().trim();
        HeadQuery query = HeadQuery.builder()
            .text(text)
            .sort(text.isBlank() ? HeadSort.ID : HeadSort.RELEVANCE)
            .direction(text.isBlank() ? SortDirection.ASCENDING : SortDirection.DESCENDING)
            .limit(25)
            .build();
        List<String> ids = databaseService.database().search(query).heads().stream()
            .map(head -> head.id().display())
            .toList();
        return SharedSuggestionProvider.suggest(ids, builder);
    }

    private static Component headLine(Head head) {
        return Component.literal(head.id().display())
            .withStyle(ChatFormatting.YELLOW)
            .append(Component.literal(" - ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(head.name()).withStyle(ChatFormatting.WHITE))
            .append(Component.literal(" [" + head.category() + "]").withStyle(ChatFormatting.GRAY));
    }

    private static Component header(String text) {
        return Component.literal("VANTABLACK HEADDB: ")
            .withStyle(ChatFormatting.GOLD)
            .append(Component.literal(text).withStyle(ChatFormatting.WHITE));
    }

    private static Component line(String label, String value) {
        return Component.literal(label + ": ")
            .withStyle(ChatFormatting.GRAY)
            .append(Component.literal(value).withStyle(ChatFormatting.WHITE));
    }

    private static Component success(String value) {
        return Component.literal(value).withStyle(ChatFormatting.GREEN);
    }

    private static Component error(String value) {
        return Component.literal("HATA: ")
            .withStyle(ChatFormatting.RED)
            .append(Component.literal(value).withStyle(ChatFormatting.WHITE));
    }

    private static String rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            return current.getClass().getSimpleName();
        }
        return message;
    }

}
