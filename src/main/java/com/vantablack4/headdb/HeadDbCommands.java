package com.vantablack4.headdb;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.silentdevelopment.headdb.core.database.DatabaseSnapshot;
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
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.item.ItemStack;

public final class HeadDbCommands {
    private static final String ID_ARGUMENT = "id";
    private static final String QUERY_ARGUMENT = "query";
    private static final String PLAYER_ARGUMENT = "player";
    private static final String TARGET_ARGUMENT = "target";
    private static final String AMOUNT_ARGUMENT = "amount";
    private static final int MAX_GIVE_AMOUNT = 64;

    private final HeadDbConfig config;
    private final HeadDbDatabaseService databaseService;
    private final FabricHeadItemFactory itemFactory;
    private final HeadDbGuiService guiService;

    public HeadDbCommands(HeadDbConfig config, HeadDbDatabaseService databaseService, FabricHeadItemFactory itemFactory) {
        this.config = config;
        this.databaseService = databaseService;
        this.itemFactory = itemFactory;
        this.guiService = new HeadDbGuiService(config, databaseService, itemFactory);
    }

    public void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(root("hdb"));
        dispatcher.register(root("headdb"));
    }

    private LiteralArgumentBuilder<CommandSourceStack> root(String name) {
        return Commands.literal(name)
            .executes(this::openDefaultGui)
            .then(Commands.literal("help")
                .executes(this::sendHelp))
            .then(Commands.literal("status")
                .executes(this::sendStatus))
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
                .requires(this::isAdmin)
                .then(Commands.argument(ID_ARGUMENT, StringArgumentType.word())
                    .suggests(this::suggestHeadIds)
                    .executes(context -> giveRemoteHead(context, context.getSource().getPlayerOrException(), 1))
                    .then(Commands.argument(AMOUNT_ARGUMENT, IntegerArgumentType.integer(1, MAX_GIVE_AMOUNT))
                        .executes(context -> giveRemoteHead(context, context.getSource().getPlayerOrException(), getInteger(context, AMOUNT_ARGUMENT))))
                    .then(Commands.argument(TARGET_ARGUMENT, EntityArgument.player())
                        .executes(context -> giveRemoteHead(context, EntityArgument.getPlayer(context, TARGET_ARGUMENT), 1))
                        .then(Commands.argument(AMOUNT_ARGUMENT, IntegerArgumentType.integer(1, MAX_GIVE_AMOUNT))
                            .executes(context -> giveRemoteHead(context, EntityArgument.getPlayer(context, TARGET_ARGUMENT), getInteger(context, AMOUNT_ARGUMENT)))))))
            .then(Commands.literal("player")
                .requires(this::isAdmin)
                .then(Commands.argument(PLAYER_ARGUMENT, StringArgumentType.word())
                    .executes(context -> givePlayerHead(context, context.getSource().getPlayerOrException(), 1))
                    .then(Commands.argument(AMOUNT_ARGUMENT, IntegerArgumentType.integer(1, MAX_GIVE_AMOUNT))
                        .executes(context -> givePlayerHead(context, context.getSource().getPlayerOrException(), getInteger(context, AMOUNT_ARGUMENT))))
                    .then(Commands.argument(TARGET_ARGUMENT, EntityArgument.player())
                        .executes(context -> givePlayerHead(context, EntityArgument.getPlayer(context, TARGET_ARGUMENT), 1))
                        .then(Commands.argument(AMOUNT_ARGUMENT, IntegerArgumentType.integer(1, MAX_GIVE_AMOUNT))
                            .executes(context -> givePlayerHead(context, EntityArgument.getPlayer(context, TARGET_ARGUMENT), getInteger(context, AMOUNT_ARGUMENT)))))))
            .then(Commands.literal("refresh")
                .requires(this::isAdmin)
                .executes(this::refresh))
            .then(Commands.literal("verify")
                .requires(this::isAdmin)
                .executes(this::verify));
    }

    private boolean isAdmin(CommandSourceStack source) {
        return source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(config.adminPermissionLevel())));
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
        source.sendSystemMessage(line("Give", "/hdb give <id> [amount] | /hdb give <id> <player> [amount]"));
        source.sendSystemMessage(line("Player", "/hdb player <name|uuid> [amount] | /hdb player <name|uuid> <player> [amount]"));
        source.sendSystemMessage(line("Admin", "/hdb status | refresh | verify"));
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

    private int giveRemoteHead(CommandContext<CommandSourceStack> context, ServerPlayer target, int amount) {
        if (!databaseAvailable(context.getSource())) {
            return 0;
        }

        Optional<Head> head = findHead(context);
        if (head.isEmpty()) {
            context.getSource().sendSystemMessage(error("Unknown remote head ID."));
            return 0;
        }

        ItemStack stack = itemFactory.remoteHead(head.get(), amount);
        giveStack(target, stack);
        context.getSource().sendSystemMessage(success("Gave " + amount + "x " + head.get().name() + " to " + displayName(target) + "."));
        if (context.getSource().getPlayer() != target) {
            target.sendSystemMessage(success("Received " + head.get().name() + "."));
        }
        return 1;
    }

    private int givePlayerHead(CommandContext<CommandSourceStack> context, ServerPlayer target, int amount) {
        String player = getString(context, PLAYER_ARGUMENT);
        ItemStack stack = itemFactory.playerHead(player, amount);
        giveStack(target, stack);
        context.getSource().sendSystemMessage(success("Gave " + amount + "x player head to " + displayName(target) + "."));
        return 1;
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

    private int refresh(CommandContext<CommandSourceStack> context) {
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

    private int verify(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSystemMessage(line("HeadDB", "Remote verification started."));
        databaseService.verifyAsync().whenComplete((snapshot, throwable) -> source.getServer().execute(() -> {
            if (throwable != null) {
                source.sendSystemMessage(error(rootCause(throwable)));
                return;
            }
            source.sendSystemMessage(success("HeadDB remote verification passed with " + snapshot.stats().heads() + " heads."));
        }));
        return 1;
    }

    private boolean databaseAvailable(CommandSourceStack source) {
        if (databaseService.status().available()) {
            return true;
        }
        source.sendSystemMessage(error("HeadDB database is not loaded yet. Run /hdb status or /hdb refresh."));
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
