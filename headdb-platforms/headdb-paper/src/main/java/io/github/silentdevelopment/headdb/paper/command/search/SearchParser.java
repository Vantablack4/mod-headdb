package io.github.silentdevelopment.headdb.paper.command.search;

import io.github.silentdevelopment.headdb.model.HeadId;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class SearchParser {

    private static final String REMOTE_PREFIX = "remote:";
    private static final String CUSTOM_PREFIX = "custom:";
    private static final String PLAYER_PREFIX = "player:";

    private SearchParser() {
    }

    public static @NotNull String singleId(@NotNull String raw, @NotNull String name) {
        String value = raw.trim();

        if (value.isEmpty()) {
            throw new IllegalArgumentException("Search " + name + " cannot be empty.");
        }

        if (value.contains(",")) {
            throw new IllegalArgumentException("Search " + name + " must contain exactly one ID.");
        }

        return value;
    }

    public static @NotNull Set<String> idList(@NotNull String raw, @NotNull String name) {
        Set<String> values = new LinkedHashSet<>();

        for (String token : raw.split(",")) {
            String value = token.trim();

            if (value.isEmpty()) {
                throw new IllegalArgumentException("Search " + name + " contains an empty ID.");
            }

            values.add(value);
        }

        return Set.copyOf(values);
    }

    public static @NotNull Set<HeadId> headIds(@NotNull String raw) {
        Set<HeadId> ids = new LinkedHashSet<>();

        for (String token : raw.split(",")) {
            String value = token.trim();

            if (value.isEmpty()) {
                throw new IllegalArgumentException("Search ids contains an empty ID.");
            }

            ids.add(headId(value));
        }

        return Set.copyOf(ids);
    }

    public static @NotNull HeadId headId(@NotNull String raw) {
        String value = raw.trim();

        if (value.isEmpty()) {
            throw new IllegalArgumentException("Head ID cannot be empty.");
        }

        if (startsWithPrefix(value, REMOTE_PREFIX)) {
            return remoteHeadId(value.substring(REMOTE_PREFIX.length()));
        }

        if (startsWithPrefix(value, CUSTOM_PREFIX)) {
            return customHeadId(value.substring(CUSTOM_PREFIX.length()));
        }

        if (startsWithPrefix(value, PLAYER_PREFIX)) {
            return playerHeadId(value.substring(PLAYER_PREFIX.length()));
        }

        if (looksPrefixed(value)) {
            throw new IllegalArgumentException("Unknown head ID prefix in '" + raw + "'. Use remote:<id>, custom:<id>, player:<name|uuid>, or a bare remote ID.");
        }

        return remoteHeadId(value);
    }

    private static @NotNull HeadId remoteHeadId(@NotNull String raw) {
        String value = raw.trim();

        if (value.isEmpty()) {
            throw new IllegalArgumentException("Remote head ID cannot be empty.");
        }

        if (!isUnsignedInteger(value)) {
            throw new IllegalArgumentException("Invalid remote head ID '" + raw + "'. Remote IDs must be numeric.");
        }

        try {
            return HeadId.remote(Integer.parseInt(value));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid remote head ID '" + raw + "'. Remote ID is too large.", exception);
        }
    }

    private static @NotNull HeadId customHeadId(@NotNull String raw) {
        String value = raw.trim();

        if (value.isEmpty()) {
            throw new IllegalArgumentException("Custom head ID cannot be empty.");
        }

        return HeadId.custom(value);
    }

    private static @NotNull HeadId playerHeadId(@NotNull String raw) {
        String value = raw.trim();

        if (value.isEmpty()) {
            throw new IllegalArgumentException("Player head name or UUID cannot be empty.");
        }

        try {
            return HeadId.player(UUID.fromString(value));
        } catch (IllegalArgumentException exception) {
            return new HeadId("player:" + value);
        }
    }

    private static boolean startsWithPrefix(@NotNull String value, @NotNull String prefix) {
        return value.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    private static boolean looksPrefixed(@NotNull String value) {
        int separator = value.indexOf(':');

        if (separator <= 0) {
            return false;
        }

        for (int index = 0; index < separator; index++) {
            char character = value.charAt(index);

            if (character >= 'a' && character <= 'z') {
                continue;
            }

            if (character >= 'A' && character <= 'Z') {
                continue;
            }

            return false;
        }

        return true;
    }

    private static boolean isUnsignedInteger(@NotNull String value) {
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
}