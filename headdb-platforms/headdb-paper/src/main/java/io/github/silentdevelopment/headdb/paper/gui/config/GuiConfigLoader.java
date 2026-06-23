package io.github.silentdevelopment.headdb.paper.gui.config;

import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.config.ConfigException;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class GuiConfigLoader {

    private static final String FILE_NAME = "gui.yml";

    private final HeadDBPlugin plugin;
    private final Path dataDirectory;

    public GuiConfigLoader(@NotNull HeadDBPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.dataDirectory = plugin.getDataFolder().toPath();
    }

    public @NotNull GuiConfig load() {
        Path guiPath = dataDirectory.resolve(FILE_NAME);

        try {
            Files.createDirectories(dataDirectory);

            Map<String, Object> defaults = loadBundledDefaults();
            Map<String, Object> current;

            if (Files.notExists(guiPath)) {
                current = deepCopyMap(defaults);
                saveMap(guiPath, current);
            } else {
                current = loadMap(guiPath);
            }

            boolean changed = normalizeLoreValues(current);
            changed |= mergeMissing(current, defaults);

            GuiConfig config = GuiConfig.fromMap(current);
            config.validate();

            if (changed) {
                saveMap(guiPath, current);
            }

            return config;
        } catch (Exception exception) {
            throw new ConfigException("Failed to load HeadDB GUI config from " + guiPath, exception);
        }
    }

    private @NotNull Map<String, Object> loadBundledDefaults() throws IOException {
        try (InputStream input = plugin.getResource(FILE_NAME)) {
            if (input == null) {
                throw new ConfigException("Bundled " + FILE_NAME + " resource is missing.");
            }

            try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                Object loaded = new Yaml().load(reader);

                if (!(loaded instanceof Map<?, ?> map)) {
                    throw new ConfigException("Bundled " + FILE_NAME + " root must be a YAML object.");
                }

                return stringKeyMap(map);
            }
        }
    }

    private static boolean mergeMissing(@NotNull Map<String, Object> target, @NotNull Map<String, Object> defaults) {
        boolean changed = false;

        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            String key = entry.getKey();
            Object defaultValue = entry.getValue();

            if (!target.containsKey(key) || target.get(key) == null) {
                target.put(key, deepCopy(defaultValue));
                changed = true;
                continue;
            }

            Object currentValue = target.get(key);

            if (currentValue instanceof Map<?, ?> rawCurrent && defaultValue instanceof Map<?, ?> rawDefault) {
                Map<String, Object> current = stringKeyMap(rawCurrent);
                Map<String, Object> defaultMap = stringKeyMap(rawDefault);

                if (mergeMissing(current, defaultMap)) {
                    target.put(key, current);
                    changed = true;
                }
            }
        }

        return changed;
    }

    private static boolean normalizeLoreValues(@NotNull Map<String, Object> map) {
        boolean changed = false;

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (key.equals("lore")) {
                if (value == null) {
                    entry.setValue(new ArrayList<String>());
                    changed = true;
                    continue;
                }

                if (value instanceof String scalar) {
                    if (scalar.isBlank()) {
                        entry.setValue(new ArrayList<String>());
                    } else {
                        entry.setValue(new ArrayList<>(List.of(scalar)));
                    }

                    changed = true;
                    continue;
                }
            }

            if (value instanceof Map<?, ?> rawChild) {
                Map<String, Object> child = stringKeyMap(rawChild);

                if (normalizeLoreValues(child)) {
                    entry.setValue(child);
                    changed = true;
                }
            }
        }

        return changed;
    }

    private static @NotNull Map<String, Object> loadMap(@NotNull Path path) throws IOException {
        if (Files.notExists(path)) {
            return new LinkedHashMap<>();
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Object loaded = new Yaml().load(reader);

            if (loaded == null) {
                return new LinkedHashMap<>();
            }

            if (!(loaded instanceof Map<?, ?> map)) {
                throw new ConfigException(path.getFileName() + " root must be a YAML object.");
            }

            return stringKeyMap(map);
        }
    }

    private static void saveMap(@NotNull Path path, @NotNull Map<String, Object> map) throws IOException {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        options.setWidth(120);
        options.setSplitLines(false);

        Yaml yaml = new Yaml(options);

        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            yaml.dump(map, writer);
        }
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
            return deepCopyMap(stringKeyMap(map));
        }

        if (value instanceof List<?> list) {
            List<Object> result = new ArrayList<>();

            for (Object entry : list) {
                result.add(deepCopy(entry));
            }

            return result;
        }

        return value;
    }

    private static @NotNull Map<String, Object> deepCopyMap(@NotNull Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : source.entrySet()) {
            result.put(entry.getKey(), deepCopy(entry.getValue()));
        }

        return result;
    }
}