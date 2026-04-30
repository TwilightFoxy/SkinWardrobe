package com.twily.skinwardrobe.mixin.client;

import net.minecraft.client.renderer.texture.SkinTextureDownloader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SkinTextureDownloader.class)
public abstract class SkinTextureDownloaderMixin {
    @ModifyVariable(method = "downloadAndRegisterSkin", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String skinwardrobe$preferHttpsForMinecraftTextures(String url) {
        if (url != null && url.startsWith("http://textures.minecraft.net/")) {
            return "https://" + url.substring("http://".length());
        }
        return url;
    }
}
