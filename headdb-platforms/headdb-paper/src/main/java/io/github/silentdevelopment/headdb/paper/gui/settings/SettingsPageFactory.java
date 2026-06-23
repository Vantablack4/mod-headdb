package io.github.silentdevelopment.headdb.paper.gui.settings;

import io.github.silentdevelopment.grafik.gui.GuiContext;
import io.github.silentdevelopment.grafik.key.GKey;
import io.github.silentdevelopment.grafik.key.PageKey;
import io.github.silentdevelopment.grafik.paper.core.element.ItemElement;
import io.github.silentdevelopment.grafik.paper.page.PaperPage;
import io.github.silentdevelopment.grafik.paper.page.PaperPageBuilder;
import io.github.silentdevelopment.grafik.paper.page.PaperPageFactory;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.gui.MenuState;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiHeadIcons;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiItems;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiTitles;
import io.github.silentdevelopment.headdb.paper.gui.main.MainPageFactory;
import io.github.silentdevelopment.headdb.paper.message.MessageKey;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MenuType;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public final class SettingsPageFactory implements PaperPageFactory<MenuState> {

    public static final GKey<PageKey> KEY = GKey.of("settings");

    private static final int ROWS = 6;
    private static final int SLOT_DENIED = 22;

    private final HeadDBPlugin plugin;

    public SettingsPageFactory(@NotNull HeadDBPlugin plugin) {
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

        page.type(MenuType.GENERIC_9X6);
        Player player = player(context);
        boolean adminMode = player != null && plugin.adminModes().enabled(player);
        page.title(GuiTitles.title("HeadDB Settings", adminMode));

        Set<Integer> reservedSlots = new HashSet<>();
        set(page, reservedSlots, slot("settings.back", 45), backButton());

        if (player == null || !Permissions.has(player, Permissions.GUI_SETTINGS)) {
            set(page, reservedSlots, SLOT_DENIED, deniedButton());
            GuiItems.fillEmpty(plugin, page, ROWS, reservedSlots);
            return page.build();
        }

        if (Permissions.has(player, Permissions.GUI_SETTINGS_LANGUAGE)) {
            set(page, reservedSlots, slot("settings.language", 20), languagesButton());
        }

        if (Permissions.has(player, Permissions.GUI_ADMIN_MODE)) {
            set(page, reservedSlots, slot("settings.admin-mode", 13), adminModeButton(player));
        }


        if (Permissions.has(player, Permissions.DEBUG)) {
            set(page, reservedSlots, slot("settings.debug", 24), commandButton("debug", "debug", Permissions.DEBUG, "hdb debug"));
        }

        if (Permissions.has(player, Permissions.VERIFY)) {
            set(page, reservedSlots, slot("settings.verify", 33), commandButton("verify", "verify", Permissions.VERIFY, "hdb verify"));
        }

        if (Permissions.has(player, Permissions.REFRESH)) {
            set(page, reservedSlots, slot("settings.refresh", 42), commandButton("refresh", "refresh", Permissions.REFRESH, "hdb refresh"));
        }

        if (Permissions.has(player, Permissions.RELOAD)) {
            set(page, reservedSlots, slot("settings.reload", 40), commandButton("reload", "reload", Permissions.RELOAD, "hdb reload"));
        }

        GuiItems.fillEmpty(plugin, page, ROWS, reservedSlots);
        return page.build();
    }

    private @NotNull ItemElement<MenuState> backButton() {
        return GuiHeadIcons.<MenuState>button(plugin, "back", "back", context -> context.openPage(MainPageFactory.KEY));
    }

    private @NotNull ItemElement<MenuState> languagesButton() {
        return GuiHeadIcons.<MenuState>button(plugin, "languages", "languages", context -> {
            Player player = player(context);
            if (player == null) {
                return;
            }

            if (!Permissions.has(player, Permissions.GUI_SETTINGS_LANGUAGE)) {
                player.sendMessage(plugin.messages().render(player, MessageKey.COMMAND_ERROR_NO_PERMISSION));
                return;
            }

            context.openPage(LanguagesPageFactory.KEY);
        });
    }

    private @NotNull ItemElement<MenuState> adminModeButton(@NotNull Player viewer) {
        boolean enabled = plugin.adminModes().enabled(viewer);
        String icon = enabled ? "admin-mode-on" : "admin-mode-off";
        return GuiHeadIcons.<MenuState>button(plugin, "admin_mode", icon, GuiItems.name("Mode", NamedTextColor.GOLD), GuiItems.miniLore(plugin.guiConfig().icon(icon).lore()), context -> {
            Player player = player(context);
            if (player == null) {
                return;
            }

            if (!Permissions.has(player, Permissions.GUI_ADMIN_MODE)) {
                player.sendMessage(plugin.messages().render(player, MessageKey.COMMAND_ERROR_NO_PERMISSION));
                return;
            }

            boolean updated = plugin.adminModes().toggle(player);
            player.sendMessage(Component.text(updated ? "Admin Mode enabled." : "User Mode enabled.", updated ? NamedTextColor.GREEN : NamedTextColor.GRAY));
            context.refresh();
        });
    }

    private @NotNull ItemElement<MenuState> commandButton(@NotNull String elementKey, @NotNull String iconKey, @NotNull String permission, @NotNull String command) {
        Objects.requireNonNull(elementKey, "elementKey");
        Objects.requireNonNull(iconKey, "iconKey");
        Objects.requireNonNull(permission, "permission");
        Objects.requireNonNull(command, "command");

        return GuiHeadIcons.<MenuState>button(plugin, elementKey, iconKey, context -> {
            Player player = player(context);
            if (player == null) {
                return;
            }

            if (!Permissions.has(player, permission)) {
                player.sendMessage(plugin.messages().render(player, MessageKey.COMMAND_ERROR_NO_PERMISSION));
                return;
            }

            player.closeInventory();
            player.performCommand(command);
        });
    }

    private @NotNull ItemElement<MenuState> deniedButton() {
        return GuiHeadIcons.<MenuState>button(plugin, "settings_denied", "no-permission", GuiItems.name("No Permission", NamedTextColor.RED), List.of(GuiItems.lore("You cannot open this settings page.", NamedTextColor.GRAY)), ignored -> {});
    }

    private int slot(@NotNull String key, int fallback) {
        return plugin.guiConfig().slot(key, fallback);
    }

    private static void set(@NotNull PaperPageBuilder<MenuState> page, @NotNull Set<Integer> reservedSlots, int slot, @NotNull ItemElement<MenuState> element) {
        page.set(slot, element);
        reservedSlots.add(slot);
    }

    private Player player(@NotNull GuiContext<MenuState> context) {
        Objects.requireNonNull(context, "context");
        return Bukkit.getPlayer(context.source().viewerId());
    }
}
