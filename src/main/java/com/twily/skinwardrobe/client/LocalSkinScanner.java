package com.twily.skinwardrobe.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.twily.skinwardrobe.SkinWardrobe;
import com.twily.skinwardrobe.skin.SkinImageValidator;
import com.twily.skinwardrobe.skin.SkinModel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public final class LocalSkinScanner {
    private LocalSkinScanner() {
    }

    public static Path skinsDirectory() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("skinwardrobe").resolve("skins").toAbsolutePath().normalize();
    }

    public static void ensureSkinsDirectory() {
        Path directory = skinsDirectory();
        try {
            Files.createDirectories(directory);
            SkinWardrobe.LOGGER.info("Local Skin Wardrobe folder: {}", directory);
        } catch (IOException e) {
            SkinWardrobe.LOGGER.warn("Could not create local skin folder {}", directory, e);
        }
    }

    public static List<LocalSkin> scan() {
        Path directory = skinsDirectory();
        try {
            ensureSkinsDirectory();
            List<LocalSkin> result = new ArrayList<>();
            try (var stream = Files.list(directory)) {
                for (Path path : stream
                        .filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                        .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)))
                        .toList()) {
                    load(path, result);
                }
            }
            return result;
        } catch (IOException e) {
            SkinWardrobe.LOGGER.warn("Could not scan local skin folder {}", directory, e);
            return List.of();
        }
    }

    private static void load(Path path, List<LocalSkin> result) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            SkinImageValidator.Result validation = SkinImageValidator.validate(bytes);
            Identifier textureId = Identifier.fromNamespaceAndPath(
                    SkinWardrobe.MOD_ID,
                    "local/" + sanitize(path.getFileName().toString()) + "_" + Integer.toHexString(Arrays.hashCode(bytes)));
            NativeImage image = NativeImage.read(bytes);
            Minecraft.getInstance().getTextureManager().register(textureId, new DynamicTexture(() -> path.getFileName().toString(), image));
            result.add(new LocalSkin(path, displayName(path), textureId, bytes, validation.height() == 32 ? SkinModel.CLASSIC : SkinModel.CLASSIC));
        } catch (Exception e) {
            SkinWardrobe.LOGGER.warn("Skipping invalid local skin {}", path, e);
        }
    }

    private static String displayName(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static String sanitize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");
    }

    public record LocalSkin(Path path, String name, Identifier textureId, byte[] bytes, SkinModel model) {
    }
}
