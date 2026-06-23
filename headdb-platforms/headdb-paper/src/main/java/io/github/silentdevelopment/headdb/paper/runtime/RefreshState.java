package io.github.silentdevelopment.headdb.paper.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class RefreshState {

    private final AtomicBoolean running;
    private final AtomicReference<Instant> lastSuccessfulRefresh;
    private final AtomicReference<Instant> lastFailedRefresh;
    private final AtomicReference<String> lastFailureMessage;

    public RefreshState() {
        this.running = new AtomicBoolean(false);
        this.lastSuccessfulRefresh = new AtomicReference<>();
        this.lastFailedRefresh = new AtomicReference<>();
        this.lastFailureMessage = new AtomicReference<>();
    }

    public boolean begin() {
        return running.compareAndSet(false, true);
    }

    public void markSuccess() {
        lastSuccessfulRefresh.set(Instant.now());
        lastFailureMessage.set(null);
        running.set(false);
    }

    public void markFailure(@NotNull Throwable throwable) {
        lastFailedRefresh.set(Instant.now());
        lastFailureMessage.set(failureMessage(throwable));
        running.set(false);
    }

    public boolean running() {
        return running.get();
    }

    public @Nullable Instant lastSuccessfulRefresh() {
        return lastSuccessfulRefresh.get();
    }

    public @Nullable Instant lastFailedRefresh() {
        return lastFailedRefresh.get();
    }

    public @Nullable String lastFailureMessage() {
        return lastFailureMessage.get();
    }

    public void finish() {
        running.set(false);
    }

    private @NotNull String failureMessage(@NotNull Throwable throwable) {
        if (throwable.getMessage() == null || throwable.getMessage().isBlank()) {
            return throwable.getClass().getSimpleName();
        }

        return throwable.getMessage();
    }
}