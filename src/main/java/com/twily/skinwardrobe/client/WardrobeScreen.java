package com.twily.skinwardrobe.client;

import com.google.gson.JsonObject;
import com.twily.skinwardrobe.network.SkinWardrobeCommandPayload;
import com.twily.skinwardrobe.skin.MineSkinClient;
import com.twily.skinwardrobe.skin.SignedSkin;
import com.twily.skinwardrobe.skin.SkinDownloader;
import com.twily.skinwardrobe.skin.SkinModel;
import com.twily.skinwardrobe.storage.WardrobeEntry;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.PlayerSkinWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerSkin;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.Nullable;

public final class WardrobeScreen extends Screen {
    private static final Component TITLE = Component.translatable("screen.skinwardrobe.title");
    private final @Nullable Screen parent;
    private final List<LocalSkinScanner.LocalSkin> localSkins = new ArrayList<>();
    private final List<GallerySkin> gallerySkins = new ArrayList<>();
    private EditBox urlBox;
    private EditBox nameBox;
    private SkinModel model = ClientWardrobeSettings.model();
    private int selectedIndex;
    private String status = "";
    private boolean syncRequested;

    public WardrobeScreen(@Nullable Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        String urlValue = this.urlBox == null ? "" : this.urlBox.getValue();
        String nameValue = this.nameBox == null ? "" : this.nameBox.getValue();
        boolean urlFocused = this.urlBox != null && this.urlBox.isFocused();
        boolean nameFocused = this.nameBox != null && this.nameBox.isFocused();

        this.localSkins.clear();
        this.localSkins.addAll(LocalSkinScanner.scan());
        rebuildGallery();
        clampSelection();
        this.clearWidgets();

        Layout layout = layout();
        int fieldWidth = layout.panelWidth - 198;

        this.urlBox = new EditBox(this.font, layout.left + 14, layout.top + 34, fieldWidth, 20, Component.translatable("screen.skinwardrobe.url"));
        this.urlBox.setMaxLength(2048);
        this.urlBox.setValue(urlValue);
        this.urlBox.setHint(Component.translatable("screen.skinwardrobe.url.hint"));
        this.addRenderableWidget(this.urlBox);

        this.nameBox = new EditBox(this.font, layout.left + 14, layout.top + 62, fieldWidth, 20, Component.translatable("screen.skinwardrobe.name"));
        this.nameBox.setMaxLength(32);
        this.nameBox.setValue(nameValue);
        this.nameBox.setHint(Component.translatable("screen.skinwardrobe.name.hint"));
        this.addRenderableWidget(this.nameBox);

        this.addRenderableWidget(CycleButton.builder(value -> Component.translatable("screen.skinwardrobe.model." + value.id()), this.model)
                .withValues(SkinModel.CLASSIC, SkinModel.SLIM)
                .create(layout.left + layout.panelWidth - 168, layout.top + 34, 154, 20, Component.translatable("screen.skinwardrobe.model"), (button, value) -> {
                    this.model = value;
                    ClientWardrobeSettings.setModel(value);
                    this.rebuildWidgets();
                }));

        this.addRenderableWidget(Button.builder(Component.translatable("screen.skinwardrobe.apply_url"), button -> applyUrl(false))
                .bounds(layout.left + layout.panelWidth - 168, layout.top + 62, 76, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("screen.skinwardrobe.save_url"), button -> applyUrl(true))
                .bounds(layout.left + layout.panelWidth - 88, layout.top + 62, 74, 20)
                .build());

        addCarouselWidgets(layout);

        int actionY = layout.bottom - 58;
        Button apply = this.addRenderableWidget(Button.builder(Component.translatable("screen.skinwardrobe.install"), button -> applySelected())
                .bounds(layout.centerX - 66, actionY, 62, 20)
                .build());
        apply.active = selected() != null;

        Button delete = this.addRenderableWidget(Button.builder(Component.translatable("screen.skinwardrobe.delete"), button -> deleteSelected())
                .bounds(layout.centerX + 4, actionY, 62, 20)
                .build());
        delete.active = selected() != null && selected().saved() != null;

        this.addRenderableWidget(Button.builder(Component.translatable("screen.skinwardrobe.reset"), button -> send("reset", "{}"))
                .bounds(layout.left + layout.panelWidth - 168, layout.bottom - 28, 76, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.minecraft.setScreen(this.parent))
                .bounds(layout.left + layout.panelWidth - 88, layout.bottom - 28, 74, 20)
                .build());

        if (urlFocused) {
            this.setFocused(this.urlBox);
            this.urlBox.setFocused(true);
        } else if (nameFocused) {
            this.setFocused(this.nameBox);
            this.nameBox.setFocused(true);
        }

        if (!this.syncRequested) {
            this.syncRequested = true;
            send("request_sync", "{}");
        }
    }

