package com.twily.skinwardrobe.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.twily.skinwardrobe.SkinWardrobe;
import com.twily.skinwardrobe.skin.SkinImageValidator;
import com.twily.skinwardrobe.skin.SkinModel;
import com.twily.skinwardrobe.skin.SkinRequestException;
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

    public static Path wardrobeDirectory() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("skinwardrobe").toAbsolutePath().normalize();
    }

    private static Path legacySkinsDirectory() {
        return wardrobeDirectory().resolve("skins");
    }

    public static Path skinsDirectory() {
        return wardrobeDirectory();
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
            loadFrom(directory, result);
            loadFrom(legacySkinsDirectory(), result);
            return result;
        } catch (IOException e) {
            SkinWardrobe.LOGGER.warn("Could not scan local skin folder {}", directory, e);
            return List.of();
        }
    }

    public static Path saveDownloaded(String name, byte[] bytes) throws IOException {
        try {
            SkinImageValidator.validate(bytes);
        } catch (SkinRequestException e) {
            throw new IOException(e.getMessage(), e);
        }
        ensureSkinsDirectory();
        String baseName = fileName(name);
        Path directory = skinsDirectory();
        Path path = directory.resolve(baseName + ".png");
        int index = 2;
        while (Files.exists(path)) {
            path = directory.resolve(baseName + "_" + index + ".png");
            index++;
        }
        Files.write(path, bytes);
        return path;
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

    private static void loadFrom(Path directory, List<LocalSkin> result) throws IOException {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (var stream = Files.list(directory)) {
            for (Path path : stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .toList()) {
                load(path, result);
            }
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

    private static String fileName(String value) {
        String sanitized = value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
        sanitized = sanitized.replaceAll("_+", "_").replaceAll("^_+|_+$", "");
        if (sanitized.isBlank()) {
            return "skin";
        }
        return sanitized.length() > 48 ? sanitized.substring(0, 48) : sanitized;
    }

    public record LocalSkin(Path path, String name, Identifier textureId, byte[] bytes, SkinModel model) {
    }
}
