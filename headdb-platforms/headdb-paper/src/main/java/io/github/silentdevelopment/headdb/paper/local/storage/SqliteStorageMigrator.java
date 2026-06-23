package io.github.silentdevelopment.headdb.paper.local.storage;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public final class SqliteStorageMigrator {

    private static final int LATEST_SCHEMA_VERSION = 1;
    private static final int BUSY_TIMEOUT_MILLIS = 5000;

    private SqliteStorageMigrator() {
    }

    public static void migrate(@NotNull Path databaseFile) {
        Objects.requireNonNull(databaseFile, "databaseFile");

        Path normalized = databaseFile.toAbsolutePath().normalize();
        createParentDirectory(normalized);

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + normalized)) {
            configureConnection(connection);
            connection.setAutoCommit(false);

            try {
                createSchemaTable(connection);
                int currentVersion = currentVersion(connection);

                if (currentVersion > LATEST_SCHEMA_VERSION) {
                    throw new IllegalStateException("Local storage schema version " + currentVersion + " is newer than supported version " + LATEST_SCHEMA_VERSION + ". Update HeadDB before using this database.");
                }

                if (currentVersion < LATEST_SCHEMA_VERSION) {
                    migrate(connection, currentVersion, LATEST_SCHEMA_VERSION);
                    setVersion(connection, LATEST_SCHEMA_VERSION);
                }

                connection.commit();
            } catch (Exception exception) {
                rollback(connection);
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to validate local storage schema at " + normalized + ".", exception);
        }
    }

    private static void createParentDirectory(@NotNull Path databaseFile) {
        try {
            Path parent = databaseFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to create local storage directory for " + databaseFile + ".", exception);
        }
    }

    private static void configureConnection(@NotNull Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("PRAGMA busy_timeout = " + BUSY_TIMEOUT_MILLIS)) {
            statement.execute();
        }

        try (PreparedStatement statement = connection.prepareStatement("PRAGMA foreign_keys = ON")) {
            statement.execute();
        }
    }

    private static void createSchemaTable(@NotNull Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS headdb_schema (id INTEGER PRIMARY KEY CHECK (id = 1), version INTEGER NOT NULL)")) {
            statement.executeUpdate();
        }
    }

    private static int currentVersion(@NotNull Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT version FROM headdb_schema WHERE id = 1")) {
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getInt("version");
                }
            }
        }

        initializeVersion(connection);
        return LATEST_SCHEMA_VERSION;
    }

    private static void initializeVersion(@NotNull Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO headdb_schema(id, version) VALUES (1, ?)")) {
            statement.setInt(1, LATEST_SCHEMA_VERSION);
            statement.executeUpdate();
        }
    }

    private static void migrate(@NotNull Connection connection, int currentVersion, int latestVersion) throws SQLException {
        int version = currentVersion;

        while (version < latestVersion) {
            int nextVersion = version + 1;
            migrateTo(connection, nextVersion);
            version = nextVersion;
        }
    }

    private static void migrateTo(@NotNull Connection connection, int version) throws SQLException {
        switch (version) {
            default -> throw new IllegalStateException("No local storage migration is registered for schema version " + version + ".");
        }
    }

    private static void setVersion(@NotNull Connection connection, int version) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE headdb_schema SET version = ? WHERE id = 1")) {
            statement.setInt(1, version);
            statement.executeUpdate();
        }
    }

    private static void rollback(@NotNull Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // Preserve the original migration failure.
        }
    }
}