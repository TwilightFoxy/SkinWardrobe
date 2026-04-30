package com.twily.skinwardrobe.network;

import com.twily.skinwardrobe.SkinWardrobe;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record WardrobeSyncPayload(String json, String message) implements CustomPacketPayload {
    public static final Type<WardrobeSyncPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SkinWardrobe.MOD_ID, "sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WardrobeSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            WardrobeSyncPayload::json,
            ByteBufCodecs.STRING_UTF8,
            WardrobeSyncPayload::message,
            WardrobeSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
