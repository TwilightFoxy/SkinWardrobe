package com.twily.skinwardrobe.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.twily.skinwardrobe.skin.PlayerSkinService;
import com.twily.skinwardrobe.skin.SignedSkin;
import com.twily.skinwardrobe.skin.SkinModel;
import com.twily.skinwardrobe.storage.WardrobeEntry;
import com.twily.skinwardrobe.storage.WardrobeStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class SkinWardrobeNetwork {
    private static final Gson GSON = new Gson();

    private SkinWardrobeNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1").optional();
        registrar.playToServer(SkinWardrobeCommandPayload.TYPE, SkinWardrobeCommandPayload.STREAM_CODEC, SkinWardrobeNetwork::handleCommand);
        registrar.playToClient(WardrobeSyncPayload.TYPE, WardrobeSyncPayload.STREAM_CODEC);
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new WardrobeSyncPayload(WardrobeStorage.toClientJson(player.level().getServer(), player.getUUID()), ""));
    }

    private static void handleCommand(SkinWardrobeCommandPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        try {
            switch (payload.action()) {
                case "request_sync" -> sync(player);
                case "set_url" -> {
                    JsonObject json = parse(payload.json());
                    PlayerSkinService.setUrl(
                            player,
                            string(json, "url"),
                            SkinModel.parseOrClassic(string(json, "model", "classic")),
                            bool(json, "save"),
                            string(json, "name", "Saved skin"));
                }
                case "set_signed" -> {
                    JsonObject json = parse(payload.json());
                    String name = WardrobeStorage.validateName(string(json, "name", "Local skin"));
                    SkinModel model = SkinModel.parseOrClassic(string(json, "model", "classic"));
                    SignedSkin signedSkin = new SignedSkin(string(json, "value"), string(json, "signature"));
                    WardrobeEntry entry = new WardrobeEntry(name, model, string(json, "sourceType", "local"), string(json, "source", name), signedSkin);
                    PlayerSkinService.setSigned(player, entry, bool(json, "save"));
                }
                case "use" -> PlayerSkinService.useSaved(player, string(parse(payload.json()), "name"));
                case "delete" -> PlayerSkinService.deleteSaved(player, string(parse(payload.json()), "name"));
                case "reset" -> PlayerSkinService.reset(player);
                default -> player.sendSystemMessage(Component.translatable("skinwardrobe.error.bad_action"));
            }
        } catch (IllegalArgumentException e) {
            player.sendSystemMessage(Component.translatable("skinwardrobe.error.request", e.getMessage()));
        }
    }

    public static String json(Object object) {
        return GSON.toJson(object);
    }

    private static JsonObject parse(String json) {
        return JsonParser.parseString(json == null || json.isBlank() ? "{}" : json).getAsJsonObject();
    }

    private static String string(JsonObject json, String key) {
        return string(json, key, "");
    }

    private static String string(JsonObject json, String key, String fallback) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : fallback;
    }

    private static boolean bool(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() && json.get(key).getAsBoolean();
    }
}
