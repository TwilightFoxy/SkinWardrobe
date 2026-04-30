package com.twily.skinwardrobe.storage;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PlayerWardrobe {
    public WardrobeEntry active;
    public Map<String, WardrobeEntry> entries = new LinkedHashMap<>();
}
