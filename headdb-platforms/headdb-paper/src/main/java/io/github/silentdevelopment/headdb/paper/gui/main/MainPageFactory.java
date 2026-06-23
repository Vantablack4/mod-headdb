package io.github.silentdevelopment.headdb.paper.gui.main;

import io.github.silentdevelopment.grafik.gui.GuiContext;
import io.github.silentdevelopment.grafik.key.GKey;
import io.github.silentdevelopment.grafik.key.PageKey;
import io.github.silentdevelopment.grafik.paper.core.element.ItemElement;
import io.github.silentdevelopment.grafik.paper.page.PaperPage;
import io.github.silentdevelopment.grafik.paper.page.PaperPageBuilder;
import io.github.silentdevelopment.grafik.paper.page.PaperPageFactory;
import io.github.silentdevelopment.headdb.database.DatabaseStats;
import io.github.silentdevelopment.headdb.database.DatabaseStatus;
import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadCategory;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.gui.MenuState;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiHeadIcons;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiItems;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiTitles;
import io.github.silentdevelopment.headdb.paper.gui.hidden.HiddenHeadsMenu;
import io.github.silentdevelopment.headdb.paper.gui.config.GuiButtonEditorMenu;
import io.github.silentdevelopment.headdb.paper.gui.config.GuiIconConfig;
import io.github.silentdevelopment.headdb.paper.item.HeadItemIds;
import io.github.silentdevelopment.headdb.paper.gui.settings.SettingsPageFactory;
import io.github.silentdevelopment.headdb.paper.message.MessageKey;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import io.github.silentdevelopment.headdb.paper.search.SearchRequest;
import io.github.silentdevelopment.headdb.query.HeadSort;
import io.github.silentdevelopment.headdb.query.SortDirection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public final class MainPageFactory implements PaperPageFactory<MenuState> {

    public static final GKey<PageKey> KEY = GKey.of("main");

    private static final String CATEGORY_PAGE_STATE_KEY = "headdb.main.category-page";
    private static final int ROWS = 6;
    private static final int SLOT_CLOSE = 45;
    private static final int SLOT_PREVIOUS = 48;
    private static final int SLOT_NEXT = 50;
    private static final int[] CATEGORY_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            37, 38, 39, 40, 41, 42, 43
    };

    private final HeadDBPlugin plugin;

    public MainPageFactory(@NotNull HeadDBPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public @NotNull GKey<PageKey> key() {
        return KEY;
    }

    @Override
    public @NotNull PaperPage<MenuState> create(@NotNull GuiContext<MenuState> context, @NotNull PaperPageBuilder<MenuState> page) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(page, "page");

        Player player = player(context);
        boolean adminMode = player != null && plugin.adminModes().enabled(player);

        page.type(MenuType.GENERIC_9X6);
        page.title(GuiTitles.title("HeadDB", adminMode));

        Set<Integer> reservedSlots = new HashSet<>();
        renderCategories(context, page, reservedSlots, adminMode);
        renderControls(context, page, reservedSlots, adminMode);
        GuiItems.fillEmpty(plugin, page, ROWS, reservedSlots);

        return page.build();
    }

    private void renderCategories(@NotNull GuiContext<MenuState> context, @NotNull PaperPageBuilder<MenuState> page, @NotNull Set<Integer> reservedSlots, boolean adminMode) {
        Player player = player(context);
        if (player == null) {
            return;
        }

        if (!Permissions.has(player, Permissions.GUI_CATEGORY_VIEW)) {
            return;
        }

        List<HeadCategory> categories = categories(player);
        Map<String, Head> iconHeads = categoryIconHeads(categories, adminMode);
        Map<String, Integer> counts = plugin.headRegistry().categoryCounts(adminMode);
        int pageIndex = categoryPage(context);
        int fromIndex = pageIndex * CATEGORY_SLOTS.length;

        if (fromIndex >= categories.size() && pageIndex > 0) {
            pageIndex = Math.max(0, pageCount(categories.size()) - 1);
            setCategoryPage(context, pageIndex);
            fromIndex = pageIndex * CATEGORY_SLOTS.length;
        }

        int toIndex = Math.min(categories.size(), fromIndex + CATEGORY_SLOTS.length);
        int slotIndex = 0;
        for (int index = fromIndex; index < toIndex; index++) {
            HeadCategory category = categories.get(index);
            int slot = CATEGORY_SLOTS[slotIndex];
            set(page, reservedSlots, slot, categoryButton(category, iconHeads.get(category.id()), counts.getOrDefault(category.id(), 0), adminMode));
            slotIndex++;
        }
    }

    private void renderControls(@NotNull GuiContext<MenuState> context, @NotNull PaperPageBuilder<MenuState> page, @NotNull Set<Integer> reservedSlots, boolean adminMode) {
        Player player = player(context);
        if (player == null) {
            return;
        }

        List<HeadCategory> categories = categories(player);
        int categoryPage = categoryPage(context);
        int categoryPages = pageCount(categories.size());

        set(page, reservedSlots, SLOT_CLOSE, closeButton());

        if (Permissions.has(player, Permissions.GUI_BROWSE) && Permissions.canViewAllCategories(player)) {
            set(page, reservedSlots, slot("main.browse-heads", 31), browseAllButton());
        }

        if (Permissions.has(player, Permissions.GUI_PLAYER_HEADS)) {
            set(page, reservedSlots, slot("main.player-heads", 37), playerHeadsButton());
        }

        if (Permissions.has(player, Permissions.GUI_CUSTOM_HEADS)) {
            set(page, reservedSlots, slot("main.custom-heads", 28), customHeadsButton());
        }

        if (Permissions.has(player, Permissions.GUI_FAVORITES)) {
            set(page, reservedSlots, slot("main.favorites", 40), favoritesButton());
        }

        if (adminMode && Permissions.has(player, Permissions.GUI_HIDDEN_HEADS)) {
            set(page, reservedSlots, slot("main.hidden-heads", 51), hiddenHeadsButton());
        }

        if (Permissions.has(player, Permissions.GUI_SEARCH)) {
            set(page, reservedSlots, slot("main.search", 43), searchButton());
        }

        if (Permissions.has(player, Permissions.GUI_MORE_CATEGORIES)) {
            set(page, reservedSlots, slot("main.more-categories", 34), moreCategoriesButton());
        }

        if (categoryPage > 0) {
            set(page, reservedSlots, SLOT_PREVIOUS, previousCategoriesButton(categoryPage));
        }

        set(page, reservedSlots, slot("main.info", 49), infoButton(categories.size(), adminMode));

        if (categoryPage + 1 < categoryPages) {
            set(page, reservedSlots, SLOT_NEXT, nextCategoriesButton(categoryPage));
        }

        if (Permissions.has(player, Permissions.GUI_SETTINGS)) {
            set(page, reservedSlots, slot("main.settings", 53), settingsButton());
        }
    }

    private @NotNull ItemElement<MenuState> categoryButton(@NotNull HeadCategory category, Head iconHead, int amount, boolean adminMode) {
        Objects.requireNonNull(category, "category");

        return ItemElement.<MenuState>of(GKey.of("category_" + category.id()), context -> {
            Player player = player(context);
            boolean showDetails = player != null && adminMode;
            return categoryItem(category, iconHead, amount, showDetails);
        }, context -> {
            Player player = player(context);
            if (player == null) {
                return;
            }

            if (!Permissions.has(player, Permissions.GUI_CATEGORY_OPEN) || !Permissions.canViewCategory(player, category.id())) {
                player.sendMessage(plugin.messages().render(player, MessageKey.COMMAND_ERROR_NO_PERMISSION));
                return;
            }

            plugin.guis().openSearch(player, categoryRequest(category));
        });
    }

    private @NotNull ItemElement<MenuState> closeButton() {
        return GuiHeadIcons.<MenuState>button(plugin, "close", "close", context -> {
            Player player = player(context);
            if (player != null) {
                player.closeInventory();
            }
        });
    }

    private @NotNull ItemElement<MenuState> previousCategoriesButton(int currentPage) {
        return GuiHeadIcons.<MenuState>button(plugin, "previous_categories", "previous", context -> {
            setCategoryPage(context, currentPage - 1);
            context.refresh();
        });
    }

    private @NotNull ItemElement<MenuState> nextCategoriesButton(int currentPage) {
        return GuiHeadIcons.<MenuState>button(plugin, "next_categories", "next", context -> {
            setCategoryPage(context, currentPage + 1);
            context.refresh();
        });
    }

    private @NotNull ItemElement<MenuState> infoButton(int visibleCategories, boolean adminMode) {
        DatabaseStatus status = plugin.runtime().database().status();
        DatabaseStats stats = status.stats();
        List<Component> lore = new ArrayList<>();
        lore.add(GuiItems.idDetail("Heads", stats.heads()));
        lore.add(GuiItems.idDetail("Categories", stats.categories()));
        lore.add(GuiItems.idDetail("Tags", stats.tags()));
        lore.add(GuiItems.idDetail("Collections", stats.collections()));

        if (adminMode) {
            lore.add(Component.empty());
            lore.add(GuiItems.metaDetail("State", status.state()));
            lore.add(GuiItems.metaDetail("Source", status.source()));
            lore.add(GuiItems.idDetail("Visible categories", visibleCategories));
            lore.add(GuiItems.idDetail("Hidden heads", plugin.headRegistry().hiddenHeads().size()));
        }

        return GuiHeadIcons.<MenuState>button(plugin, "main_info", "info", GuiItems.name("HeadDB", adminMode ? NamedTextColor.RED : NamedTextColor.GOLD), lore, ignored -> {});
    }

    private @NotNull ItemElement<MenuState> searchButton() {
        return GuiHeadIcons.<MenuState>button(plugin, "main_search", "search", context -> {
            Player player = player(context);
            if (player == null) {
                return;
            }

            if (!Permissions.has(player, Permissions.GUI_SEARCH) || !Permissions.has(player, Permissions.SEARCH)) {
                player.sendMessage(plugin.messages().render(player, MessageKey.COMMAND_ERROR_NO_PERMISSION));
                return;
            }

            player.closeInventory();
            plugin.prompts().request(player, Component.text("Enter a search query.", NamedTextColor.GOLD), query -> {
                sendSearchQueryInfo(player, query);
                plugin.guis().openSearch(player, new SearchRequest(query, Set.of(), Set.of(), Set.of(), Set.of(), HeadSort.RELEVANCE, SortDirection.DESCENDING, 1, 28, false));
            }, () -> sendSearchCancelled(player));
        });
    }

    private @NotNull ItemElement<MenuState> playerHeadsButton() {
        return GuiHeadIcons.<MenuState>button(plugin, "player_heads", plugin.guiConfig().icon("player-heads"), context -> {
            Player player = player(context);
            if (player != null) {
                plugin.guis().openPlayerHeads(player);
            }
        }, context -> mainButtonName("player-heads"), context -> mainButtonLore(context, "player-heads", plugin.headRegistry().playerHeads().knownPlayers().size()));
    }

    private @NotNull ItemElement<MenuState> customHeadsButton() {
        return GuiHeadIcons.<MenuState>button(plugin, "custom_heads", plugin.guiConfig().icon("custom-heads"), context -> {
            Player player = player(context);
            if (player != null) {
                plugin.guis().openCustomHeads(player);
            }
        }, context -> mainButtonName("custom-heads"), context -> mainButtonLore(context, "custom-heads", plugin.headRegistry().customHeads().list().size()));
    }


    private @NotNull ItemElement<MenuState> favoritesButton() {
        return GuiHeadIcons.<MenuState>button(plugin, "favorites", "favorites", context -> {
            Player player = player(context);
            if (player != null) {
                plugin.guis().openFavorites(player);
            }
        });
    }

    private @NotNull ItemElement<MenuState> moreCategoriesButton() {
        return GuiHeadIcons.<MenuState>button(plugin, "more_categories", "more-categories", context -> {
            Player player = player(context);
            if (player != null) {
                plugin.guis().openMoreCategories(player);
            }
        });
    }

    private @NotNull ItemElement<MenuState> hiddenHeadsButton() {
        return GuiHeadIcons.<MenuState>button(plugin, "hidden_heads", "hidden-heads", context -> {
            Player player = player(context);
            if (player == null) {
                return;
            }

            if (!plugin.adminModes().enabled(player) || !Permissions.has(player, Permissions.GUI_HIDDEN_HEADS)) {
                player.sendMessage(plugin.messages().render(player, MessageKey.COMMAND_ERROR_NO_PERMISSION));
                return;
            }

            HiddenHeadsMenu.open(plugin, player);
        });
    }

    private @NotNull ItemElement<MenuState> browseAllButton() {
        return GuiHeadIcons.<MenuState>button(plugin, "browse_all", plugin.guiConfig().icon("browse-all"), context -> {
            Player player = player(context);
            if (player == null) {
                return;
            }

            if (!Permissions.has(player, Permissions.GUI_BROWSE) || !Permissions.canViewAllCategories(player)) {
                player.sendMessage(plugin.messages().render(player, MessageKey.COMMAND_ERROR_NO_PERMISSION));
                return;
            }

            plugin.guis().openSearch(player, browseRequest());
        }, context -> mainButtonName("browse-all"), context -> mainButtonLore(context, "browse-all", plugin.headRegistry().heads(adminMode(context)).size()));
    }

    private @NotNull ItemElement<MenuState> settingsButton() {
        return GuiHeadIcons.<MenuState>button(plugin, "settings", "settings", context -> {
            Player player = player(context);
            if (player == null) {
                return;
            }

            if (!Permissions.has(player, Permissions.GUI_SETTINGS)) {
                player.sendMessage(plugin.messages().render(player, MessageKey.COMMAND_ERROR_NO_PERMISSION));
                return;
            }

            context.openPage(SettingsPageFactory.KEY);
        });
    }

    private @NotNull Component mainButtonName(@NotNull String iconKey) {
        return GuiItems.mini(plugin.guiConfig().icon(iconKey).name());
    }

    private @NotNull List<Component> mainButtonLore(@NotNull GuiContext<MenuState> context, @NotNull String iconKey, int heads) {
        List<Component> lore = new ArrayList<>(GuiItems.miniLore(plugin.guiConfig().icon(iconKey).lore()));
        if (adminMode(context)) {
            lore.add(Component.empty());
            lore.add(GuiItems.idDetail("Heads", heads));
        }
        return List.copyOf(lore);
    }

    private boolean adminMode(@NotNull GuiContext<MenuState> context) {
        Player player = player(context);
        return player != null && plugin.adminModes().enabled(player);
    }

    private @NotNull ItemStack categoryItem(@NotNull HeadCategory category, Head iconHead, int amount, boolean showTechnicalDetails) {
        Objects.requireNonNull(category, "category");

        String iconKey = categoryIconKey(category.id());
        boolean configured = plugin.guiConfig().hasIcon(iconKey);
        GuiIconConfig icon = plugin.guiConfig().iconOrDefault(iconKey, "category");
        ItemStack item = categoryIcon(category, iconHead, configured, icon);

        item.editMeta(meta -> {
            List<Component> lore = new ArrayList<>();

            if (configured) {
                lore.addAll(GuiItems.miniLore(replaceAll(icon.lore(), category, amount)));
            } else {
                String description = category.description();
                if (description != null && !description.isBlank()) {
                    lore.add(Component.text(description, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                }
            }

            if (!lore.isEmpty()) {
                lore.add(Component.empty());
            }

            if (showTechnicalDetails) {
                lore.add(GuiItems.idDetail("Heads", amount));
                lore.add(GuiItems.idDetail("ID", category.id()));
                lore.add(Component.empty());
            }

            lore.add(Component.text("Click to browse this category.", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
            Component name = configured ? GuiItems.mini(replace(icon.name(), category, amount)) : Component.text(categoryTitle(category.name(), amount), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false);
            meta.displayName(name);
            meta.lore(lore);
            meta.getPersistentDataContainer().remove(HeadItemIds.key(plugin));
            meta.getPersistentDataContainer().set(GuiButtonEditorMenu.iconKey(plugin), org.bukkit.persistence.PersistentDataType.STRING, iconKey);
        });

        return item;
    }

    private @NotNull ItemStack categoryIcon(@NotNull HeadCategory category, Head iconHead, boolean configured, @NotNull GuiIconConfig icon) {
        if (configured) {
            return GuiHeadIcons.icon(plugin, icon);
        }

        if (iconHead == null) {
            return GuiHeadIcons.icon(plugin, plugin.guiConfig().icon("category"));
        }

        return plugin.itemFactory().create(iconHead);
    }


    private static @NotNull String categoryIconKey(@NotNull String categoryId) {
        Objects.requireNonNull(categoryId, "categoryId");
        return "category." + categoryId.trim().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
    }

    private static @NotNull String replace(@NotNull String value, @NotNull HeadCategory category, int amount) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(category, "category");

        return value.replace("%name%", category.name()).replace("%id%", category.id()).replace("%count%", String.valueOf(amount));
    }

    private static @NotNull List<String> replaceAll(@NotNull List<String> values, @NotNull HeadCategory category, int amount) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(category, "category");

        List<String> result = new ArrayList<>();
        for (String value : values) {
            result.add(replace(value, category, amount));
        }
        return List.copyOf(result);
    }

    private @NotNull Map<String, Head> categoryIconHeads(@NotNull List<HeadCategory> categories, boolean includeHidden) {
        Objects.requireNonNull(categories, "categories");

        Map<String, Head> icons = new LinkedHashMap<>();
        Set<String> missing = new HashSet<>();
        for (HeadCategory category : categories) {
            missing.add(category.id());
        }

        if (missing.isEmpty()) {
            return icons;
        }

        List<Head> candidateHeads = new ArrayList<>(plugin.headRegistry().heads(includeHidden));

        for (Head head : candidateHeads) {
            String category = head.category();
            if (!missing.contains(category)) {
                continue;
            }

            icons.put(category, head);
            missing.remove(category);
            if (missing.isEmpty()) {
                return icons;
            }
        }

        for (Head head : plugin.headRegistry().customHeads().list()) {
            String category = head.category();
            if (!missing.contains(category)) {
                continue;
            }

            icons.put(category, head);
            missing.remove(category);
            if (missing.isEmpty()) {
                return icons;
            }
        }

        return icons;
    }

    private @NotNull List<HeadCategory> categories(@NotNull Player player) {
        Objects.requireNonNull(player, "player");
        return plugin.headRegistry().categories().stream().filter(category -> Permissions.canViewCategory(player, category.id())).sorted(Comparator.comparing(HeadCategory::name, String.CASE_INSENSITIVE_ORDER)).toList();
    }


    private @NotNull String categoryTitle(@NotNull String name, int amount) {
        return plugin.guiConfig().text("category-count", "%name% (%count%)").replace("%name%", name).replace("%count%", String.valueOf(amount));
    }

    private int slot(@NotNull String key, int fallback) {
        return plugin.guiConfig().slot(key, fallback);
    }

    private static int categoryPage(@NotNull GuiContext<MenuState> context) {
        Integer value = context.state().get(CATEGORY_PAGE_STATE_KEY, Integer.class);
        if (value == null || value < 0) {
            return 0;
        }

        return value;
    }

    private static void setCategoryPage(@NotNull GuiContext<MenuState> context, int page) {
        context.state().put(CATEGORY_PAGE_STATE_KEY, Math.max(0, page));
    }

    private static int pageCount(int entries) {
        if (entries <= 0) {
            return 1;
        }

        return (int) Math.ceil((double) entries / (double) CATEGORY_SLOTS.length);
    }

    private static Player player(@NotNull GuiContext<MenuState> context) {
        Objects.requireNonNull(context, "context");
        return Bukkit.getPlayer(context.source().viewerId());
    }

    private static void sendSearchQueryInfo(@NotNull Player player, @NotNull String query) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(query, "query");
        player.sendMessage(Component.text("Search Info", NamedTextColor.GOLD, TextDecoration.BOLD));
        player.sendMessage(Component.text("Query: ", NamedTextColor.GRAY).append(Component.text(query, NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Sort: ", NamedTextColor.GRAY).append(Component.text("relevance descending", NamedTextColor.AQUA)));
    }

    private static void sendSearchCancelled(@NotNull Player player) {
        Objects.requireNonNull(player, "player");
        player.sendMessage(Component.text("Search cancelled.", NamedTextColor.GRAY));
    }

    private static @NotNull SearchRequest browseRequest() {
        return new SearchRequest("", Set.of(), Set.of(), Set.of(), Set.of(), HeadSort.ID, SortDirection.ASCENDING, 1, 28, false);
    }

    private static @NotNull SearchRequest categoryRequest(@NotNull HeadCategory category) {
        Objects.requireNonNull(category, "category");
        return new SearchRequest("", Set.of(), Set.of(category.id()), Set.of(), Set.of(), HeadSort.ID, SortDirection.ASCENDING, 1, 28, true);
    }

    private static void set(@NotNull PaperPageBuilder<MenuState> page, @NotNull Set<Integer> reservedSlots, int slot, @NotNull ItemElement<MenuState> element) {
        page.set(slot, element);
        reservedSlots.add(slot);
    }
}
