package io.github.silentdevelopment.headdb.registry;

import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadCategory;
import io.github.silentdevelopment.headdb.model.HeadCollection;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.model.HeadTag;
import io.github.silentdevelopment.headdb.query.HeadQuery;
import io.github.silentdevelopment.headdb.query.HeadQueryResult;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Effective HeadDB registry exposed to other plugins.
 *
 * <p>The registry resolves the effective server view of HeadDB heads. Remote heads are
 * read from the immutable verified remote catalog and then merged with local metadata
 * overrides. Custom heads are server-local durable entries. Player heads are resolved
 * explicitly and may require asynchronous profile/cache work.</p>
 */
public interface HeadRegistry {

    /**
     * Finds a head without performing external player profile lookup.
     *
     * @param id head id
     * @return cached/effective head if available
     */
    @NotNull Optional<Head> find(@NotNull HeadId id);

    /**
     * Resolves a head. For {@code player:*} ids this may perform asynchronous profile/cache work.
     *
     * @param id head id
     * @return resolved head, or empty if unavailable
     */
    @NotNull CompletionStage<Optional<Head>> resolve(@NotNull HeadId id);

    /**
     * Searches effective remote heads and custom heads. Arbitrary player heads are not included
     * in normal search results.
     *
     * @param query query
     * @return query result
     */
    @NotNull HeadQueryResult search(@NotNull HeadQuery query);

    @NotNull Collection<HeadCategory> categories();

    @NotNull Collection<HeadTag> tags();

    @NotNull Collection<HeadCollection> collections();
}
