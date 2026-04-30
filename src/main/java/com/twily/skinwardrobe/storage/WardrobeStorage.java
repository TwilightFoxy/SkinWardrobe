package com.twily.skinwardrobe.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.twily.skinwardrobe.SkinWardrobe;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

public final class WardrobeStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path loadedPath;
    private static Data data = new Data();

    private WardrobeStorage() {
    }

    public static synchronized PlayerWardrobe get(MinecraftServer server, UUID playerId) {
        ensureLoaded(server);
        return data.players.computeIfAbsent(playerId.toString(), ignored -> new PlayerWardrobe());
    }

    public static synchronized void save(MinecraftServer server) {
        ensureLoaded(server);
        try {
            Files.createDirectories(loadedPath.getParent());
            try (Writer writer = Files.newBufferedWriter(loadedPath)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            SkinWardrobe.LOGGER.error("Could not save wardrobe data", e);
        }
    }

    public static synchronized String toClientJson(MinecraftServer server, UUID playerId) {
        return GSON.toJson(get(server, playerId));
    }

    public static String key(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    public static String validateName(String name) {
        String trimmed = name.trim();
        if (trimmed.isEmpty() || trimmed.length() > 32) {
            throw new IllegalArgumentException("Name must be 1-32 characters.");
        }
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            boolean ok = Character.isLetterOrDigit(c) || c == ' ' || c == '_' || c == '-' || c == '.';
            if (!ok) {
                throw new IllegalArgumentException("Name may contain only letters, numbers, spaces, _, - and .");
            }
        }
        return trimmed;
    }

    private static void ensureLoaded(MinecraftServer server) {
        Path path = server.getWorldPath(LevelResource.ROOT).resolve("skinwardrobe").resolve("wardrobes.json");
        if (path.equals(loadedPath)) {
            return;
        }

        loadedPath = path;
        data = new Data();
        if (!Files.exists(path)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            Data loaded = GSON.fromJson(reader, Data.class);
            if (loaded != null && loaded.players != null) {
                data = loaded;
            }
        } catch (IOException e) {
            SkinWardrobe.LOGGER.error("Could not load wardrobe data", e);
        }
    }

    private static final class Data {
        Map<String, PlayerWardrobe> players = new LinkedHashMap<>();
    }
}
