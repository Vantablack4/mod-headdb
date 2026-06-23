package io.github.silentdevelopment.headdb.core.database;

import io.github.silentdevelopment.headdb.database.DatabaseSource;
import io.github.silentdevelopment.headdb.database.DatabaseStats;
import io.github.silentdevelopment.headdb.database.DatabaseStatus;
import io.github.silentdevelopment.headdb.database.HeadDatabase;
import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadCategory;
import io.github.silentdevelopment.headdb.model.HeadCollection;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.model.HeadSource;
import io.github.silentdevelopment.headdb.model.HeadTag;
import io.github.silentdevelopment.headdb.query.HeadQuery;
import io.github.silentdevelopment.headdb.query.HeadQueryResult;
import io.github.silentdevelopment.headdb.query.HeadSort;
import io.github.silentdevelopment.headdb.query.SortDirection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

public final class DefaultHeadDatabase implements HeadDatabase {

    private final AtomicReference<DatabaseSnapshot> snapshot;
    private final AtomicReference<DatabaseStatus> status;

    public DefaultHeadDatabase() {
        this.snapshot = new AtomicReference<>();
        this.status = new AtomicReference<>(DatabaseStatus.notLoaded());
    }

    public DefaultHeadDatabase(@NotNull DatabaseSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        this.snapshot = new AtomicReference<>(snapshot);
        this.status = new AtomicReference<>(snapshot.status());
    }

    public void markLoading(@NotNull DatabaseSource source) {
        Objects.requireNonNull(source, "source");
        this.status.set(DatabaseStatus.loading(source));
    }

    public void markFailed(@NotNull String error) {
        Objects.requireNonNull(error, "error");

        DatabaseSnapshot current = snapshot.get();
        if (current == null) {
            this.status.set(DatabaseStatus.failed(error));
            return;
        }

        this.status.set(current.status().withLastError(error));
    }

    public void replace(@NotNull DatabaseSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        this.snapshot.set(snapshot);
        this.status.set(snapshot.status());
    }

    @Override
    public @NotNull DatabaseStatus status() {
        return status.get();
    }

    @Override
    public @NotNull DatabaseStats stats() {
        DatabaseSnapshot current = snapshot.get();
        if (current == null) {
            return DatabaseStats.empty();
        }

        return current.stats();
    }


    public @NotNull List<Head> heads() {
        DatabaseSnapshot current = snapshot.get();
        if (current == null) {
            return List.of();
        }

        return current.heads();
    }

