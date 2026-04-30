package com.twily.skinwardrobe.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.twily.skinwardrobe.SkinWardrobe;
import com.twily.skinwardrobe.skin.SkinModel;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ClientWardrobeSettings {
    private static boolean loaded;
    private static SkinModel model = SkinModel.CLASSIC;

    private ClientWardrobeSettings() {
    }

    public static SkinModel model() {
        load();
        return model;
    }

    public static void setModel(SkinModel value) {
        model = value;
        loaded = true;
        save();
    }

    private static Path path() {
        return LocalSkinScanner.wardrobeDirectory().resolve("settings.json");
    }

    private static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path path = path();
        if (!Files.exists(path)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            if (json.has("model")) {
                model = SkinModel.parseOrClassic(json.get("model").getAsString());
            }
        } catch (Exception e) {
            SkinWardrobe.LOGGER.warn("Could not load Skin Wardrobe client settings", e);
        }
    }

    private static void save() {
        Path path = path();
        try {
            Files.createDirectories(path.getParent());
            JsonObject json = new JsonObject();
            json.addProperty("model", model.id());
            try (Writer writer = Files.newBufferedWriter(path)) {
                writer.write(json.toString());
            }
        } catch (IOException e) {
            SkinWardrobe.LOGGER.warn("Could not save Skin Wardrobe client settings", e);
        }
    }
}
