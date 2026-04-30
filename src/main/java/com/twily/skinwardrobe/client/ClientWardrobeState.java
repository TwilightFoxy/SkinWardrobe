package com.twily.skinwardrobe.client;

import com.google.gson.Gson;
import com.twily.skinwardrobe.storage.PlayerWardrobe;

public final class ClientWardrobeState {
    private static final Gson GSON = new Gson();
    private static PlayerWardrobe wardrobe = new PlayerWardrobe();
    private static String lastMessage = "";

    private ClientWardrobeState() {
    }

    public static PlayerWardrobe wardrobe() {
        return wardrobe;
    }

    public static String lastMessage() {
        return lastMessage;
    }

    public static void update(String json, String message) {
        PlayerWardrobe parsed = GSON.fromJson(json, PlayerWardrobe.class);
        wardrobe = parsed == null ? new PlayerWardrobe() : parsed;
        lastMessage = message == null ? "" : message;
    }
}
