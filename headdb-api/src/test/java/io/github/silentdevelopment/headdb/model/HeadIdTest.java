package io.github.silentdevelopment.headdb.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class HeadIdTest {

    @Test
    void createsCanonicalRemoteId() {
        HeadId id = HeadId.remote(123);

        assertEquals("remote:123", id.value());
        assertEquals(HeadSource.REMOTE, id.source());
        assertEquals("123", id.key());
        assertEquals("123", id.display());
    }

    @Test
    void createsRemoteIdFromInteger() {
        HeadId id = HeadId.remote(123);

        assertEquals("remote:123", id.value());
        assertEquals(HeadSource.REMOTE, id.source());
        assertEquals("123", id.key());
    }

    @Test
    void createsCustomId() {
        HeadId id = HeadId.custom("Spawn-Dragon");

        assertEquals("custom:spawn-dragon", id.value());
        assertEquals(HeadSource.CUSTOM, id.source());
        assertEquals("spawn-dragon", id.key());
        assertEquals("custom:spawn-dragon", id.display());
    }

    @Test
    void createsPlayerId() {
        UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        HeadId id = HeadId.player(uuid);

        assertEquals("player:550e8400-e29b-41d4-a716-446655440000", id.value());
        assertEquals(HeadSource.PLAYER, id.source());
        assertEquals("550e8400-e29b-41d4-a716-446655440000", id.key());
    }

    @Test
    void normalizesCanonicalInput() {
        HeadId id = new HeadId(" REMOTE:123 ");

        assertEquals("remote:123", id.value());
        assertEquals(HeadSource.REMOTE, id.source());
    }

    @Test
    void rejectsEmptyId() {
        assertThrows(IllegalArgumentException.class, () -> new HeadId(""));
        assertThrows(IllegalArgumentException.class, () -> new HeadId("   "));
    }

    @Test
    void rejectsNonCanonicalId() {
        assertThrows(IllegalArgumentException.class, () -> new HeadId("123"));
        assertThrows(IllegalArgumentException.class, () -> new HeadId("spawn-dragon"));
    }

    @Test
    void rejectsUnsupportedSource() {
        assertThrows(IllegalArgumentException.class, () -> new HeadId("other:123"));
    }

    @Test
    void rejectsMissingKey() {
        assertThrows(IllegalArgumentException.class, () -> new HeadId("remote:"));
    }

    @Test
    void rejectsNonPositiveRemoteInteger() {
        assertThrows(IllegalArgumentException.class, () -> HeadId.remote(0));
        assertThrows(IllegalArgumentException.class, () -> HeadId.remote(-1));
    }

}