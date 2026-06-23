package io.github.silentdevelopment.headdb.catalog;

import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadCategory;
import io.github.silentdevelopment.headdb.model.HeadCollection;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.model.HeadTag;
import io.github.silentdevelopment.headdb.query.HeadQuery;
import io.github.silentdevelopment.headdb.query.HeadQueryResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public interface HeadCatalog {

    @NotNull Optional<Head> find(@NotNull HeadId id);

    @NotNull Optional<Head> find(@NotNull String input);

    @NotNull HeadQueryResult search(@NotNull HeadQuery query);

    @NotNull List<HeadCategory> categories();

    @NotNull List<HeadTag> tags();

    @NotNull List<HeadCollection> collections();

}