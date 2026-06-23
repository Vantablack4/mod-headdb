package io.github.silentdevelopment.headdb.core.database.cache;

import io.github.silentdevelopment.headdb.core.compression.ZstdArtifactDecoder;
import io.github.silentdevelopment.headdb.core.database.parse.GsonCatalogIndexParser;
import io.github.silentdevelopment.headdb.core.database.parse.GsonRevocationsIndexParser;
import io.github.silentdevelopment.headdb.core.database.parse.ParsedCatalogIndex;
import io.github.silentdevelopment.headdb.core.database.parse.ParsedRevocationsIndex;
import io.github.silentdevelopment.headdb.core.remote.ArtifactSelection;
import io.github.silentdevelopment.headdb.core.remote.ArtifactSelector;
import io.github.silentdevelopment.headdb.core.remote.RemoteArtifact;
import io.github.silentdevelopment.headdb.core.remote.RemoteManifest;
import io.github.silentdevelopment.headdb.core.remote.RemoteResourceId;
import io.github.silentdevelopment.headdb.core.remote.parse.RemoteManifestParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public final class FileDatabaseArtifactCache implements DatabaseArtifactCache {

    private static final String MANIFEST_FILE = "manifest.json";
    private static final String LEGACY_STAGING_DIRECTORY = ".staging";
    private static final String TEMP_SUFFIX = ".tmp";
    private static final String BACKUP_SUFFIX = ".backup";

    private final Path directory;
    private final RemoteManifestParser manifestParser;
    private final ArtifactSelector artifactSelector;
    private final ZstdArtifactDecoder zstdDecoder;
    private final GsonCatalogIndexParser catalogIndexParser;
    private final GsonRevocationsIndexParser revocationsIndexParser;

    public FileDatabaseArtifactCache(@NotNull Path directory, @NotNull RemoteManifestParser manifestParser, @NotNull ArtifactSelector artifactSelector, @NotNull ZstdArtifactDecoder zstdDecoder, @NotNull GsonCatalogIndexParser catalogIndexParser, @NotNull GsonRevocationsIndexParser revocationsIndexParser) {
        this.directory = Objects.requireNonNull(directory, "directory");
        this.manifestParser = Objects.requireNonNull(manifestParser, "manifestParser");
        this.artifactSelector = Objects.requireNonNull(artifactSelector, "artifactSelector");
        this.zstdDecoder = Objects.requireNonNull(zstdDecoder, "zstdDecoder");
        this.catalogIndexParser = Objects.requireNonNull(catalogIndexParser, "catalogIndexParser");
        this.revocationsIndexParser = Objects.requireNonNull(revocationsIndexParser, "revocationsIndexParser");
    }

    @Override
    public @NotNull Optional<CachedDatabaseArtifacts> load() throws IOException {
        recoverTransientDirectories();
        if (!Files.isRegularFile(directory.resolve(MANIFEST_FILE))) {
            return Optional.empty();
        }
        return Optional.of(loadFrom(directory));
    }

    @Override
    public void save(@NotNull CachedDatabaseArtifacts artifacts) throws IOException {
        Objects.requireNonNull(artifacts, "artifacts");
        recoverTransientDirectories();

        Path temporary = sibling(TEMP_SUFFIX);
        Path backup = sibling(BACKUP_SUFFIX);

        deleteRecursively(temporary);
        deleteRecursively(backup);
        Files.createDirectories(temporary);

        writeAll(temporary, artifacts);
        loadFrom(temporary);

        boolean previousMoved = false;
        try {
            if (Files.exists(directory)) {
                moveReplacing(directory, backup);
                previousMoved = true;
            }

            moveReplacing(temporary, directory);
            tryDeleteRecursively(backup);
        } catch (IOException exception) {
            deleteRecursively(temporary);
            if (previousMoved && !Files.exists(directory) && Files.exists(backup)) {
                moveReplacing(backup, directory);
            }
            throw exception;
        }
    }

    private @NotNull CachedDatabaseArtifacts loadFrom(@NotNull Path base) throws IOException {
        String manifestJson = Files.readString(base.resolve(MANIFEST_FILE));
        RemoteManifest manifest = manifestParser.parse(manifestJson);
        CachedArtifact catalogIndex = readSelected(base, artifactSelector.selectCatalogIndex(manifest));
        ParsedCatalogIndex parsedCatalogIndex = catalogIndexParser.parse(decode(catalogIndex.selection().artifact(), catalogIndex.bytes()));
        List<CachedArtifact> catalogParts = readParts(base, manifest, RemoteResourceId.CATALOG, parsedCatalogIndex.artifacts());
        CachedArtifact revocationsIndex = readSelected(base, artifactSelector.selectRevocationsIndex(manifest));
        ParsedRevocationsIndex parsedRevocationsIndex = revocationsIndexParser.parse(decode(revocationsIndex.selection().artifact(), revocationsIndex.bytes()));
        List<CachedArtifact> revocationParts = readParts(base, manifest, RemoteResourceId.REVOCATIONS, parsedRevocationsIndex.artifacts());
        return new CachedDatabaseArtifacts(manifestJson, manifest, catalogIndex, catalogParts, revocationsIndex, revocationParts);
    }

    private void writeAll(@NotNull Path base, @NotNull CachedDatabaseArtifacts artifacts) throws IOException {
        Files.writeString(base.resolve(MANIFEST_FILE), artifacts.manifestJson());
        write(base, artifacts.catalogIndex());
        for (CachedArtifact part : artifacts.catalogParts()) {
            write(base, part);
        }
        write(base, artifacts.revocationsIndex());
        for (CachedArtifact part : artifacts.revocationParts()) {
            write(base, part);
        }
    }

    private @NotNull CachedArtifact readSelected(@NotNull Path base, @NotNull ArtifactSelection selection) throws IOException {
        Path path = resolveUnder(base, selection.artifact().path());
        if (!Files.isRegularFile(path)) {
            throw new IOException("Cached artifact is missing: " + selection.artifact().path());
        }
        return new CachedArtifact(selection, Files.readAllBytes(path));
    }

    private @NotNull List<CachedArtifact> readParts(@NotNull Path base, @NotNull RemoteManifest manifest, @NotNull String resourceId, @NotNull List<RemoteArtifact> parts) throws IOException {
        List<CachedArtifact> cached = new ArrayList<>();
        for (RemoteArtifact part : parts) {
            cached.add(readSelected(base, artifactSelector.selectPart(manifest, resourceId, part)));
        }
        return List.copyOf(cached);
    }

    private @NotNull String decode(@NotNull RemoteArtifact artifact, byte @NotNull [] bytes) {
        if (artifact.compression().equals(RemoteArtifact.COMPRESSION_NONE)) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return zstdDecoder.decodeString(bytes);
    }

    private void write(@NotNull Path base, @NotNull CachedArtifact artifact) throws IOException {
        Path target = resolveUnder(base, artifact.selection().artifact().path());
        Files.createDirectories(target.getParent());
        Files.write(target, artifact.bytes());
    }

    private void recoverTransientDirectories() throws IOException {
        Path temporary = sibling(TEMP_SUFFIX);
        Path backup = sibling(BACKUP_SUFFIX);

        deleteRecursively(temporary);
        deleteRecursively(directory.resolve(LEGACY_STAGING_DIRECTORY));

        if (Files.exists(directory)) {
            return;
        }

        if (!Files.exists(backup)) {
            return;
        }

        moveReplacing(backup, directory);
    }

    private @NotNull Path sibling(@NotNull String suffix) {
        Path fileName = directory.getFileName();
        if (fileName == null) {
            throw new IllegalStateException("Cache directory must have a file name.");
        }
        return directory.resolveSibling(fileName + suffix);
    }

    private static @NotNull Path resolveUnder(@NotNull Path base, @NotNull String relativePath) {
        RemoteArtifact.validatePath(relativePath);
        Path normalizedBase = base.toAbsolutePath().normalize();
        Path resolved = normalizedBase.resolve(relativePath).normalize();
        if (!resolved.startsWith(normalizedBase)) {
            throw new IllegalArgumentException("Cached artifact path escapes cache directory: " + relativePath);
        }
        return resolved;
    }

    private static void moveReplacing(@NotNull Path source, @NotNull Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void tryDeleteRecursively(@NotNull Path path) {
        try {
            deleteRecursively(path);
        } catch (IOException ignored) {
        }
    }

    private static void deleteRecursively(@NotNull Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }

        if (Files.isRegularFile(path)) {
            Files.delete(path);
            return;
        }

        try (var paths = Files.walk(path)) {
            for (Path current : paths.sorted(Comparator.comparingInt(Path::getNameCount).reversed()).toList()) {
                Files.deleteIfExists(current);
            }
        }
    }
}