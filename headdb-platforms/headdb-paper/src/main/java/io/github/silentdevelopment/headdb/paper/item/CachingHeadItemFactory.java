package io.github.silentdevelopment.headdb.paper.item;

import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadId;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class CachingHeadItemFactory implements HeadItemFactory {

    private static final int DEFAULT_MAX_SIZE = 4096;

    private final HeadItemFactory delegate;
    private final Map<HeadId, ItemStack> cache;

    public CachingHeadItemFactory(@NotNull HeadItemFactory delegate) {
        this(delegate, DEFAULT_MAX_SIZE);
    }

    public CachingHeadItemFactory(@NotNull HeadItemFactory delegate, int maxSize) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.cache = new LruItemCache(maxSize);
    }

    @Override
    public @NotNull ItemStack create(@NotNull Head head) {
        Objects.requireNonNull(head, "head");

        synchronized (cache) {
            ItemStack cached = cache.get(head.id());

            if (cached != null) {
                return cached.clone();
            }

            ItemStack created = delegate.create(head);
            ItemStack prototype = created.clone();

            cache.put(head.id(), prototype);

            return prototype.clone();
        }
    }

    public void clear() {
        synchronized (cache) {
            cache.clear();
        }
    }

    public int size() {
        synchronized (cache) {
            return cache.size();
        }
    }

    private static final class LruItemCache extends LinkedHashMap<HeadId, ItemStack> {

        private final int maxSize;

        private LruItemCache(int maxSize) {
            super(128, 0.75F, true);

            if (maxSize < 0) {
                throw new IllegalArgumentException("maxSize cannot be negative.");
            }

            this.maxSize = maxSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<HeadId, ItemStack> eldest) {
            if (maxSize == 0) {
                return false;
            }

            return size() > maxSize;
        }
    }
}