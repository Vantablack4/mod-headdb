package io.github.silentdevelopment.headdb.paper.gui.search;

import io.github.silentdevelopment.grafik.gui.GuiContext;
import io.github.silentdevelopment.grafik.key.GKey;
import io.github.silentdevelopment.grafik.key.PageKey;
import io.github.silentdevelopment.grafik.paper.core.element.ItemElement;
import io.github.silentdevelopment.grafik.paper.page.PaperPage;
import io.github.silentdevelopment.grafik.paper.page.PaperPageBuilder;
import io.github.silentdevelopment.grafik.paper.page.PaperPageFactory;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.search.SearchRequest;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiHeadIcons;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiItems;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiTitles;
import io.github.silentdevelopment.headdb.query.HeadSort;
import io.github.silentdevelopment.headdb.query.SortDirection;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MenuType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public final class SearchOptionsPageFactory implements PaperPageFactory<SearchMenuState> {

    public static final GKey<PageKey> KEY = GKey.of("search_options");

    private static final int ROWS = 6;

    private static final int SLOT_SORT = 20;
    private static final int SLOT_DIRECTION = 22;
    private static final int SLOT_CATEGORY = 24;
    private static final int SLOT_TAGS = 29;
    private static final int SLOT_COLLECTIONS = 31;
    private static final int SLOT_CLEAR_FILTERS = 33;
    private static final int SLOT_BACK = 45;
    private static final int SLOT_INFO = 49;

    private final HeadDBPlugin plugin;

    public SearchOptionsPageFactory(@NotNull HeadDBPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public @NotNull GKey<PageKey> key() {
        return KEY;
    }

    @Override
    public @NotNull PaperPage<SearchMenuState> create(
            @NotNull GuiContext<SearchMenuState> context,
            @NotNull PaperPageBuilder<SearchMenuState> page
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(page, "page");

        SearchRequest request = SearchState.request(context);
        Set<Integer> reservedSlots = new HashSet<>();

        page.type(MenuType.GENERIC_9X6);
        Player player = player(context);
        page.title(GuiTitles.title("Sort / Filter", player != null && plugin.adminModes().enabled(player)));

        if (player == null || !Permissions.has(player, Permissions.GUI_FILTER)) {
            set(page, reservedSlots, SLOT_BACK, backButton());
            set(page, reservedSlots, SLOT_INFO, deniedButton());
            GuiItems.fillEmpty(plugin, page, ROWS, reservedSlots);
            return page.build();
        }

        set(page, reservedSlots, SLOT_SORT, sortCycleButton(request));
        set(page, reservedSlots, SLOT_DIRECTION, directionButton(request));

        if (request.canChangeCategory()) {
            set(page, reservedSlots, SLOT_CATEGORY, categoryButton(request));
        }

        set(page, reservedSlots, SLOT_TAGS, tagsButton(request));
        set(page, reservedSlots, SLOT_COLLECTIONS, collectionsButton(request));

        if (request.hasFilters()) {
            set(page, reservedSlots, SLOT_CLEAR_FILTERS, clearFiltersButton(request));
        }

        set(page, reservedSlots, SLOT_BACK, backButton());
        set(page, reservedSlots, SLOT_INFO, summaryButton(request));

        GuiItems.fillEmpty(plugin, page, ROWS, reservedSlots);

        return page.build();
    }


    private @NotNull ItemElement<SearchMenuState> deniedButton() {
        return GuiHeadIcons.<SearchMenuState>button(plugin, "empty", "empty", GuiItems.name("No Permission", NamedTextColor.RED), List.of(GuiItems.lore("You cannot change search filters.", NamedTextColor.GRAY)), ignored -> {});
    }

    private @NotNull ItemElement<SearchMenuState> sortCycleButton(@NotNull SearchRequest request) {
        Objects.requireNonNull(request, "request");

        return GuiHeadIcons.<SearchMenuState>button(
                plugin,
                "sort_cycle",
                "sort-cycle",
                GuiItems.name("Sort: " + displayEnum(request.sort().name()), NamedTextColor.GOLD),
                sortLore(request.sort()),
                context -> {
                    SearchRequest currentRequest = SearchState.request(context);
                    SearchState.request(context, currentRequest.withSort(nextSort(currentRequest.sort())));
                    context.refresh();
                }
        );
    }

    private @NotNull ItemElement<SearchMenuState> directionButton(@NotNull SearchRequest request) {
        Objects.requireNonNull(request, "request");

        return GuiHeadIcons.<SearchMenuState>button(
                plugin,
                "sort_direction",
                "sort-direction",
                GuiItems.name("Direction: " + displayEnum(request.direction().name()), NamedTextColor.GOLD),
                directionLore(request.direction()),
                context -> {
                    SearchRequest currentRequest = SearchState.request(context);
                    SortDirection updated = currentRequest.direction() == SortDirection.ASCENDING
                            ? SortDirection.DESCENDING
                            : SortDirection.ASCENDING;

                    SearchState.request(context, currentRequest.withDirection(updated));
                    context.refresh();
                }
        );
    }

    private @NotNull ItemElement<SearchMenuState> categoryButton(@NotNull SearchRequest request) {
        Objects.requireNonNull(request, "request");

        return GuiHeadIcons.<SearchMenuState>button(
                plugin,
                "filter_category",
                "filter-category",
                GuiItems.name("Category Filters", NamedTextColor.GOLD),
                List.of(
                        GuiItems.idDetail("Selected", request.categories().size()),
                        GuiItems.lore("Click to toggle categories.", NamedTextColor.GREEN)
                ),
                context -> context.openPage(SearchFilterPageFactory.CATEGORY_KEY)
        );
    }

    private @NotNull ItemElement<SearchMenuState> tagsButton(@NotNull SearchRequest request) {
        Objects.requireNonNull(request, "request");

        return GuiHeadIcons.<SearchMenuState>button(
                plugin,
                "filter_tags",
                "filter-tags",
                GuiItems.name("Tag Filters", NamedTextColor.GOLD),
                List.of(
                        GuiItems.idDetail("Selected", request.tags().size()),
                        GuiItems.lore("Click to toggle tags.", NamedTextColor.GREEN)
                ),
                context -> context.openPage(SearchFilterPageFactory.TAGS_KEY)
        );
    }

    private @NotNull ItemElement<SearchMenuState> collectionsButton(@NotNull SearchRequest request) {
        Objects.requireNonNull(request, "request");

        return GuiHeadIcons.<SearchMenuState>button(
                plugin,
                "filter_collections",
                "filter-collections",
                GuiItems.name("Collection Filters", NamedTextColor.GOLD),
                List.of(
                        GuiItems.idDetail("Selected", request.collections().size()),
                        GuiItems.lore("Click to toggle collections.", NamedTextColor.GREEN)
                ),
                context -> context.openPage(SearchFilterPageFactory.COLLECTIONS_KEY)
        );
    }

    private @NotNull ItemElement<SearchMenuState> clearFiltersButton(@NotNull SearchRequest request) {
        Objects.requireNonNull(request, "request");

        return GuiHeadIcons.<SearchMenuState>button(
                plugin,
                "clear_filters",
                "clear-filters",
                GuiItems.name("Clear Filters", NamedTextColor.RED),
                List.of(
                        GuiItems.idDetail("Categories", request.categoryLocked() ? "locked" : request.categories().size()),
                        GuiItems.idDetail("Tags", request.tags().size()),
                        GuiItems.idDetail("Collections", request.collections().size()),
                        GuiItems.lore("Click to clear selected filters.", NamedTextColor.GREEN)
                ),
                context -> {
                    SearchRequest currentRequest = SearchState.request(context);
                    SearchState.request(context, currentRequest.withoutFilters());
                    context.refresh();
                }
        );
    }

    private @NotNull ItemElement<SearchMenuState> summaryButton(@NotNull SearchRequest request) {
        Objects.requireNonNull(request, "request");

        return GuiHeadIcons.<SearchMenuState>button(
                plugin,
                "options_summary",
                "info",
                GuiItems.name("Current Search", NamedTextColor.GOLD),
                List.of(
                        GuiItems.metaDetail("Query", request.query().isBlank() ? "none" : request.query()),
                        GuiItems.idDetail("Categories", categorySummary(request)),
                        GuiItems.idDetail("Tags", request.tags().size()),
                        GuiItems.idDetail("Collections", request.collections().size()),
                        GuiItems.metaDetail("Sort", displayEnum(request.sort().name()) + " " + displayEnum(request.direction().name()))
                ),
                ignored -> {}
        );
    }

    private @NotNull ItemElement<SearchMenuState> backButton() {
        return GuiHeadIcons.<SearchMenuState>button(
                plugin,
                "back",
                "back",
                context -> context.openPage(SearchPageFactory.KEY)
        );
    }

    private static @NotNull List<Component> sortLore(@NotNull HeadSort selected) {
        Objects.requireNonNull(selected, "selected");

        List<Component> lore = new ArrayList<>();

        lore.add(GuiItems.lore("Click to cycle sort mode.", NamedTextColor.DARK_GRAY));
        lore.add(Component.empty());

        for (HeadSort sort : HeadSort.values()) {
            lore.add(optionLine(displayEnum(sort.name()), sort == selected));
        }

        return List.copyOf(lore);
    }

    private static @NotNull List<Component> directionLore(@NotNull SortDirection selected) {
        Objects.requireNonNull(selected, "selected");

        List<Component> lore = new ArrayList<>();

        lore.add(GuiItems.lore("Click to toggle direction.", NamedTextColor.DARK_GRAY));
        lore.add(Component.empty());

        for (SortDirection direction : SortDirection.values()) {
            lore.add(optionLine(displayEnum(direction.name()), direction == selected));
        }

        return List.copyOf(lore);
    }

    private static @NotNull Component optionLine(@NotNull String text, boolean selected) {
        Objects.requireNonNull(text, "text");

        if (selected) {
            return Component.text(text, NamedTextColor.GREEN, TextDecoration.BOLD);
        }

        return Component.text(text, NamedTextColor.GRAY);
    }

    private static @NotNull HeadSort nextSort(@NotNull HeadSort current) {
        Objects.requireNonNull(current, "current");

        HeadSort[] values = HeadSort.values();

        for (int index = 0; index < values.length; index++) {
            if (values[index] != current) {
                continue;
            }

            return values[(index + 1) % values.length];
        }

        return values[0];
    }

    private static @NotNull String categorySummary(@NotNull SearchRequest request) {
        Objects.requireNonNull(request, "request");

        if (request.categoryLocked()) {
            return request.category() + " (locked)";
        }

        if (request.categories().isEmpty()) {
            return "all";
        }

        return String.valueOf(request.categories().size());
    }

    private static @NotNull String displayEnum(@NotNull String value) {
        Objects.requireNonNull(value, "value");

        String[] parts = value.toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();

        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }

            if (!builder.isEmpty()) {
                builder.append(' ');
            }

            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }

        return builder.toString();
    }

    private static Player player(@NotNull GuiContext<SearchMenuState> context) {
        Objects.requireNonNull(context, "context");
        return Bukkit.getPlayer(context.source().viewerId());
    }

    private static void set(
            @NotNull PaperPageBuilder<SearchMenuState> page,
            @NotNull Set<Integer> reservedSlots,
            int slot,
            @NotNull ItemElement<SearchMenuState> element
    ) {
        page.set(slot, element);
        reservedSlots.add(slot);
    }
}