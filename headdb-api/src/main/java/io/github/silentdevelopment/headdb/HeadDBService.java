package io.github.silentdevelopment.headdb;

import io.github.silentdevelopment.headdb.category.CustomCategories;
import io.github.silentdevelopment.headdb.database.DatabaseStatus;
import io.github.silentdevelopment.headdb.database.HeadDatabase;
import io.github.silentdevelopment.headdb.favorite.HeadFavorites;
import io.github.silentdevelopment.headdb.head.CustomHeads;
import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.registry.HeadRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface HeadDBService {

    @NotNull HeadDatabase remoteDatabase();

    @NotNull HeadRegistry registry();

    @NotNull CustomHeads customHeads();

    @NotNull HeadFavorites favorites();

    @NotNull CustomCategories customCategories();

    default @NotNull DatabaseStatus status() {
        return remoteDatabase().status();
    }

    default @NotNull Optional<Head> find(@NotNull HeadId id) {
        return registry().find(id);
    }

    default @NotNull Optional<Head> find(@NotNull String input) {
        return registry().find(parseInputId(input));
    }

    default @NotNull CompletionStage<Optional<Head>> resolve(@NotNull HeadId id) {
        return registry().resolve(id);
    }

    default @NotNull CompletionStage<Optional<Head>> resolve(@NotNull String input) {
        return registry().resolve(parseInputId(input));
    }

    private static @NotNull HeadId parseInputId(@NotNull String input) {
        String value = input.trim();
        if (value.contains(":")) {
            return new HeadId(value);
        }

        return HeadId.remote(value);
    }

}