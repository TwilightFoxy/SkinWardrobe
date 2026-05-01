package com.twily.skinwardrobe.client;

import com.twily.skinwardrobe.SkinWardrobe;
import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;

public final class ClientSkinCacheRefresher {
    private static final long RETRY_DELAY_MILLIS = 250L;
    private static final Field ABSTRACT_CLIENT_PLAYER_INFO = findField(AbstractClientPlayer.class, PlayerInfo.class, "playerInfo");
    private static final Field PLAYER_INFO_SKIN_LOOKUP = findField(PlayerInfo.class, java.util.function.Supplier.class, "skinLookup");

    private ClientSkinCacheRefresher() {
    }

    public static void refreshAll() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() != null) {
            for (PlayerInfo info : minecraft.getConnection().getOnlinePlayers()) {
                setNull(PLAYER_INFO_SKIN_LOOKUP, info);
            }
        }
        if (minecraft.level != null) {
            for (AbstractClientPlayer player : minecraft.level.players()) {
                setNull(ABSTRACT_CLIENT_PLAYER_INFO, player);
            }
        }
        if (minecraft.player != null) {
            setNull(ABSTRACT_CLIENT_PLAYER_INFO, minecraft.player);
        }
    }

    public static void refreshAllWithRetry() {
        refreshAll();
        CompletableFuture.delayedExecutor(RETRY_DELAY_MILLIS, TimeUnit.MILLISECONDS).execute(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.execute(ClientSkinCacheRefresher::refreshAll);
        });
    }

    private static Field findField(Class<?> owner, Class<?> type, String fallbackName) {
        try {
            Field field = owner.getDeclaredField(fallbackName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException ignored) {
            for (Field field : owner.getDeclaredFields()) {
                if (field.getType() == type) {
                    field.setAccessible(true);
                    return field;
                }
            }
            SkinWardrobe.LOGGER.warn("Could not find {} field on {}", type.getSimpleName(), owner.getName());
            return null;
        }
    }

    private static void setNull(Field field, Object target) {
        if (field == null || target == null) {
            return;
        }
        try {
            field.set(target, null);
        } catch (IllegalAccessException e) {
            SkinWardrobe.LOGGER.warn("Could not clear client skin cache", e);
        }
    }
}
