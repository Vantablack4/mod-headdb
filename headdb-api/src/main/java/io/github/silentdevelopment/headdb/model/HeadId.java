package io.github.silentdevelopment.headdb.model;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record HeadId(String value) {

    public HeadId {
        Objects.requireNonNull(value, "value");

        value = value.trim().toLowerCase(Locale.ROOT);

        if (value.isEmpty()) {
            throw new IllegalArgumentException("Head ID cannot be empty.");
        }

        int separator = value.indexOf(':');
        if (separator <= 0) {
            throw new IllegalArgumentException("Head ID must be canonical. Use remote:<id>, custom:<id>, or player:<uuid>.");
        }

        String source = value.substring(0, separator);
        String key = value.substring(separator + 1);

        if (key.isEmpty()) {
            throw new IllegalArgumentException("Head ID key cannot be empty.");
        }

        HeadSource.fromId(source);
    }

    public static HeadId remote(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Remote head ID must be positive.");
        }

        return new HeadId(HeadSource.REMOTE.id() + ":" + id);
    }

    public static HeadId remote(String id) {
        return new HeadId(HeadSource.REMOTE.id() + ":" + requireKey(id, "Remote head ID"));
    }

    public static HeadId custom(String id) {
        return new HeadId(HeadSource.CUSTOM.id() + ":" + requireKey(id, "Custom head ID"));
    }

    public static HeadId player(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        return new HeadId(HeadSource.PLAYER.id() + ":" + uuid);
    }

    public HeadSource source() {
        return HeadSource.fromId(value.substring(0, value.indexOf(':')));
    }

    public String key() {
        return value.substring(value.indexOf(':') + 1);
    }

    public boolean isRemote() {
        return source() == HeadSource.REMOTE;
    }

    public boolean isCustom() {
        return source() == HeadSource.CUSTOM;
    }

    public boolean isPlayer() {
        return source() == HeadSource.PLAYER;
    }

    public String display() {
        if (isRemote()) {
            return key();
        }

        return value;
    }

    @Override
    public @NotNull String toString() {
        return value;
    }

    private static String requireKey(String value, String name) {
        Objects.requireNonNull(value, "value");

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be empty.");
        }

        return normalized;
    }

    private static boolean isPositiveInteger(String value) {
        if (value.startsWith("0")) {
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