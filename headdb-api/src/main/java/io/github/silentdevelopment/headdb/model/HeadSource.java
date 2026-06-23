package io.github.silentdevelopment.headdb.model;

public enum HeadSource {

    REMOTE("remote"),
    CUSTOM("custom"),
    PLAYER("player");

    private final String id;

    HeadSource(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static HeadSource fromId(String id) {
        for (HeadSource source : values()) {
            if (source.id.equals(id)) {
                return source;
            }
        }

        throw new IllegalArgumentException("Unsupported head source: " + id);
    }
}