    public void refreshFromServer() {
        this.status = ClientWardrobeState.lastMessage();
        this.rebuildWidgets();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = layout();
        graphics.fill(layout.left, layout.top, layout.left + layout.panelWidth, layout.bottom, 0xE0101010);
        graphics.outline(layout.left, layout.top, layout.panelWidth, layout.panelHeight, 0xFF777777);
        drawCarouselFrames(graphics, layout);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        drawCarouselOverlays(graphics, layout);
        graphics.centeredText(this.font, TITLE, this.width / 2, layout.top + 12, 0xFFFFFFFF);

        GallerySkin selected = selected();
        int textY = layout.previewY + layout.previewSize + 16;
        if (selected == null) {
            graphics.centeredText(this.font, Component.translatable("screen.skinwardrobe.gallery.empty"), this.width / 2, textY, 0xFFFFE08A);
        } else {
            graphics.centeredText(this.font, Component.literal(selected.name()), this.width / 2, textY, 0xFFFFFFFF);
            graphics.centeredText(this.font, selected.sourceLabel(), this.width / 2, textY + 12, 0xFFBDBDBD);
            graphics.centeredText(this.font, Component.literal((this.selectedIndex + 1) + " / " + this.gallerySkins.size()), this.width / 2, layout.previewY - 16, 0xFFA0A0A0);
        }

        WardrobeEntry active = ClientWardrobeState.wardrobe().active;
        String activeText = active == null
                ? Component.translatable("screen.skinwardrobe.active.none").getString()
                : Component.translatable("screen.skinwardrobe.active", active.name).getString();
        graphics.text(this.font, activeText, layout.left + 14, layout.bottom - 24, 0xFFB8FFB8);
        if (!this.status.isBlank()) {
            graphics.text(this.font, this.status, layout.left + 14, layout.bottom - 12, 0xFFFFE08A);
        }
    }

    private void addCarouselWidgets(Layout layout) {
        for (PreviewSlot slot : previewSlots(layout)) {
            PlayerSkinWidget preview = new PlayerSkinWidget(slot.size, slot.size, Minecraft.getInstance().getEntityModels(), () -> previewSkinAtOffset(slot.offset));
            preview.setX(slot.x);
            preview.setY(slot.y);
            this.addRenderableWidget(preview);
        }

        Button previous = this.addRenderableWidget(Button.builder(Component.literal("<"), button -> selectPrevious())
                .bounds(layout.centerX - layout.previewSize / 2 - 36, layout.previewY + layout.previewSize / 2 - 10, 28, 20)
                .build());
        previous.active = this.gallerySkins.size() > 1;

        Button next = this.addRenderableWidget(Button.builder(Component.literal(">"), button -> selectNext())
                .bounds(layout.centerX + layout.previewSize / 2 + 8, layout.previewY + layout.previewSize / 2 - 10, 28, 20)
                .build());
        next.active = this.gallerySkins.size() > 1;
    }

    private void drawCarouselFrames(GuiGraphicsExtractor graphics, Layout layout) {
        for (PreviewSlot slot : previewSlots(layout)) {
            int padding = slot.offset == 0 ? 8 : 5;
            int color = slot.offset == 0 ? 0xFF606060 : 0xFF3E3E3E;
            graphics.fill(slot.x - padding, slot.y - padding, slot.x + slot.size + padding, slot.y + slot.size + padding, 0x70202020);
            graphics.outline(slot.x - padding, slot.y - padding, slot.size + padding * 2, slot.size + padding * 2, color);
        }
    }

    private void drawCarouselOverlays(GuiGraphicsExtractor graphics, Layout layout) {
        for (PreviewSlot slot : previewSlots(layout)) {
            if (slot.offset != 0) {
                graphics.fill(slot.x - 1, slot.y - 1, slot.x + slot.size + 1, slot.y + slot.size + 1, 0x78000000);
            }
        }
    }

