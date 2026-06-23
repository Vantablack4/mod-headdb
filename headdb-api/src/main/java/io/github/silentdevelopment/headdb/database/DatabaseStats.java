package io.github.silentdevelopment.headdb.database;

public record DatabaseStats(int heads, int categories, int tags, int collections, int revocations) {

    public DatabaseStats {
        if (heads < 0) {
            throw new IllegalArgumentException("Head count cannot be negative.");
        }

        if (categories < 0) {
            throw new IllegalArgumentException("Category count cannot be negative.");
        }

        if (tags < 0) {
            throw new IllegalArgumentException("Tag count cannot be negative.");
        }

        if (collections < 0) {
            throw new IllegalArgumentException("Collection count cannot be negative.");
        }

        if (revocations < 0) {
            throw new IllegalArgumentException("Revocation count cannot be negative.");
        }
    }

    public static DatabaseStats empty() {
        return new DatabaseStats(0, 0, 0, 0, 0);
    }

}