package com.vantablack4.headdb;

import java.io.IOException;
import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.silentdevelopment.headdb.core.compression.ZstdArtifactDecoder;
import io.github.silentdevelopment.headdb.core.database.DatabaseSnapshot;
import io.github.silentdevelopment.headdb.core.database.DefaultHeadDatabase;
import io.github.silentdevelopment.headdb.core.database.cache.FileDatabaseArtifactCache;
import io.github.silentdevelopment.headdb.core.database.parse.GsonCatalogIndexParser;
import io.github.silentdevelopment.headdb.core.database.parse.GsonCatalogPartParser;
import io.github.silentdevelopment.headdb.core.database.parse.GsonRevocationPartParser;
import io.github.silentdevelopment.headdb.core.database.parse.GsonRevocationsIndexParser;
import io.github.silentdevelopment.headdb.core.database.refresh.DatabaseRefreshService;
import io.github.silentdevelopment.headdb.core.hash.Sha256Verifier;
import io.github.silentdevelopment.headdb.core.remote.ArtifactSelector;
import io.github.silentdevelopment.headdb.core.remote.http.JdkRemoteHttpClient;
import io.github.silentdevelopment.headdb.core.remote.parse.GsonRemoteManifestParser;
import io.github.silentdevelopment.headdb.database.DatabaseStatus;
import io.github.silentdevelopment.headdb.database.HeadDatabase;
import org.slf4j.Logger;

public final class HeadDbDatabaseService implements AutoCloseable {
    private final HeadDbConfig config;
    private final Logger logger;
    private final DefaultHeadDatabase database;
    private final DatabaseRefreshService refreshService;
    private final ExecutorService executor;

    private HeadDbDatabaseService(
        HeadDbConfig config,
        Logger logger,
        DefaultHeadDatabase database,
        DatabaseRefreshService refreshService,
        ExecutorService executor
    ) {
        this.config = Objects.requireNonNull(config, "config");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.database = Objects.requireNonNull(database, "database");
        this.refreshService = Objects.requireNonNull(refreshService, "refreshService");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    public static HeadDbDatabaseService create(HeadDbConfig config, Logger logger) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(logger, "logger");

        DefaultHeadDatabase database = new DefaultHeadDatabase();
        GsonRemoteManifestParser manifestParser = new GsonRemoteManifestParser();
        ArtifactSelector artifactSelector = new ArtifactSelector(config.preferredMirrorId());
        ZstdArtifactDecoder zstdDecoder = new ZstdArtifactDecoder();
        GsonCatalogIndexParser catalogIndexParser = new GsonCatalogIndexParser();
        GsonCatalogPartParser catalogPartParser = new GsonCatalogPartParser();
        GsonRevocationsIndexParser revocationsIndexParser = new GsonRevocationsIndexParser();
        GsonRevocationPartParser revocationPartParser = new GsonRevocationPartParser();
        FileDatabaseArtifactCache artifactCache = new FileDatabaseArtifactCache(
            config.cacheDirectory(),
            manifestParser,
            artifactSelector,
            zstdDecoder,
            catalogIndexParser,
            revocationsIndexParser
        );
        DatabaseRefreshService refreshService = new DatabaseRefreshService(
            database,
            config.manifestUri(),
            new JdkRemoteHttpClient(),
            manifestParser,
            artifactSelector,
            new Sha256Verifier(),
            zstdDecoder,
            catalogIndexParser,
            catalogPartParser,
            revocationsIndexParser,
            revocationPartParser,
            Clock.systemUTC(),
            artifactCache
        );
        ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "mod-headdb-refresh");
            thread.setDaemon(true);
            return thread;
        });
        return new HeadDbDatabaseService(config, logger, database, refreshService, executor);
    }

    public void start() {
        CompletableFuture.runAsync(() -> {
            if (config.loadCachedOnStartup()) {
                loadCached();
            }
            if (config.refreshOnStartup()) {
                refreshBlocking();
            }
        }, executor);
    }

    public HeadDatabase database() {
        return database;
    }

    public DatabaseStatus status() {
        return database.status();
    }

    public CompletableFuture<DatabaseSnapshot> refreshAsync() {
        return CompletableFuture.supplyAsync(this::refreshBlocking, executor);
    }

    public CompletableFuture<DatabaseSnapshot> verifyAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return refreshService.verifyRemote();
            } catch (IOException exception) {
                throw new HeadDbOperationException("Remote verification failed: " + message(exception), exception);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new HeadDbOperationException("Remote verification was interrupted.", exception);
            }
        }, executor);
    }

    private void loadCached() {
        try {
            if (refreshService.loadCached()) {
                logger.info("Loaded HeadDB cache with {} heads", database.stats().heads());
            } else {
                logger.info("No HeadDB cache is available yet");
            }
        } catch (IOException | RuntimeException exception) {
            logger.warn("Unable to load HeadDB cache", exception);
        }
    }

    private DatabaseSnapshot refreshBlocking() {
        try {
            DatabaseSnapshot snapshot = refreshService.refresh();
            logger.info("Refreshed HeadDB remote catalog with {} heads", snapshot.stats().heads());
            return snapshot;
        } catch (IOException exception) {
            throw new HeadDbOperationException("Remote refresh failed: " + message(exception), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new HeadDbOperationException("Remote refresh was interrupted.", exception);
        }
    }

    private static String message(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return throwable.getClass().getSimpleName();
        }
        return message;
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
