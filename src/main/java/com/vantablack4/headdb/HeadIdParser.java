package com.vantablack4.headdb;

import java.util.Locale;

import io.github.silentdevelopment.headdb.model.HeadId;

public final class HeadIdParser {
    private HeadIdParser() {
    }

    public static HeadId remote(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Head ID cannot be empty.");
        }
        if (value.startsWith("remote:")) {
            value = value.substring("remote:".length()).trim();
        }
        if (!isPositiveInteger(value)) {
            throw new IllegalArgumentException("Remote head ID must be a positive number.");
        }
        return HeadId.remote(value);
    }

    private static boolean isPositiveInteger(String value) {
        if (value.isEmpty() || value.startsWith("0")) {
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