    @Override
    public @NotNull Optional<Head> findById(@NotNull HeadId id) {
        Objects.requireNonNull(id, "id");

        DatabaseSnapshot current = snapshot.get();
        if (current == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(current.headIndex().get(id));
    }

    @Override
    public @NotNull Optional<HeadCategory> category(@NotNull String id) {
        Objects.requireNonNull(id, "id");

        DatabaseSnapshot current = snapshot.get();
        if (current == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(current.categoryIndex().get(normalizeId(id)));
    }

    @Override
    public @NotNull Optional<HeadTag> tag(@NotNull String id) {
        Objects.requireNonNull(id, "id");

        DatabaseSnapshot current = snapshot.get();
        if (current == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(current.tagIndex().get(normalizeId(id)));
    }

    @Override
    public @NotNull Optional<HeadCollection> collection(@NotNull String id) {
        Objects.requireNonNull(id, "id");

        DatabaseSnapshot current = snapshot.get();
        if (current == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(current.collectionIndex().get(normalizeId(id)));
    }

    @Override
    public @NotNull List<HeadCategory> categories() {
        DatabaseSnapshot current = snapshot.get();
        if (current == null) {
            return List.of();
        }

        return current.categories();
    }

    @Override
    public @NotNull List<HeadTag> tags() {
        DatabaseSnapshot current = snapshot.get();
        if (current == null) {
            return List.of();
        }

        return current.tags();
    }

    @Override
    public @NotNull List<HeadCollection> collections() {
        DatabaseSnapshot current = snapshot.get();
        if (current == null) {
            return List.of();
        }

        return current.collections();
    }

    @Override
    public @NotNull HeadQueryResult search(@NotNull HeadQuery query) {
        Objects.requireNonNull(query, "query");

        DatabaseSnapshot current = snapshot.get();
        if (current == null) {
            return new HeadQueryResult(List.of(), 0, query.offset(), query.limit());
        }

        List<Head> matches = filter(current, query).sorted(comparator(query)).toList();
        int from = Math.min(query.offset(), matches.size());
        int to = Math.min(from + query.limit(), matches.size());

        return new HeadQueryResult(matches.subList(from, to), matches.size(), query.offset(), query.limit());
    }

    private @NotNull Stream<Head> filter(@NotNull DatabaseSnapshot snapshot, @NotNull HeadQuery query) {
        Stream<Head> stream = snapshot.heads().stream();

        if (query.source() != null) {
            HeadSource source = query.source();
            stream = stream.filter(head -> head.id().source() == source);
        }

        if (!query.ids().isEmpty()) {
            Set<HeadId> ids = query.ids();
            stream = stream.filter(head -> ids.contains(head.id()));
        }

        if (!query.categories().isEmpty()) {
            Set<String> categories = query.categories();
            stream = stream.filter(head -> categories.contains(head.category()));
        }

        if (!query.tags().isEmpty()) {
            stream = stream.filter(head -> head.tags().containsAll(query.tags()));
        }

        if (!query.collections().isEmpty()) {
            stream = stream.filter(head -> head.collections().containsAll(query.collections()));
        }

        if (!query.text().isBlank()) {
            String text = query.text().toLowerCase(Locale.ROOT);
            stream = stream.filter(head -> matchesText(head, text));
        }

        return stream;
    }

    private boolean matchesText(@NotNull Head head, @NotNull String text) {
        if (head.name().toLowerCase(Locale.ROOT).contains(text)) {
            return true;
        }

        if (head.id().value().contains(text)) {
            return true;
        }

        if (head.texture().hash().contains(text)) {
            return true;
        }

        if (head.category().contains(text)) {
            return true;
        }

        for (String tag : head.tags()) {
            if (tag.contains(text)) {
                return true;
            }
        }

        for (String collection : head.collections()) {
            if (collection.contains(text)) {
                return true;
            }
        }

        return false;
    }

    private @NotNull Comparator<Head> comparator(@NotNull HeadQuery query) {
        if (query.sort() == HeadSort.RELEVANCE) {
            return relevanceComparator(query);
        }

        Comparator<Head> comparator = switch (query.sort()) {
            case ID -> idComparator();
            case NAME -> Comparator.comparing(head -> head.name().toLowerCase(Locale.ROOT));
            case CATEGORY -> Comparator.comparing(Head::category).thenComparing(idComparator());
            default -> throw new IllegalStateException("Unsupported head sort: " + query.sort());
        };

        if (query.direction() == SortDirection.DESCENDING) {
            return comparator.reversed();
        }

        return comparator;
    }

    private @NotNull Comparator<Head> relevanceComparator(@NotNull HeadQuery query) {
        if (query.text().isBlank()) {
            return idComparator();
        }

        String text = query.text().toLowerCase(Locale.ROOT);
        return Comparator.comparingInt((Head head) -> relevanceScore(head, text)).reversed().thenComparing(idComparator());
    }

    private int relevanceScore(@NotNull Head head, @NotNull String text) {
        String name = head.name().toLowerCase(Locale.ROOT);

        if (name.equals(text)) {
            return 100;
        }

        if (name.startsWith(text)) {
            return 75;
        }

        if (name.contains(text)) {
            return 50;
        }

        if (head.category().equals(text)) {
            return 40;
        }

        if (head.tags().contains(text)) {
            return 35;
        }

        if (head.collections().contains(text)) {
            return 30;
        }

        if (head.id().value().contains(text)) {
            return 20;
        }

        if (head.texture().hash().contains(text)) {
            return 10;
        }

        return 0;
    }

    private @NotNull Comparator<Head> idComparator() {
        return Comparator.comparing((Head head) -> head.id().source().ordinal()).thenComparing(head -> comparableIdKey(head.id()));
    }

    private @NotNull ComparableIdKey comparableIdKey(@NotNull HeadId id) {
        if (id.source() == HeadSource.REMOTE && isNumeric(id.key())) {
            return new ComparableIdKey(Long.parseLong(id.key()), "");
        }

        return new ComparableIdKey(Long.MAX_VALUE, id.key());
    }

    private boolean isNumeric(@NotNull String value) {
        if (value.isEmpty()) {
            return false;
        }

        for (int index = 0; index < value.length(); index++) {
            if (!Character.isDigit(value.charAt(index))) {
                return false;
            }
        }

        return true;
    }

    private @NotNull String normalizeId(@NotNull String id) {
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("ID cannot be empty.");
        }

        return normalized;
    }

    private record ComparableIdKey(long number, @NotNull String text) implements Comparable<ComparableIdKey> {

        private ComparableIdKey {
            Objects.requireNonNull(text, "text");
        }

        @Override
        public int compareTo(@NotNull ComparableIdKey other) {
            int numberCompare = Long.compare(number, other.number);
            if (numberCompare != 0) {
                return numberCompare;
            }

            return text.compareTo(other.text);
        }
    }
}