    private List<PreviewSlot> previewSlots(Layout layout) {
        List<PreviewSlot> slots = new ArrayList<>();
        int count = this.gallerySkins.size();
        if (count == 0) {
            slots.add(new PreviewSlot(0, layout.centerX - layout.previewSize / 2, layout.previewY, layout.previewSize));
            return slots;
        }

        int sideSize = Math.max(112, layout.previewSize * 3 / 5);
        int farSize = Math.max(82, layout.previewSize * 2 / 5);
        if (count >= 5 && layout.panelWidth >= 820) {
            slots.add(new PreviewSlot(-2, layout.centerX - layout.previewSize / 2 - sideSize - farSize - 78, layout.previewY + (layout.previewSize - farSize) / 2, farSize));
            slots.add(new PreviewSlot(-1, layout.centerX - layout.previewSize / 2 - sideSize - 38, layout.previewY + (layout.previewSize - sideSize) / 2, sideSize));
            slots.add(new PreviewSlot(0, layout.centerX - layout.previewSize / 2, layout.previewY, layout.previewSize));
            slots.add(new PreviewSlot(1, layout.centerX + layout.previewSize / 2 + 38, layout.previewY + (layout.previewSize - sideSize) / 2, sideSize));
            slots.add(new PreviewSlot(2, layout.centerX + layout.previewSize / 2 + sideSize + 78, layout.previewY + (layout.previewSize - farSize) / 2, farSize));
        } else if (count >= 3) {
            slots.add(new PreviewSlot(-1, layout.centerX - layout.previewSize / 2 - sideSize - 42, layout.previewY + (layout.previewSize - sideSize) / 2, sideSize));
            slots.add(new PreviewSlot(0, layout.centerX - layout.previewSize / 2, layout.previewY, layout.previewSize));
            slots.add(new PreviewSlot(1, layout.centerX + layout.previewSize / 2 + 42, layout.previewY + (layout.previewSize - sideSize) / 2, sideSize));
        } else if (count == 2) {
            slots.add(new PreviewSlot(0, layout.centerX - layout.previewSize / 2, layout.previewY, layout.previewSize));
            slots.add(new PreviewSlot(1, layout.centerX + layout.previewSize / 2 + 42, layout.previewY + (layout.previewSize - sideSize) / 2, sideSize));
        } else {
            slots.add(new PreviewSlot(0, layout.centerX - layout.previewSize / 2, layout.previewY, layout.previewSize));
        }
        return slots;
    }

    private void rebuildGallery() {
        this.gallerySkins.clear();
        for (Map.Entry<String, WardrobeEntry> entry : ClientWardrobeState.wardrobe().entries.entrySet()) {
            WardrobeEntry value = entry.getValue();
            this.gallerySkins.add(GallerySkin.saved(entry.getKey(), value, ClientSkinPreview.saved(value)));
        }
        for (LocalSkinScanner.LocalSkin skin : this.localSkins) {
            this.gallerySkins.add(GallerySkin.local(skin, () -> ClientSkinPreview.localSkin(skin, this.model)));
        }
    }

    private void selectPrevious() {
        if (this.gallerySkins.isEmpty()) {
            return;
        }
        this.selectedIndex = (this.selectedIndex - 1 + this.gallerySkins.size()) % this.gallerySkins.size();
        updateNameFromSelection();
        this.rebuildWidgets();
    }

    private void selectNext() {
        if (this.gallerySkins.isEmpty()) {
            return;
        }
        this.selectedIndex = (this.selectedIndex + 1) % this.gallerySkins.size();
        updateNameFromSelection();
        this.rebuildWidgets();
    }

    private void updateNameFromSelection() {
        GallerySkin selected = selected();
        if (selected != null && this.nameBox != null) {
            this.nameBox.setValue(selected.name());
        }
    }

    private void applySelected() {
        GallerySkin selected = selected();
        if (selected == null) {
            return;
        }
        if (selected.local() != null) {
            applyLocal(selected.local());
        } else if (selected.saved() != null) {
            send("use", nameJson(selected.saved().name));
        }
    }

    private void deleteSelected() {
        GallerySkin selected = selected();
        if (selected != null && selected.saved() != null) {
            send("delete", nameJson(selected.saved().name));
        }
    }

    private void applyUrl(boolean save) {
        String url = this.urlBox.getValue();
        String name = this.nameBox.getValue().isBlank() ? "URL skin" : this.nameBox.getValue();
        SkinModel selectedModel = this.model;
        this.status = Component.translatable("skinwardrobe.status.working").getString();
        SkinDownloader.downloadPng(url)
                .thenCompose(bytes -> MineSkinClient.sign(bytes, selectedModel).thenApply(signedSkin -> new DownloadedSkin(bytes, signedSkin)))
                .whenComplete((downloaded, throwable) -> Minecraft.getInstance().execute(() -> {
                    if (throwable != null) {
                        this.status = MineSkinClient.unwrap(throwable).getMessage();
                        return;
                    }
                    try {
                        Path savedPath = LocalSkinScanner.saveDownloaded(name, downloaded.bytes());
                        sendSigned(name, selectedModel, "url", url, downloaded.signedSkin(), save);
                        refreshLocalSelection(savedPath);
                    } catch (IOException | IllegalArgumentException e) {
                        this.status = e.getMessage();
                    }
                }));
    }

