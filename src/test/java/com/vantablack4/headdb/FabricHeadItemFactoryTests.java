package com.vantablack4.headdb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;

import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.model.HeadTexture;
import org.junit.jupiter.api.Test;

final class FabricHeadItemFactoryTests {
    @Test
    void createsBase64TexturePayloadFromHash() {
        String value = FabricHeadItemFactory.textureValue("abcdef123456");
        String decoded = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);

        assertThat(decoded).isEqualTo("{\"textures\":{\"SKIN\":{\"url\":\"https://textures.minecraft.net/texture/abcdef123456\"}}}");
    }

    @Test
    void normalizesTextureHashes() {
        assertThat(FabricHeadItemFactory.normalizedTextureHash(" ABCDEF123456 ")).isEqualTo("abcdef123456");
    }

    @Test
    void rejectsInvalidTextureHashes() {
        assertThatThrownBy(() -> FabricHeadItemFactory.normalizedTextureHash("not/a/hash"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("invalid character");
    }

    @Test
    void buildsDeterministicGameProfiles() {
        Head head = new Head(
            HeadId.remote(12),
            "Stone",
            new HeadTexture("abcdef123456"),
            "blocks",
            Set.of("stone"),
            Set.of()
        );

        var first = FabricHeadItemFactory.profile(head, head.texture().hash());
        var second = FabricHeadItemFactory.profile(head, head.texture().hash());

        assertThat(first.id()).isEqualTo(second.id());
        assertThat(first.name()).isEqualTo("remote_12");
        assertThat(first.properties().get("textures")).singleElement()
            .extracting(property -> new String(Base64.getDecoder().decode(property.value()), StandardCharsets.UTF_8))
            .isEqualTo("{\"textures\":{\"SKIN\":{\"url\":\"https://textures.minecraft.net/texture/abcdef123456\"}}}");
    }
}
