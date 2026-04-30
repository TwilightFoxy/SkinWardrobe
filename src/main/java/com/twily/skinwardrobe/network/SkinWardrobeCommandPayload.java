package com.twily.skinwardrobe.network;

import com.twily.skinwardrobe.SkinWardrobe;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SkinWardrobeCommandPayload(String action, String json) implements CustomPacketPayload {
    public static final Type<SkinWardrobeCommandPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SkinWardrobe.MOD_ID, "command"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SkinWardrobeCommandPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            SkinWardrobeCommandPayload::action,
            ByteBufCodecs.STRING_UTF8,
            SkinWardrobeCommandPayload::json,
            SkinWardrobeCommandPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
