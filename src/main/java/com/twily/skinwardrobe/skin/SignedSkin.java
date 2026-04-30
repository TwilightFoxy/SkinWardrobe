package com.twily.skinwardrobe.skin;

public record SignedSkin(String value, String signature) {
    public boolean isComplete() {
        return this.value != null && !this.value.isBlank() && this.signature != null && !this.signature.isBlank();
    }
}
