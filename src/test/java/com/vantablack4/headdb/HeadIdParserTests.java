package com.vantablack4.headdb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.silentdevelopment.headdb.model.HeadId;
import org.junit.jupiter.api.Test;

final class HeadIdParserTests {
    @Test
    void parsesBareRemoteIds() {
        assertThat(HeadIdParser.remote("123")).isEqualTo(HeadId.remote(123));
    }

    @Test
    void parsesCanonicalRemoteIds() {
        assertThat(HeadIdParser.remote("remote:456")).isEqualTo(HeadId.remote(456));
    }

    @Test
    void rejectsNonRemoteIds() {
        assertThatThrownBy(() -> HeadIdParser.remote("custom:test"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("positive number");
    }
}
