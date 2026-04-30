package com.twily.skinwardrobe.skin;

import java.util.Locale;
import java.util.Optional;

public enum SkinModel {
    CLASSIC("classic"),
    SLIM("slim");

    private final String id;

    SkinModel(String id) {
        this.id = id;
    }

    public String id() {
        return this.id;
    }

    public static Optional<SkinModel> parse(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "classic", "wide", "default" -> Optional.of(CLASSIC);
            case "slim", "alex" -> Optional.of(SLIM);
            default -> Optional.empty();
        };
    }

    public static SkinModel parseOrClassic(String value) {
        return parse(value).orElse(CLASSIC);
    }
}
