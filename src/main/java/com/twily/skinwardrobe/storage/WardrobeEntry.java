package com.twily.skinwardrobe.storage;

import com.twily.skinwardrobe.skin.SignedSkin;
import com.twily.skinwardrobe.skin.SkinModel;

public final class WardrobeEntry {
    public String name;
    public String model;
    public String sourceType;
    public String source;
    public String value;
    public String signature;
    public long createdAt;
    public long updatedAt;

    public WardrobeEntry() {
    }

    public WardrobeEntry(String name, SkinModel model, String sourceType, String source, SignedSkin signedSkin) {
        long now = System.currentTimeMillis();
        this.name = name;
        this.model = model.id();
        this.sourceType = sourceType;
        this.source = source;
        this.value = signedSkin.value();
        this.signature = signedSkin.signature();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public SkinModel skinModel() {
        return SkinModel.parseOrClassic(this.model == null ? "classic" : this.model);
    }

    public SignedSkin signedSkin() {
        return new SignedSkin(this.value, this.signature);
    }

    public WardrobeEntry copyAsActive() {
        WardrobeEntry copy = new WardrobeEntry();
        copy.name = this.name;
        copy.model = this.model;
        copy.sourceType = this.sourceType;
        copy.source = this.source;
        copy.value = this.value;
        copy.signature = this.signature;
        copy.createdAt = this.createdAt;
        copy.updatedAt = System.currentTimeMillis();
        return copy;
    }
}
