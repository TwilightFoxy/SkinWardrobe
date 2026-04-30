package com.twily.skinwardrobe.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.twily.skinwardrobe.SkinWardrobe;
import com.twily.skinwardrobe.network.WardrobeSyncPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

@EventBusSubscriber(modid = SkinWardrobe.MOD_ID, value = Dist.CLIENT)
public final class SkinWardrobeClient {
    private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath(SkinWardrobe.MOD_ID, "main"));
    private static final KeyMapping OPEN_WARDROBE = new KeyMapping(
            "key.skinwardrobe.open",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_O,
            CATEGORY);

    private SkinWardrobeClient() {
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(OPEN_WARDROBE);
    }

    @SubscribeEvent
    public static void registerClientPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(WardrobeSyncPayload.TYPE, (payload, context) -> {
            ClientWardrobeState.update(payload.json(), payload.message());
            if (Minecraft.getInstance().screen instanceof WardrobeScreen screen) {
                screen.refreshFromServer();
            }
        });
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (OPEN_WARDROBE.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                minecraft.setScreen(new WardrobeScreen(null));
            }
        }
    }
}
