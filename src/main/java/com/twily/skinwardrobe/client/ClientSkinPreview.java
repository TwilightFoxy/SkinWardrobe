package com.twily.skinwardrobe.client;

import com.google.common.collect.ArrayListMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.twily.skinwardrobe.skin.SkinModel;
import com.twily.skinwardrobe.storage.WardrobeEntry;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

public final class ClientSkinPreview {
    private ClientSkinPreview() {
    }

    public static PlayerSkin localSkin(LocalSkinScanner.LocalSkin skin, SkinModel model) {
        return PlayerSkin.insecure(
                new TextureAsset(skin.textureId()),
                DefaultPlayerSkin.getDefaultSkin().cape(),
                DefaultPlayerSkin.getDefaultSkin().elytra(),
                modelType(model));
    }

    public static Supplier<PlayerSkin> saved(WardrobeEntry entry) {
        if (entry.value == null || entry.value.isBlank()) {
            return DefaultPlayerSkin::getDefaultSkin;
        }
        var properties = ArrayListMultimap.<String, Property>create();
        properties.put("textures", new Property("textures", entry.value, entry.signature));
        String seed = "skinwardrobe:" + entry.name + ":" + entry.value;
        UUID id = UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
        GameProfile profile = new GameProfile(id, safeProfileName(entry.name), new PropertyMap(properties));
        return Minecraft.getInstance().getSkinManager().createLookup(profile, false);
    }

    private static PlayerModelType modelType(SkinModel model) {
        return model == SkinModel.SLIM ? PlayerModelType.SLIM : PlayerModelType.WIDE;
    }

    private static String safeProfileName(String name) {
        String sanitized = name == null ? "SkinWardrobe" : name.replaceAll("[^A-Za-z0-9_]", "_");
        if (sanitized.isBlank()) {
            return "SkinWardrobe";
        }
        return sanitized.length() > 16 ? sanitized.substring(0, 16) : sanitized;
    }

    private record TextureAsset(Identifier id) implements ClientAsset.Texture {
        @Override
        public Identifier texturePath() {
            return id;
        }
    }
}
