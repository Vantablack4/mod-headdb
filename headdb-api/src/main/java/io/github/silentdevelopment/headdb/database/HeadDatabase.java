package io.github.silentdevelopment.headdb.database;

import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadCategory;
import io.github.silentdevelopment.headdb.model.HeadCollection;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.model.HeadTag;
import io.github.silentdevelopment.headdb.query.HeadQuery;
import io.github.silentdevelopment.headdb.query.HeadQueryResult;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public interface HeadDatabase {

    @NotNull DatabaseStatus status();

    @NotNull DatabaseStats stats();

    @NotNull Optional<Head> findById(@NotNull HeadId id);

    @NotNull Optional<HeadCategory> category(@NotNull String id);

    @NotNull Optional<HeadTag> tag(@NotNull String id);

    @NotNull Optional<HeadCollection> collection(@NotNull String id);

    @NotNull List<HeadCategory> categories();

    @NotNull List<HeadTag> tags();

    @NotNull List<HeadCollection> collections();

    @NotNull HeadQueryResult search(@NotNull HeadQuery query);

}