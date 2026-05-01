package com.twily.skinwardrobe.skin;

import com.google.common.collect.ArrayListMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.twily.skinwardrobe.SkinWardrobe;
import com.twily.skinwardrobe.network.SkinWardrobeNetwork;
import com.twily.skinwardrobe.storage.PlayerWardrobe;
import com.twily.skinwardrobe.storage.WardrobeEntry;
import com.twily.skinwardrobe.storage.WardrobeStorage;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerPlayer;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

public final class PlayerSkinService {
    private static final String DEFAULT_TEXTURES_KEY = "skinwardrobe-default_textures";
    private static final Map<UUID, Long> LAST_SIGNED_APPLY = new ConcurrentHashMap<>();
    private static final long RATE_LIMIT_MILLIS = 3_000L;
    private static final long LOGIN_APPLY_DELAY_MILLIS = 500L;
    private static final Field GAME_PROFILE_FIELD = findGameProfileField();
    private static final Field CHUNK_MAP_ENTITY_MAP = findField(ChunkMap.class, "entityMap");
    private static final Field TRACKED_ENTITY_SERVER_ENTITY = findTrackedEntityServerEntityField();

    private PlayerSkinService() {
    }

    public static void setUrl(ServerPlayer player, String url, SkinModel model, boolean save, String requestedName) {
        if (!checkRateLimit(player)) {
            return;
        }

        player.sendSystemMessage(Component.translatable("skinwardrobe.status.working"));
        UUID playerId = player.getUUID();
        MinecraftServer server = player.level().getServer();
        SkinDownloader.downloadPng(url)
                .thenCompose(bytes -> MineSkinClient.sign(bytes, model))
                .whenComplete((signedSkin, throwable) -> server.execute(() -> {
                    ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(playerId);
                    if (onlinePlayer == null) {
                        return;
                    }
                    if (throwable != null) {
                        onlinePlayer.sendSystemMessage(Component.translatable("skinwardrobe.error.request", MineSkinClient.unwrap(throwable).getMessage()));
                        return;
                    }
                    String name = save ? safeName(requestedName) : "URL skin";
                    WardrobeEntry entry = new WardrobeEntry(name, model, "url", url, signedSkin);
                    applyEntry(onlinePlayer, entry, save);
                    onlinePlayer.sendSystemMessage(Component.translatable("skinwardrobe.status.applied"));
                }));
    }

    public static void setSigned(ServerPlayer player, WardrobeEntry entry, boolean save) {
        if (!checkRateLimit(player)) {
            return;
        }
        applyEntry(player, entry, save);
        player.sendSystemMessage(Component.translatable("skinwardrobe.status.applied"));
    }

    public static void useSaved(ServerPlayer player, String name) {
        PlayerWardrobe wardrobe = WardrobeStorage.get(player.level().getServer(), player.getUUID());
        WardrobeEntry entry = wardrobe.entries.get(WardrobeStorage.key(name));
        if (entry == null) {
            player.sendSystemMessage(Component.translatable("skinwardrobe.error.not_found", name));
            return;
        }
        applyEntry(player, entry.copyAsActive(), false);
        player.sendSystemMessage(Component.translatable("skinwardrobe.status.applied"));
    }

    public static void deleteSaved(ServerPlayer player, String name) {
        PlayerWardrobe wardrobe = WardrobeStorage.get(player.level().getServer(), player.getUUID());
        WardrobeEntry removed = wardrobe.entries.remove(WardrobeStorage.key(name));
        if (removed == null) {
            player.sendSystemMessage(Component.translatable("skinwardrobe.error.not_found", name));
            return;
        }
        WardrobeStorage.save(player.level().getServer());
        SkinWardrobeNetwork.sync(player);
        player.sendSystemMessage(Component.translatable("skinwardrobe.status.deleted", removed.name));
    }

    public static void reset(ServerPlayer player) {
        PlayerWardrobe wardrobe = WardrobeStorage.get(player.level().getServer(), player.getUUID());
        wardrobe.active = null;
        WardrobeStorage.save(player.level().getServer());
        applySignedSkin(player, null);
        SkinWardrobeNetwork.sync(player);
        player.sendSystemMessage(Component.translatable("skinwardrobe.status.reset"));
    }

