package com.vantablack4.headdb;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class HeadDbTargetPinTests {
    @Test
    void pinsInventoryGrantsToTheAuditedCharacterAndGatewaySession() {
        UUID player = UUID.randomUUID();
        UUID account = UUID.randomUUID();
        UUID character = UUID.randomUUID();
        Instant selectedAt = Instant.parse("2026-07-13T00:00:00Z");
        var pinned = new HeadDbCommands.PinnedTarget(player, account, character, 11, selectedAt, "Target");

        assertThat(pinned.matches(player, account, character, 11, selectedAt)).isTrue();
        assertThat(pinned.matches(player, account, UUID.randomUUID(), 11, selectedAt)).isFalse();
        assertThat(pinned.matches(player, account, character, 12, selectedAt)).isFalse();
        assertThat(pinned.matches(player, account, character, 11, selectedAt.plusSeconds(1))).isFalse();
    }
}
