package io.github.silentdevelopment.headdb.paper.gui.config;

import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.config.ConfigException;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;

public final class GuiIconConfigEditor {

    private final HeadDBPlugin plugin;
    private final Path path;

    public GuiIconConfigEditor(@NotNull HeadDBPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.path = plugin.getDataFolder().toPath().resolve("gui.yml");
    }

    public void setName(@NotNull String key, @NotNull String name) {
        update(key, icon -> icon.put("name", name));
    }

    public void setMaterial(@NotNull String key, @NotNull String material) {
        update(key, icon -> icon.put("material", material.toUpperCase(java.util.Locale.ROOT)));
    }

    public void setType(@NotNull String key, @NotNull String type) {
        update(key, icon -> icon.put("type", type.toUpperCase(java.util.Locale.ROOT)));
    }

    public void setHeadId(@NotNull String key, @NotNull String headId) {
        update(key, icon -> icon.put("head-id", headId));
    }

    public void setLore(@NotNull String key, @NotNull List<String> lore) {
        update(key, icon -> icon.put("lore", new ArrayList<>(lore)));
    }

    public void clearLore(@NotNull String key) {
        setLore(key, List.of());
    }

    public void reset(@NotNull String key) {
        Objects.requireNonNull(key, "key");

        try {
            Map<String, Object> root = load();
            Map<String, Object> icons = section(root, "icons");
            Optional<Map<String, Object>> defaultIcon = defaultIcon(key);

            if (defaultIcon.isPresent()) {
                icons.put(key, defaultIcon.get());
            } else {
                icons.remove(key);
            }

            root.put("icons", icons);
            save(root);
            plugin.reloadGuiConfigOnly();
        } catch (Exception exception) {
            throw new ConfigException("Failed to reset GUI icon config for " + key, exception);
        }
    }

    private void update(@NotNull String key, @NotNull java.util.function.Consumer<Map<String, Object>> mutator) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(mutator, "mutator");

        try {
            Map<String, Object> root = load();
            Map<String, Object> icons = section(root, "icons");
            Map<String, Object> icon = section(icons, key);
            ensureDefaults(icons, key, icon);
            mutator.accept(icon);
            icons.put(key, icon);
            root.put("icons", icons);
            save(root);
            plugin.reloadGuiConfigOnly();
        } catch (Exception exception) {
            throw new ConfigException("Failed to update GUI icon config for " + key, exception);
        }
    }


    private static void ensureDefaults(@NotNull Map<String, Object> icons, @NotNull String key, @NotNull Map<String, Object> icon) {
        Objects.requireNonNull(icons, "icons");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(icon, "icon");

        Map<String, Object> fallback = fallbackIcon(icons, key);
        icon.putIfAbsent("type", fallback.getOrDefault("type", "ITEM"));
        icon.putIfAbsent("head-id", fallback.getOrDefault("head-id", ""));
        icon.putIfAbsent("material", fallback.getOrDefault("material", "STONE"));
        icon.putIfAbsent("name", fallback.getOrDefault("name", "<white>" + key));
        icon.putIfAbsent("lore", fallback.getOrDefault("lore", List.of()));
    }

    private static @NotNull Map<String, Object> fallbackIcon(@NotNull Map<String, Object> icons, @NotNull String key) {
        Objects.requireNonNull(icons, "icons");
        Objects.requireNonNull(key, "key");

        if (key.startsWith("category.")) {
            Object fallback = icons.get("category");
            if (fallback instanceof Map<?, ?> map) {
                return stringKeyMap(map);
            }
        }

        return Map.of();
    }


    private @NotNull Optional<Map<String, Object>> defaultIcon(@NotNull String key) throws Exception {
        try (InputStream stream = plugin.getResource("gui.yml")) {
            if (stream == null) {
                return Optional.empty();
            }

            Object loaded = new Yaml().load(stream);
            if (!(loaded instanceof Map<?, ?> map)) {
                return Optional.empty();
            }

            Map<String, Object> root = stringKeyMap(map);
            Object iconsObject = root.get("icons");
            if (!(iconsObject instanceof Map<?, ?> iconsMap)) {
                return Optional.empty();
            }

            Map<String, Object> icons = stringKeyMap(iconsMap);
            Object exact = icons.get(key);
            if (exact instanceof Map<?, ?> exactMap) {
                return Optional.of(stringKeyMap(exactMap));
            }

            if (key.startsWith("category.")) {
                return Optional.empty();
            }

            return Optional.empty();
        }
    }

    private @NotNull Map<String, Object> load() throws Exception {
        if (Files.notExists(path)) {
            plugin.reloadGuiConfigOnly();
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Object loaded = new Yaml().load(reader);
            if (loaded == null) {
                return new LinkedHashMap<>();
            }

            if (!(loaded instanceof Map<?, ?> map)) {
                throw new ConfigException("gui.yml root must be an object.");
            }

            return stringKeyMap(map);
        }
    }

    private void save(@NotNull Map<String, Object> root) throws Exception {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        options.setWidth(120);
        options.setSplitLines(false);

        Yaml yaml = new Yaml(options);
        Files.createDirectories(path.getParent());
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            yaml.dump(root, writer);
        }
    }

    private static @NotNull Map<String, Object> section(@NotNull Map<String, Object> root, @NotNull String key) {
        Object value = root.get(key);
        if (!(value instanceof Map<?, ?> map)) {
            Map<String, Object> created = new LinkedHashMap<>();
            root.put(key, created);
            return created;
        }

        return stringKeyMap(map);
    }

    private static @NotNull Map<String, Object> stringKeyMap(@NotNull Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            result.put(String.valueOf(entry.getKey()), deepCopy(entry.getValue()));
        }
        return result;
    }

    private static Object deepCopy(Object value) {
        if (value instanceof Map<?, ?> map) {
            return stringKeyMap(map);
        }

        if (value instanceof List<?> list) {
            return new ArrayList<>(list);
        }

        return value;
    }
}