    public static void prepareActiveForLogin(ServerPlayer player) {
        PlayerWardrobe wardrobe = WardrobeStorage.get(player.level().getServer(), player.getUUID());
        if (wardrobe.active == null || !wardrobe.active.signedSkin().isComplete()) {
            return;
        }

        applySignedSkin(player, wardrobe.active.signedSkin(), false);
    }

    public static void applyActiveOnLogin(ServerPlayer player) {
        PlayerWardrobe wardrobe = WardrobeStorage.get(player.level().getServer(), player.getUUID());
        if (wardrobe.active == null || !wardrobe.active.signedSkin().isComplete()) {
            return;
        }

        MinecraftServer server = player.level().getServer();
        UUID playerId = player.getUUID();
        CompletableFuture.delayedExecutor(LOGIN_APPLY_DELAY_MILLIS, TimeUnit.MILLISECONDS).execute(() ->
                server.executeIfPossible(() -> {
                    ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(playerId);
                    if (onlinePlayer != null) {
                        refreshPlayer(onlinePlayer);
                        SkinWardrobeNetwork.sync(onlinePlayer);
                    }
                }));
    }

    public static void sendCurrent(ServerPlayer player) {
        PlayerWardrobe wardrobe = WardrobeStorage.get(player.level().getServer(), player.getUUID());
        if (wardrobe.active == null) {
            player.sendSystemMessage(Component.translatable("skinwardrobe.current.none"));
        } else {
            player.sendSystemMessage(Component.translatable(
                    "skinwardrobe.current.active",
                    wardrobe.active.name,
                    wardrobe.active.model,
                    wardrobe.entries.size()));
        }
    }

    public static void sendList(ServerPlayer player) {
        PlayerWardrobe wardrobe = WardrobeStorage.get(player.level().getServer(), player.getUUID());
        if (wardrobe.entries.isEmpty()) {
            player.sendSystemMessage(Component.translatable("skinwardrobe.list.empty"));
            return;
        }
        player.sendSystemMessage(Component.translatable("skinwardrobe.list.header", wardrobe.entries.size()));
        for (WardrobeEntry entry : wardrobe.entries.values()) {
            player.sendSystemMessage(Component.literal("- " + entry.name + " [" + entry.model + "]"));
        }
    }

    private static void applyEntry(ServerPlayer player, WardrobeEntry entry, boolean save) {
        PlayerWardrobe wardrobe = WardrobeStorage.get(player.level().getServer(), player.getUUID());
        WardrobeEntry active = entry.copyAsActive();
        wardrobe.active = active;
        if (save) {
            String validated = WardrobeStorage.validateName(entry.name);
            entry.name = validated;
            entry.updatedAt = System.currentTimeMillis();
            wardrobe.entries.put(WardrobeStorage.key(validated), entry);
        }
        WardrobeStorage.save(player.level().getServer());
        applySignedSkin(player, active.signedSkin());
        SkinWardrobeNetwork.sync(player);
    }

    private static void applySignedSkin(ServerPlayer player, SignedSkin signedSkin) {
        applySignedSkin(player, signedSkin, true);
    }

    private static void applySignedSkin(ServerPlayer player, SignedSkin signedSkin, boolean refresh) {
        var properties = ArrayListMultimap.create(player.getGameProfile().properties());
        if (!properties.containsKey(DEFAULT_TEXTURES_KEY) && properties.containsKey("textures")) {
            Property original = properties.get("textures").stream().findFirst().orElse(null);
            if (original != null) {
                properties.put(DEFAULT_TEXTURES_KEY, new Property(DEFAULT_TEXTURES_KEY, original.value(), original.signature()));
            }
        }

        properties.removeAll("textures");
        if (signedSkin != null && signedSkin.isComplete()) {
            properties.put("textures", new Property("textures", signedSkin.value(), signedSkin.signature()));
        } else {
            properties.get(DEFAULT_TEXTURES_KEY).stream().findFirst()
                    .ifPresent(original -> properties.put("textures", new Property("textures", original.value(), original.signature())));
        }

        setProfile(player, new GameProfile(player.getUUID(), player.getGameProfile().name(), new PropertyMap(properties)));
        if (refresh) {
            refreshPlayer(player);
        }
    }

