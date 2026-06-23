package io.github.silentdevelopment.headdb.paper.gui;

import io.github.silentdevelopment.grafik.gui.dynamic.DynamicGui;
import io.github.silentdevelopment.grafik.paper.PaperGrafik;
import io.github.silentdevelopment.grafik.paper.core.PaperGrafiks;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.gui.main.MainMenu;
import io.github.silentdevelopment.headdb.paper.gui.category.MoreCategoriesMenu;
import io.github.silentdevelopment.headdb.paper.gui.favorites.FavoritesMenu;
import io.github.silentdevelopment.headdb.paper.gui.local.LocalHeadListMenu;
import io.github.silentdevelopment.headdb.paper.gui.search.SearchMenu;
import io.github.silentdevelopment.headdb.paper.gui.search.SearchMenuState;
import io.github.silentdevelopment.headdb.paper.item.HeadItemFactory;
import io.github.silentdevelopment.headdb.paper.message.MessageKey;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import io.github.silentdevelopment.headdb.paper.search.SearchRequest;
import io.github.silentdevelopment.headdb.paper.search.SearchResultCache;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class GuiService {

    private final HeadDBPlugin plugin;
    private final PaperGrafik grafik;
    private final DynamicGui<MenuState> mainGui;
    private final DynamicGui<SearchMenuState> searchGui;
    private final SearchResultCache searchResultCache;

    public GuiService(@NotNull HeadDBPlugin plugin, @NotNull HeadItemFactory itemFactory) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(itemFactory, "itemFactory");

        this.searchResultCache = new SearchResultCache();
        this.grafik = PaperGrafiks.create(plugin);
        this.mainGui = grafik.prepare(new MainMenu(plugin));
        this.searchGui = grafik.prepare(new SearchMenu(plugin, itemFactory, searchResultCache));
    }

    public void openMain(@NotNull Player player) {
        Objects.requireNonNull(player, "player");

        if (!Permissions.has(player, Permissions.GUI_MAIN)) {
            player.sendMessage(plugin.messages().render(player, MessageKey.COMMAND_ERROR_NO_PERMISSION));
            return;
        }

        grafik.open(mainGui, new MenuState(player.getUniqueId()), player);
    }

    public void openSearch(@NotNull Player player, @NotNull SearchRequest request) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(request, "request");

        if (request.category() != null) {
            if (!Permissions.has(player, Permissions.GUI_CATEGORY_OPEN) || !Permissions.canViewCategory(player, request.category())) {
                player.sendMessage(plugin.messages().render(player, MessageKey.COMMAND_ERROR_NO_PERMISSION));
                return;
            }
        } else if (!Permissions.has(player, Permissions.GUI_BROWSE) || !Permissions.canViewAllCategories(player)) {
            player.sendMessage(plugin.messages().render(player, MessageKey.COMMAND_ERROR_NO_PERMISSION));
            return;
        }

        if (!request.query().isBlank() && !Permissions.has(player, Permissions.SEARCH)) {
            player.sendMessage(plugin.messages().render(player, MessageKey.COMMAND_ERROR_NO_PERMISSION));
            return;
        }

        grafik.open(searchGui, new SearchMenuState(player.getUniqueId(), request), player);
    }


    public void openPlayerHeads(@NotNull Player player) {
        Objects.requireNonNull(player, "player");

        if (!Permissions.has(player, Permissions.GUI_PLAYER_HEADS)) {
            player.sendMessage(plugin.messages().render(player, MessageKey.COMMAND_ERROR_NO_PERMISSION));
            return;
        }

        LocalHeadListMenu.openPlayers(plugin, player);
    }

    public void openFavorites(@NotNull Player player) {
        Objects.requireNonNull(player, "player");

        if (!Permissions.has(player, Permissions.GUI_FAVORITES)) {
            player.sendMessage(plugin.messages().render(player, MessageKey.COMMAND_ERROR_NO_PERMISSION));
            return;
        }

        FavoritesMenu.open(plugin, player);
    }

    public void openMoreCategories(@NotNull Player player) {
        Objects.requireNonNull(player, "player");

        if (!Permissions.has(player, Permissions.GUI_MORE_CATEGORIES)) {
            player.sendMessage(plugin.messages().render(player, MessageKey.COMMAND_ERROR_NO_PERMISSION));
            return;
        }

        MoreCategoriesMenu.open(plugin, player);
    }

    public void openCustomHeads(@NotNull Player player) {
        Objects.requireNonNull(player, "player");

        if (!Permissions.has(player, Permissions.GUI_CUSTOM_HEADS)) {
            player.sendMessage(plugin.messages().render(player, MessageKey.COMMAND_ERROR_NO_PERMISSION));
            return;
        }

        LocalHeadListMenu.openCustom(plugin, player);
    }

    public void clearSearchCache() {
        searchResultCache.clear();
    }

    public int searchCacheSize() {
        return searchResultCache.size();
    }
}