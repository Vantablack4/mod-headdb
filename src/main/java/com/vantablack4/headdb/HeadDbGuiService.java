package com.vantablack4.headdb;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.query.HeadQuery;
import io.github.silentdevelopment.headdb.query.HeadQueryResult;
import io.github.silentdevelopment.headdb.query.HeadSort;
import io.github.silentdevelopment.headdb.query.SortDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

final class HeadDbGuiService {
    static final int ROWS = 6;
    static final int COLUMNS = 9;
    static final int MENU_SIZE = ROWS * COLUMNS;
    static final int PAGE_SIZE = 28;
    static final int SLOT_BACK = 45;
    static final int SLOT_PREVIOUS = 48;
    static final int SLOT_SUMMARY = 49;
    static final int SLOT_NEXT = 50;
    static final int SLOT_EMPTY = 22;
    static final int[] RESULT_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };

    private final HeadDbConfig config;
    private final HeadDbDatabaseService databaseService;
    private final FabricHeadItemFactory itemFactory;

    HeadDbGuiService(HeadDbConfig config, HeadDbDatabaseService databaseService, FabricHeadItemFactory itemFactory) {
        this.config = Objects.requireNonNull(config, "config");
        this.databaseService = Objects.requireNonNull(databaseService, "databaseService");
        this.itemFactory = Objects.requireNonNull(itemFactory, "itemFactory");
    }

    int openDefault(ServerPlayer player) {
        return open(player, "", 0);
    }

    int openSearch(ServerPlayer player, String queryText) {
        return open(player, queryText, 0);
    }

    private int open(ServerPlayer player, String queryText, int requestedPage) {
        if (!databaseService.status().available()) {
            player.sendSystemMessage(error("HeadDB database is not loaded yet. Run /hdb status or /hdb refresh."));
            return 0;
        }

        String normalizedQuery = normalizeQuery(queryText);
        int requestedIndex = Math.max(0, requestedPage);
        HeadQueryResult result = search(normalizedQuery, requestedIndex);
        int pageIndex = clampedPageIndex(requestedPage, result.total(), PAGE_SIZE);
        if (pageIndex != requestedIndex) {
            result = search(normalizedQuery, pageIndex);
        }

        HeadQueryResult finalResult = result;
        int finalPageIndex = pageIndex;
        player.openMenu(new SimpleMenuProvider(
            (syncId, inventory, menuPlayer) -> new HeadDbSearchMenu(
                this,
                syncId,
                inventory,
                player,
                normalizedQuery,
                finalPageIndex,
                finalResult
            ),
            title(normalizedQuery, finalPageIndex, finalResult.total())
        ));
        return 1;
    }

    private HeadQueryResult search(String queryText, int pageIndex) {
        boolean blank = queryText.isBlank();
        HeadQuery query = HeadQuery.builder()
            .text(queryText)
            .sort(blank ? HeadSort.ID : HeadSort.RELEVANCE)
            .direction(blank ? SortDirection.ASCENDING : SortDirection.DESCENDING)
            .offset(pageIndex * PAGE_SIZE)
            .limit(PAGE_SIZE)
            .build();
        return databaseService.database().search(query);
    }

    private void giveHead(ServerPlayer player, Head head) {
        if (!canReceiveHead(player)) {
            player.sendSystemMessage(error("You need operator permission to receive heads from HeadDB."));
            return;
        }

        ItemStack stack = itemFactory.remoteHead(head, 1);
        giveStack(player, stack);
        player.sendSystemMessage(success("Received " + head.name() + "."));
    }

    private boolean canReceiveHead(ServerPlayer player) {
        return player.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(config.adminPermissionLevel())));
    }

    private void giveStack(ServerPlayer target, ItemStack stack) {
        ItemStack remaining = stack.copy();
        boolean inserted = target.getInventory().add(remaining);
        if (!inserted && !remaining.isEmpty()) {
            target.drop(remaining, false);
        }
        target.inventoryMenu.broadcastChanges();
    }

    private ItemStack displayHead(ServerPlayer viewer, Head head) {
        ItemStack stack = itemFactory.remoteHead(head, 1);
        List<Component> lore = new ArrayList<>();
        lore.add(detailLine("ID", head.id().display()));
        lore.add(detailLine("Category", head.category()));
        if (!head.tags().isEmpty()) {
            lore.add(detailLine("Tags", String.join(", ", head.tags())));
        }
        if (!head.collections().isEmpty()) {
            lore.add(detailLine("Collections", String.join(", ", head.collections())));
        }
        lore.add(canReceiveHead(viewer)
            ? Component.literal("Click to receive.").withStyle(ChatFormatting.GREEN)
            : Component.literal("Operator permission required.").withStyle(ChatFormatting.RED));
        stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    private static Component title(String queryText, int pageIndex, int total) {
        String label = queryText.isBlank() ? "HeadDB" : "HDB: " + truncate(queryText, 18);
        return Component.literal(label + " " + (pageIndex + 1) + "/" + pageCount(total, PAGE_SIZE));
    }

    private static String normalizeQuery(String queryText) {
        return queryText == null ? "" : queryText.trim();
    }

    static int pageCount(int total, int pageSize) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Page size must be positive.");
        }
        if (total <= 0) {
            return 1;
        }
        return (total + pageSize - 1) / pageSize;
    }

    static int clampedPageIndex(int requestedPage, int total, int pageSize) {
        int maxIndex = pageCount(total, pageSize) - 1;
        return Math.min(Math.max(0, requestedPage), maxIndex);
    }

    static boolean isResultSlot(int slot) {
        return resultIndex(slot) >= 0;
    }

    static int resultIndex(int slot) {
        for (int index = 0; index < RESULT_SLOTS.length; index++) {
            if (RESULT_SLOTS[index] == slot) {
                return index;
            }
        }
        return -1;
    }

    private static String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 1)) + ".";
    }

    private static Component detailLine(String label, String value) {
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

    private static ItemStack named(Item item, Component name) {
        return named(item, name, List.of());
    }

    private static ItemStack named(Item item, Component name, List<Component> lore) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, name);
        if (!lore.isEmpty()) {
            stack.set(DataComponents.LORE, new ItemLore(lore));
        }
        return stack;
    }

    private static final class HeadDbSearchMenu extends ChestMenu {
        private final HeadDbGuiService service;
        private final SimpleContainer guiContainer;
        private final ServerPlayer viewer;
        private final String queryText;
        private final int pageIndex;
        private final HeadQueryResult result;

        private HeadDbSearchMenu(
            HeadDbGuiService service,
            int syncId,
            Inventory playerInventory,
            ServerPlayer viewer,
            String queryText,
            int pageIndex,
            HeadQueryResult result
        ) {
            this(service, syncId, playerInventory, viewer, queryText, pageIndex, result, new SimpleContainer(MENU_SIZE));
        }

        private HeadDbSearchMenu(
            HeadDbGuiService service,
            int syncId,
            Inventory playerInventory,
            ServerPlayer viewer,
            String queryText,
            int pageIndex,
            HeadQueryResult result,
            SimpleContainer guiContainer
        ) {
            super(MenuType.GENERIC_9x6, syncId, playerInventory, guiContainer, ROWS);
            this.service = service;
            this.guiContainer = guiContainer;
            this.viewer = viewer;
            this.queryText = queryText;
            this.pageIndex = pageIndex;
            this.result = result;
            render();
        }

        @Override
        public boolean stillValid(Player player) {
            return player == viewer && guiContainer.stillValid(player);
        }

        @Override
        public ItemStack quickMoveStack(Player player, int slotIndex) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean canDragTo(Slot slot) {
            return slot.container != guiContainer && super.canDragTo(slot);
        }

        @Override
        public void clicked(int slotId, int button, ContainerInput input, Player player) {
            if (slotId >= 0 && slotId < MENU_SIZE) {
                if ((input == ContainerInput.PICKUP || input == ContainerInput.QUICK_MOVE) && player == viewer) {
                    if (handleGuiClick(slotId)) {
                        return;
                    }
                }
                broadcastFullState();
                return;
            }

            if (input == ContainerInput.QUICK_MOVE) {
                broadcastFullState();
                return;
            }

            super.clicked(slotId, button, input, player);
        }

        private boolean handleGuiClick(int slotId) {
            if (slotId == SLOT_BACK) {
                if (queryText.isBlank()) {
                    viewer.closeContainer();
                } else {
                    service.open(viewer, "", 0);
                }
                return true;
            }

            if (slotId == SLOT_PREVIOUS && result.hasPreviousPage()) {
                service.open(viewer, queryText, pageIndex - 1);
                return true;
            }

            if (slotId == SLOT_NEXT && result.hasNextPage()) {
                service.open(viewer, queryText, pageIndex + 1);
                return true;
            }

            int resultIndex = resultIndex(slotId);
            if (resultIndex >= 0 && resultIndex < result.heads().size()) {
                service.giveHead(viewer, result.heads().get(resultIndex));
            }
            return false;
        }

        private void render() {
            guiContainer.clearContent();
            fillBorder();
            for (int index = 0; index < result.heads().size() && index < RESULT_SLOTS.length; index++) {
                guiContainer.setItem(RESULT_SLOTS[index], service.displayHead(viewer, result.heads().get(index)));
            }

            if (result.heads().isEmpty()) {
                guiContainer.setItem(SLOT_EMPTY, named(
                    Items.BARRIER,
                    Component.literal("No results").withStyle(ChatFormatting.RED),
                    List.of(Component.literal("Try a different search.").withStyle(ChatFormatting.GRAY))
                ));
            }

            guiContainer.setItem(SLOT_BACK, named(
                queryText.isBlank() ? Items.BARRIER : Items.ARROW,
                Component.literal(queryText.isBlank() ? "Close" : "All heads").withStyle(ChatFormatting.YELLOW),
                queryText.isBlank()
                    ? List.of(Component.literal("Close this menu.").withStyle(ChatFormatting.GRAY))
                    : List.of(Component.literal("Return to the full catalog.").withStyle(ChatFormatting.GRAY))
            ));

            if (result.hasPreviousPage()) {
                guiContainer.setItem(SLOT_PREVIOUS, named(
                    Items.ARROW,
                    Component.literal("Previous page").withStyle(ChatFormatting.YELLOW),
                    List.of(Component.literal("Page " + pageIndex).withStyle(ChatFormatting.GRAY))
                ));
            }

            guiContainer.setItem(SLOT_SUMMARY, summaryItem());

            if (result.hasNextPage()) {
                guiContainer.setItem(SLOT_NEXT, named(
                    Items.ARROW,
                    Component.literal("Next page").withStyle(ChatFormatting.YELLOW),
                    List.of(Component.literal("Page " + (pageIndex + 2)).withStyle(ChatFormatting.GRAY))
                ));
            }

            guiContainer.setChanged();
        }

        private ItemStack summaryItem() {
            int totalPages = pageCount(result.total(), PAGE_SIZE);
            String queryLabel = queryText.isBlank() ? "All heads" : queryText;
            List<Component> lore = List.of(
                detailLine("Query", queryLabel),
                detailLine("Matches", Integer.toString(result.total())),
                detailLine("Page", (pageIndex + 1) + "/" + totalPages),
                detailLine("Showing", Integer.toString(result.heads().size()))
            );
            return named(
                Items.BOOK,
                Component.literal("Search results").withStyle(ChatFormatting.GOLD),
                lore
            );
        }

        private void fillBorder() {
            ItemStack filler = named(Items.BLACK_STAINED_GLASS_PANE, Component.literal(" ").withStyle(ChatFormatting.DARK_GRAY));
            for (int slot = 0; slot < MENU_SIZE; slot++) {
                int row = slot / COLUMNS;
                int column = slot % COLUMNS;
                if (row == 0 || row == ROWS - 1 || column == 0 || column == COLUMNS - 1) {
                    guiContainer.setItem(slot, filler.copy());
                }
            }
        }
    }
}