    private static void refreshPlayer(ServerPlayer player) {
        for (ServerPlayer other : player.level().getServer().getPlayerList().getPlayers()) {
            other.connection.send(new ClientboundPlayerInfoRemovePacket(Collections.singletonList(player.getUUID())));
            other.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(Collections.singleton(player)));
        }

        if (player.level().getChunkSource() instanceof ServerChunkCache chunkCache) {
            chunkCache.move(player);
            refreshEntityPairings(player, chunkCache);
        }
    }

    private static void refreshEntityPairings(ServerPlayer player, ServerChunkCache chunkCache) {
        if (CHUNK_MAP_ENTITY_MAP == null || TRACKED_ENTITY_SERVER_ENTITY == null) {
            return;
        }

        try {
            @SuppressWarnings("unchecked")
            Int2ObjectMap<Object> entityMap = (Int2ObjectMap<Object>) CHUNK_MAP_ENTITY_MAP.get(chunkCache.chunkMap);
            Object trackedEntity = entityMap.get(player.getId());
            if (trackedEntity == null) {
                return;
            }
            ServerEntity serverEntity = (ServerEntity) TRACKED_ENTITY_SERVER_ENTITY.get(trackedEntity);
            Set<ServerPlayer> watchers = new LinkedHashSet<>(chunkCache.chunkMap.getPlayersWatching(player));
            watchers.remove(player);
            for (ServerPlayer watcher : player.level().getServer().getPlayerList().getPlayers()) {
                if (watcher != player && watcher.level() == player.level() && player.broadcastToPlayer(watcher)) {
                    watchers.add(watcher);
                }
            }
            for (ServerPlayer watcher : watchers) {
                serverEntity.removePairing(watcher);
            }
            CompletableFuture.delayedExecutor(100L, TimeUnit.MILLISECONDS).execute(() ->
                    player.level().getServer().executeIfPossible(() -> {
                        for (ServerPlayer watcher : watchers) {
                            if (watcher.connection != null && watcher.level() == player.level() && player.broadcastToPlayer(watcher)) {
                                serverEntity.addPairing(watcher);
                            }
                        }
                    }));
        } catch (IllegalAccessException | ClassCastException e) {
            SkinWardrobe.LOGGER.warn("Could not refresh player entity pairing for skin update", e);
        }
    }

    private static boolean checkRateLimit(ServerPlayer player) {
        long now = System.currentTimeMillis();
        Long previous = LAST_SIGNED_APPLY.get(player.getUUID());
        if (previous != null && now - previous < RATE_LIMIT_MILLIS) {
            long seconds = Math.max(1L, (RATE_LIMIT_MILLIS - (now - previous) + 999L) / 1000L);
            player.sendSystemMessage(Component.translatable("skinwardrobe.error.rate_limit", seconds));
            return false;
        }
        LAST_SIGNED_APPLY.put(player.getUUID(), now);
        return true;
    }

    private static String safeName(String requestedName) {
        try {
            return WardrobeStorage.validateName(requestedName == null || requestedName.isBlank() ? "Saved skin" : requestedName);
        } catch (IllegalArgumentException e) {
            return "Saved skin";
        }
    }

    private static void setProfile(ServerPlayer player, GameProfile profile) {
        try {
            GAME_PROFILE_FIELD.set(player, profile);
        } catch (IllegalAccessException e) {
            SkinWardrobe.LOGGER.error("Could not update player GameProfile", e);
        }
    }

    private static Field findGameProfileField() {
        try {
            Field field = net.minecraft.world.entity.player.Player.class.getDeclaredField("gameProfile");
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("Could not find Player.gameProfile", e);
        }
    }

    private static Field findField(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            SkinWardrobe.LOGGER.warn("Could not find {}.{}", owner.getName(), name);
            return null;
        }
    }

    private static Field findTrackedEntityServerEntityField() {
        for (Class<?> nested : ChunkMap.class.getDeclaredClasses()) {
            for (Field field : nested.getDeclaredFields()) {
                if (field.getType() == ServerEntity.class) {
                    field.setAccessible(true);
                    return field;
                }
            }
        }
        SkinWardrobe.LOGGER.warn("Could not find ChunkMap tracked ServerEntity field");
        return null;
    }
}