    private void applyLocal(LocalSkinScanner.LocalSkin skin) {
        this.status = Component.translatable("skinwardrobe.status.working").getString();
        SkinModel selectedModel = this.model;
        MineSkinClient.sign(skin.bytes(), selectedModel).whenComplete((signedSkin, throwable) -> {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.execute(() -> {
                if (throwable != null) {
                    this.status = MineSkinClient.unwrap(throwable).getMessage();
                    return;
                }
                String name = this.nameBox.getValue().isBlank() ? skin.name() : this.nameBox.getValue();
                sendSigned(name, selectedModel, "local", skin.path().getFileName().toString(), signedSkin, false);
            });
        });
    }

    private void sendSigned(String name, SkinModel model, String sourceType, String source, SignedSkin signedSkin, boolean save) {
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        json.addProperty("model", model.id());
        json.addProperty("sourceType", sourceType);
        json.addProperty("source", source);
        json.addProperty("value", signedSkin.value());
        json.addProperty("signature", signedSkin.signature());
        json.addProperty("save", save);
        send("set_signed", json.toString());
    }

    private void refreshLocalSelection(Path selectedPath) {
        this.localSkins.clear();
        this.localSkins.addAll(LocalSkinScanner.scan());
        rebuildGallery();
        for (int i = 0; i < this.gallerySkins.size(); i++) {
            LocalSkinScanner.LocalSkin local = this.gallerySkins.get(i).local();
            if (local != null && local.path().equals(selectedPath)) {
                this.selectedIndex = i;
                break;
            }
        }
        updateNameFromSelection();
        this.rebuildWidgets();
    }

    private PlayerSkin selectedPreviewSkin() {
        return previewSkinAtOffset(0);
    }

    private PlayerSkin previewSkinAtOffset(int offset) {
        GallerySkin selected = selectedAtOffset(offset);
        return selected == null ? DefaultPlayerSkin.getDefaultSkin() : selected.preview().get();
    }

    private @Nullable GallerySkin selected() {
        return selectedAtOffset(0);
    }

    private @Nullable GallerySkin selectedAtOffset(int offset) {
        if (this.gallerySkins.isEmpty()) {
            return null;
        }
        clampSelection();
        int index = Math.floorMod(this.selectedIndex + offset, this.gallerySkins.size());
        return this.gallerySkins.get(index);
    }

    private void clampSelection() {
        if (this.gallerySkins.isEmpty()) {
            this.selectedIndex = 0;
        } else if (this.selectedIndex >= this.gallerySkins.size()) {
            this.selectedIndex = this.gallerySkins.size() - 1;
        } else if (this.selectedIndex < 0) {
            this.selectedIndex = 0;
        }
    }

    private Layout layout() {
        int panelWidth = Math.min(860, this.width - 24);
        int panelHeight = Math.min(410, this.height - 24);
        int left = (this.width - panelWidth) / 2;
        int top = Math.max(12, (this.height - panelHeight) / 2);
        int bottom = top + panelHeight;
        int previewSize = Math.max(168, Math.min(214, panelHeight - 196));
        int centerX = left + panelWidth / 2;
        int previewY = top + 118;
        return new Layout(panelWidth, panelHeight, left, top, bottom, centerX, previewY, previewSize);
    }

    private static String nameJson(String name) {
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        return json.toString();
    }

    private static void send(String action, String json) {
        if (Minecraft.getInstance().getConnection() != null) {
            ClientPacketDistributor.sendToServer(new SkinWardrobeCommandPayload(action, json));
        }
    }

    private record Layout(int panelWidth, int panelHeight, int left, int top, int bottom, int centerX, int previewY, int previewSize) {
    }

    private record PreviewSlot(int offset, int x, int y, int size) {
    }

    private record DownloadedSkin(byte[] bytes, SignedSkin signedSkin) {
    }

    private record GallerySkin(
            String key,
            String name,
            @Nullable WardrobeEntry saved,
            @Nullable LocalSkinScanner.LocalSkin local,
            Supplier<PlayerSkin> preview) {
        private static GallerySkin saved(String key, WardrobeEntry entry, Supplier<PlayerSkin> preview) {
            return new GallerySkin(key, entry.name, entry, null, preview);
        }

        private static GallerySkin local(LocalSkinScanner.LocalSkin skin, Supplier<PlayerSkin> preview) {
            return new GallerySkin(skin.path().toString(), skin.name(), null, skin, preview);
        }

        private Component sourceLabel() {
            if (this.local != null) {
                return Component.translatable("screen.skinwardrobe.source.local");
            }
            return Component.translatable("screen.skinwardrobe.source.saved");
        }
    }
}
