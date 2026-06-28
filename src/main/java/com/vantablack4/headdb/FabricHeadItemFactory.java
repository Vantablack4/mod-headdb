package com.vantablack4.headdb;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import com.google.common.collect.LinkedHashMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import io.github.silentdevelopment.headdb.model.Head;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;

public final class FabricHeadItemFactory {
    private static final String TEXTURE_BASE_URL = "https://textures.minecraft.net/texture/";
    private static final int MAX_AMOUNT = 64;

    public ItemStack remoteHead(Head head, int amount) {
        Objects.requireNonNull(head, "head");
        int count = clampAmount(amount);
        String textureHash = normalizedTextureHash(head.texture().hash());
        ItemStack stack = new ItemStack(Items.PLAYER_HEAD, count);
        stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile(head, textureHash)));
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(head.name()).withStyle(ChatFormatting.GOLD));
        stack.set(DataComponents.LORE, new ItemLore(lore(head)));
        stack.set(DataComponents.CUSTOM_DATA, marker(head));
        return stack;
    }

    public ItemStack playerHead(String playerNameOrUuid, int amount) {
        String value = Objects.requireNonNull(playerNameOrUuid, "playerNameOrUuid").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Player name or UUID cannot be empty.");
        }

        ResolvableProfile profile;
        try {
            profile = ResolvableProfile.createUnresolved(UUID.fromString(value));
        } catch (IllegalArgumentException ignored) {
            profile = ResolvableProfile.createUnresolved(value);
        }

        ItemStack stack = new ItemStack(Items.PLAYER_HEAD, clampAmount(amount));
        stack.set(DataComponents.PROFILE, profile);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(value + "'s Head").withStyle(ChatFormatting.GOLD));
        return stack;
    }

    static GameProfile profile(Head head, String textureHash) {
        LinkedHashMultimap<String, Property> properties = LinkedHashMultimap.create();
        properties.put("textures", new Property("textures", textureValue(textureHash)));
        return new GameProfile(profileId(head), profileName(head), new PropertyMap(properties));
    }

    private static UUID profileId(Head head) {
        return UUID.nameUUIDFromBytes(("vantablack-headdb:" + head.id()).getBytes(StandardCharsets.UTF_8));
    }

    private static String profileName(Head head) {
        String raw = head.id().source().name().toLowerCase(Locale.ROOT) + "_" + head.id().key();
        String sanitized = raw.replaceAll("[^A-Za-z0-9_]", "_");
        if (sanitized.isBlank()) {
            return "HeadDB";
        }
        if (sanitized.length() > 16) {
            return sanitized.substring(0, 16);
        }
        return sanitized;
    }

    static String textureValue(String textureHash) {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + TEXTURE_BASE_URL + textureHash + "\"}}}";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    static String normalizedTextureHash(String raw) {
        String textureHash = Objects.requireNonNull(raw, "textureHash").trim().toLowerCase(Locale.ROOT);
        if (textureHash.isEmpty()) {
            throw new IllegalArgumentException("Head texture hash cannot be empty.");
        }
        for (int index = 0; index < textureHash.length(); index++) {
            char character = textureHash.charAt(index);
            if ((character >= '0' && character <= '9') || (character >= 'a' && character <= 'f')) {
                continue;
            }
            throw new IllegalArgumentException("Head texture hash contains an invalid character.");
        }
        return textureHash;
    }

    private static CustomData marker(Head head) {
        CompoundTag tag = new CompoundTag();
        tag.putString("VantablackHeadDbId", head.id().toString());
        return CustomData.of(tag);
    }

    private static List<Component> lore(Head head) {
        List<Component> lines = new ArrayList<>();
        lines.add(line("ID", head.id().display()));
        lines.add(line("Category", head.category()));
        if (!head.tags().isEmpty()) {
            lines.add(line("Tags", String.join(", ", head.tags())));
        }
        return List.copyOf(lines);
    }

    private static Component line(String label, String value) {
        return Component.literal(label + ": ")
            .withStyle(ChatFormatting.GRAY)
            .append(Component.literal(value).withStyle(ChatFormatting.WHITE));
    }

    private static int clampAmount(int amount) {
        if (amount < 1 || amount > MAX_AMOUNT) {
            throw new IllegalArgumentException("Amount must be between 1 and " + MAX_AMOUNT + ".");
        }
        return amount;
    }
}
