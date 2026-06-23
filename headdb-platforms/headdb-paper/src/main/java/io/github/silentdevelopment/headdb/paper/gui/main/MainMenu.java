package io.github.silentdevelopment.headdb.paper.gui.main;

import io.github.silentdevelopment.grafik.key.GKey;
import io.github.silentdevelopment.grafik.key.GuiKey;
import io.github.silentdevelopment.grafik.paper.gui.dynamic.PaperDynamicGuiConfigurator;
import io.github.silentdevelopment.grafik.paper.gui.dynamic.PaperDynamicGuiDefinition;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.gui.MenuState;
import io.github.silentdevelopment.headdb.paper.gui.settings.LanguagesPageFactory;
import io.github.silentdevelopment.headdb.paper.gui.settings.SettingsPageFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class MainMenu implements PaperDynamicGuiDefinition<MenuState> {

    private static final GKey<GuiKey> KEY = GKey.of("headdb_main");

    private final HeadDBPlugin plugin;

    public MainMenu(@NotNull HeadDBPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public @NotNull GKey<GuiKey> key() {
        return KEY;
    }

    @Override
    public void configure(@NotNull PaperDynamicGuiConfigurator<MenuState> gui) {
        gui.initialPage(MainPageFactory.KEY);
        gui.page(new MainPageFactory(plugin));
        gui.page(new SettingsPageFactory(plugin));
        gui.page(new LanguagesPageFactory(plugin));
    }
}