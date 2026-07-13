package com.vantablack4.headdb;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.vantablack4.permissions.api.AdminCommandDefinition;
import com.vantablack4.permissions.api.AdminCommandInvocation;
import com.vantablack4.permissions.api.AdminCommandResult;
import com.vantablack4.permissions.api.VantablackPermissions;
import io.github.silentdevelopment.headdb.database.DatabaseStatus;
import io.github.silentdevelopment.headdb.database.DatabaseStats;
import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.query.HeadQuery;
import io.github.silentdevelopment.headdb.query.HeadQueryResult;
import io.github.silentdevelopment.headdb.query.HeadSort;
import io.github.silentdevelopment.headdb.query.SortDirection;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class HeadDbCommands {
    private static final AdminCommandDefinition GIVE_REMOTE = definition(
        "headdb.remote.give", "agivehead", "vantablack.command.agivehead",
        AdminCommandDefinition.Risk.MEDIUM, AdminCommandDefinition.ReasonPolicy.REQUIRED,
        "character", "headdb.remote.give", "admcmd.headdb.remote.give"
    );
    private static final AdminCommandDefinition GIVE_PLAYER = definition(
        "headdb.player.give", "agiveplayerhead", "vantablack.command.agiveplayerhead",
        AdminCommandDefinition.Risk.MEDIUM, AdminCommandDefinition.ReasonPolicy.REQUIRED,
        "character", "headdb.player.give", "admcmd.headdb.player.give"
    );
    private static final AdminCommandDefinition REFRESH = definition(
        "headdb.refresh", "arefreshheaddb", "vantablack.command.arefreshheaddb",
        AdminCommandDefinition.Risk.MEDIUM, AdminCommandDefinition.ReasonPolicy.REQUIRED,
        "service", "headdb.refresh", "admcmd.headdb.refresh"
    );
    private static final AdminCommandDefinition VERIFY = definition(
        "headdb.verify", "averifyheaddb", "vantablack.command.averifyheaddb",
        AdminCommandDefinition.Risk.MEDIUM, AdminCommandDefinition.ReasonPolicy.REQUIRED,
        "service", "headdb.verify", "admcmd.headdb.verify"
    );
    private static final AdminCommandDefinition STATUS = definition(
        "headdb.status", "aheaddbstatus", "vantablack.command.aheaddbstatus",
        AdminCommandDefinition.Risk.LOW, AdminCommandDefinition.ReasonPolicy.OPTIONAL,
        "service", "headdb.status", "admcmd.headdb.status"
    );
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

    private void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(root("hdb"));
        dispatcher.register(root("headdb"));
        dispatcher.register(canonicalRemoteGive());
        dispatcher.register(canonicalPlayerGive());
        dispatcher.register(Commands.literal(REFRESH.literal())
            .then(Commands.argument(REASON_ARGUMENT, StringArgumentType.greedyString())
                .executes(context -> refresh(context, getString(context, REASON_ARGUMENT), false))));
        dispatcher.register(Commands.literal(VERIFY.literal())
            .then(Commands.argument(REASON_ARGUMENT, StringArgumentType.greedyString())
                .executes(context -> verify(context, getString(context, REASON_ARGUMENT), false))));
        dispatcher.register(Commands.literal(STATUS.literal())
            .executes(context -> auditedStatus(context, "", false)));
    }

    private LiteralArgumentBuilder<CommandSourceStack> canonicalRemoteGive() {
        return Commands.literal(GIVE_REMOTE.literal())
            .then(Commands.argument(ID_ARGUMENT, StringArgumentType.word())
                .suggests(this::suggestHeadIds)
                .then(Commands.argument(TARGET_ARGUMENT, StringArgumentType.string())
                    .suggests(this::suggestTargetsIncludingSelf)
                    .then(Commands.argument(AMOUNT_ARGUMENT, IntegerArgumentType.integer(1, MAX_GIVE_AMOUNT))
                        .then(Commands.argument(REASON_ARGUMENT, StringArgumentType.greedyString())
                            .executes(context -> giveRemoteHeadToTarget(
                                context,
                                getInteger(context, AMOUNT_ARGUMENT),
                                getString(context, REASON_ARGUMENT),
                                false
                            ))))));
    }

    private LiteralArgumentBuilder<CommandSourceStack> canonicalPlayerGive() {
        return Commands.literal(GIVE_PLAYER.literal())
            .then(Commands.argument(PLAYER_ARGUMENT, StringArgumentType.word())
                .then(Commands.argument(TARGET_ARGUMENT, StringArgumentType.string())
                    .suggests(this::suggestTargetsIncludingSelf)
                    .then(Commands.argument(AMOUNT_ARGUMENT, IntegerArgumentType.integer(1, MAX_GIVE_AMOUNT))
                        .then(Commands.argument(REASON_ARGUMENT, StringArgumentType.greedyString())
                            .executes(context -> givePlayerHeadToTarget(
                                context,
                                getInteger(context, AMOUNT_ARGUMENT),
                                getString(context, REASON_ARGUMENT),
                                false
                            ))))));
    }

    private LiteralArgumentBuilder<CommandSourceStack> root(String name) {
        return Commands.literal(name)
            .executes(this::openDefaultGui)
            .then(Commands.literal("help")
                .executes(this::sendHelp))
            .then(Commands.literal("status")
                .executes(context -> auditedStatus(context, legacyReason(name, "status"), true)))
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
                .then(Commands.argument(ID_ARGUMENT, StringArgumentType.word())
                    .suggests(this::suggestHeadIds)
                    .executes(context -> giveRemoteHead(context, context.getSource().getPlayerOrException(), 1, legacyReason(name, "give"), true))
                    .then(Commands.argument(AMOUNT_ARGUMENT, IntegerArgumentType.integer(1, MAX_GIVE_AMOUNT))
                        .executes(context -> giveRemoteHead(context, context.getSource().getPlayerOrException(), getInteger(context, AMOUNT_ARGUMENT), legacyReason(name, "give"), true)))
                    .then(Commands.argument(TARGET_ARGUMENT, StringArgumentType.string())
                        .suggests(this::suggestTargets)
                        .executes(context -> giveRemoteHeadToTarget(context, 1, legacyReason(name, "give"), true))
                        .then(Commands.argument(AMOUNT_ARGUMENT, IntegerArgumentType.integer(1, MAX_GIVE_AMOUNT))
                            .executes(context -> giveRemoteHeadToTarget(context, getInteger(context, AMOUNT_ARGUMENT), legacyReason(name, "give"), true))))))
            .then(Commands.literal("player")
                .then(Commands.argument(PLAYER_ARGUMENT, StringArgumentType.word())
                    .executes(context -> givePlayerHead(context, context.getSource().getPlayerOrException(), 1, legacyReason(name, "player"), true))
                    .then(Commands.argument(AMOUNT_ARGUMENT, IntegerArgumentType.integer(1, MAX_GIVE_AMOUNT))
                        .executes(context -> givePlayerHead(context, context.getSource().getPlayerOrException(), getInteger(context, AMOUNT_ARGUMENT), legacyReason(name, "player"), true)))
                    .then(Commands.argument(TARGET_ARGUMENT, StringArgumentType.string())
                        .suggests(this::suggestTargets)
                        .executes(context -> givePlayerHeadToTarget(context, 1, legacyReason(name, "player"), true))
                        .then(Commands.argument(AMOUNT_ARGUMENT, IntegerArgumentType.integer(1, MAX_GIVE_AMOUNT))
                            .executes(context -> givePlayerHeadToTarget(context, getInteger(context, AMOUNT_ARGUMENT), legacyReason(name, "player"), true))))))
            .then(Commands.literal("refresh")
                .executes(context -> refresh(context, legacyReason(name, "refresh"), true)))
            .then(Commands.literal("verify")
                .executes(context -> verify(context, legacyReason(name, "verify"), true)));
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
        source.sendSystemMessage(line("Give", "/agivehead <id> <target|self> <amount> <reason>"));
        source.sendSystemMessage(line("Player", "/agiveplayerhead <name|uuid> <target|self> <amount> <reason>"));
        source.sendSystemMessage(line("Admin", "/aheaddbstatus | /arefreshheaddb <reason> | /averifyheaddb <reason>"));
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
        String reason,
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

        warnLegacy(context.getSource(), legacy, GIVE_REMOTE.literal());
        Head value = head.get();
        Map<String, Object> parameters = Map.of(
            "headId", value.id().display(),
            "headName", value.name(),
            "amount", amount,
            "legacyAlias", legacy
        );
        return VantablackPermissions.adminCommands().execute(
            context.getSource(),
            GIVE_REMOTE,
            invocation(target, parameters, reason),
            () -> {
                giveStack(target, itemFactory.remoteHead(value, amount));
                if (context.getSource().getPlayer() != target) {
                    target.sendSystemMessage(success("Received " + value.name() + "."));
                }
                return completed(GIVE_REMOTE, "headdb_remote_head_given", parameters);
            }
        );
    }

    private int giveRemoteHeadToTarget(CommandContext<CommandSourceStack> context, int amount, String reason, boolean legacy) {
        Optional<ServerPlayer> target = findTarget(context);
        if (target.isEmpty()) {
            return 0;
        }
        return giveRemoteHead(context, target.get(), amount, reason, legacy);
    }

    private int givePlayerHead(
        CommandContext<CommandSourceStack> context,
        ServerPlayer target,
        int amount,
        String reason,
        boolean legacy
    ) {
        String player = getString(context, PLAYER_ARGUMENT);
        warnLegacy(context.getSource(), legacy, GIVE_PLAYER.literal());
        Map<String, Object> parameters = Map.of(
            "playerHead", player,
            "amount", amount,
            "legacyAlias", legacy
        );
        return VantablackPermissions.adminCommands().execute(
            context.getSource(),
            GIVE_PLAYER,
            invocation(target, parameters, reason),
            () -> {
                giveStack(target, itemFactory.playerHead(player, amount));
                return completed(GIVE_PLAYER, "headdb_player_head_given", parameters);
            }
        );
    }

    private int givePlayerHeadToTarget(CommandContext<CommandSourceStack> context, int amount, String reason, boolean legacy) {
        Optional<ServerPlayer> target = findTarget(context);
        if (target.isEmpty()) {
            return 0;
        }
        return givePlayerHead(context, target.get(), amount, reason, legacy);
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

    private int refresh(CommandContext<CommandSourceStack> context, String reason, boolean legacy) {
        warnLegacy(context.getSource(), legacy, REFRESH.literal());
        Map<String, Object> parameters = Map.of("legacyAlias", legacy);
        return VantablackPermissions.adminCommands().execute(
            context.getSource(),
            REFRESH,
            AdminCommandInvocation.target("service", "headdb", "HeadDB", parameters, reason),
            () -> databaseService.refreshAsync().thenApply(snapshot -> AdminCommandResult.success(
                1,
                "headdb_refreshed",
                REFRESH.audit().successTemplate(),
                Map.of("heads", snapshot.stats().heads(), "legacyAlias", legacy)
            ))
        );
    }

    private int verify(CommandContext<CommandSourceStack> context, String reason, boolean legacy) {
        warnLegacy(context.getSource(), legacy, VERIFY.literal());
        Map<String, Object> parameters = Map.of("legacyAlias", legacy);
        return VantablackPermissions.adminCommands().execute(
            context.getSource(),
            VERIFY,
            AdminCommandInvocation.target("service", "headdb", "HeadDB", parameters, reason),
            () -> databaseService.verifyAsync().thenApply(snapshot -> AdminCommandResult.success(
                1,
                "headdb_verified",
                VERIFY.audit().successTemplate(),
                Map.of("heads", snapshot.stats().heads(), "legacyAlias", legacy)
            ))
        );
    }

    private int auditedStatus(CommandContext<CommandSourceStack> context, String reason, boolean legacy) {
        warnLegacy(context.getSource(), legacy, STATUS.literal());
        Map<String, Object> parameters = Map.of("legacyAlias", legacy);
        return VantablackPermissions.adminCommands().execute(
            context.getSource(),
            STATUS,
            AdminCommandInvocation.target("service", "headdb", "HeadDB", parameters, reason),
            () -> {
                int commandResult = sendStatus(context);
                DatabaseStatus status = databaseService.status();
                return CompletableFuture.completedFuture(AdminCommandResult.success(
                    commandResult,
                    "headdb_status_viewed",
                    STATUS.audit().successTemplate(),
                    Map.of(
                        "state", status.state().name(),
                        "heads", status.stats().heads(),
                        "legacyAlias", legacy
                    )
                ));
            }
        );
    }

    private boolean canReceiveHead(ServerPlayer player) {
        return VantablackPermissions.allowed(player.createCommandSourceStack(), GIVE_REMOTE.permission());
    }

    private void giveGuiHead(ServerPlayer player, Head head) {
        Map<String, Object> parameters = Map.of(
            "headId", head.id().display(),
            "headName", head.name(),
            "amount", 1,
            "origin", "gui"
        );
        VantablackPermissions.adminCommands().execute(
            player.createCommandSourceStack(),
            GIVE_REMOTE,
            invocation(player, parameters, "HeadDB GUI grant"),
            () -> {
                giveStack(player, itemFactory.remoteHead(head, 1));
                return completed(GIVE_REMOTE, "headdb_remote_head_given", parameters);
            }
        );
    }

    private static CompletableFuture<AdminCommandResult> completed(
        AdminCommandDefinition definition,
        String resultCode,
        Map<String, Object> parameters
    ) {
        return CompletableFuture.completedFuture(AdminCommandResult.success(
            1,
            resultCode,
            definition.audit().successTemplate(),
            parameters
        ));
    }

    private static AdminCommandInvocation invocation(ServerPlayer target, Map<String, Object> parameters, String reason) {
        return AdminCommandInvocation.target(
            "character",
            target.getUUID().toString(),
            displayName(target),
            parameters,
            reason
        );
    }

    private static String legacyReason(String root, String subcommand) {
        return "Legacy alias /" + root + " " + subcommand + "; migrate before 0.3.0.";
    }

    private static void warnLegacy(CommandSourceStack source, boolean legacy, String replacement) {
        if (legacy) {
            source.sendSystemMessage(Component.literal(
                "Deprecated admin alias; use /" + replacement + ". This alias is removed in 0.3.0."
            ).withStyle(ChatFormatting.YELLOW));
        }
    }

    private static AdminCommandDefinition definition(
        String id,
        String literal,
        String permission,
        AdminCommandDefinition.Risk risk,
        AdminCommandDefinition.ReasonPolicy reason,
        String targetType,
        String eventType,
        String successTemplate
    ) {
        return new AdminCommandDefinition(
            id,
            literal,
            permission,
            risk,
            reason,
            targetType,
            new AdminCommandDefinition.Audit(eventType, successTemplate, "vantablack.audit.admin.receive")
        );
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

}
