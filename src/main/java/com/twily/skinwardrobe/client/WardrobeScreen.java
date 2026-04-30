package com.twily.skinwardrobe.client;

import com.google.gson.JsonObject;
import com.twily.skinwardrobe.network.SkinWardrobeCommandPayload;
import com.twily.skinwardrobe.skin.MineSkinClient;
import com.twily.skinwardrobe.skin.SignedSkin;
import com.twily.skinwardrobe.skin.SkinModel;
import com.twily.skinwardrobe.storage.WardrobeEntry;
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
    private SkinModel model = SkinModel.CLASSIC;
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

        int panelWidth = Math.min(600, this.width - 24);
        int panelHeight = Math.min(330, this.height - 24);
        int left = (this.width - panelWidth) / 2;
        int top = Math.max(12, (this.height - panelHeight) / 2);
        int bottom = top + panelHeight;
        int fieldWidth = panelWidth - 170;
        int previewSize = Math.max(132, Math.min(190, panelHeight - 142));
        int previewX = left + (panelWidth - previewSize) / 2;
        int previewY = top + 94;

        this.urlBox = new EditBox(this.font, left + 14, top + 34, fieldWidth, 20, Component.translatable("screen.skinwardrobe.url"));
        this.urlBox.setMaxLength(2048);
        this.urlBox.setValue(urlValue);
        this.urlBox.setHint(Component.translatable("screen.skinwardrobe.url.hint"));
        this.addRenderableWidget(this.urlBox);

        this.nameBox = new EditBox(this.font, left + 14, top + 62, fieldWidth, 20, Component.translatable("screen.skinwardrobe.name"));
        this.nameBox.setMaxLength(32);
        this.nameBox.setValue(nameValue);
        this.nameBox.setHint(Component.translatable("screen.skinwardrobe.name.hint"));
        this.addRenderableWidget(this.nameBox);

        this.addRenderableWidget(CycleButton.builder(value -> Component.translatable("screen.skinwardrobe.model." + value.id()), this.model)
                .withValues(SkinModel.CLASSIC, SkinModel.SLIM)
                .create(left + panelWidth - 142, top + 34, 128, 20, Component.translatable("screen.skinwardrobe.model"), (button, value) -> this.model = value));

        this.addRenderableWidget(Button.builder(Component.translatable("screen.skinwardrobe.apply_url"), button -> applyUrl(false))
                .bounds(left + panelWidth - 142, top + 62, 62, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("screen.skinwardrobe.save_url"), button -> applyUrl(true))
                .bounds(left + panelWidth - 76, top + 62, 62, 20)
                .build());

        PlayerSkinWidget preview = new PlayerSkinWidget(previewSize, previewSize, Minecraft.getInstance().getEntityModels(), this::selectedPreviewSkin);
        preview.setX(previewX);
        preview.setY(previewY);
        this.addRenderableWidget(preview);

        Button previous = this.addRenderableWidget(Button.builder(Component.literal("<"), button -> selectPrevious())
                .bounds(previewX - 42, previewY + previewSize / 2 - 10, 30, 20)
                .build());
        previous.active = this.gallerySkins.size() > 1;

        Button next = this.addRenderableWidget(Button.builder(Component.literal(">"), button -> selectNext())
                .bounds(previewX + previewSize + 12, previewY + previewSize / 2 - 10, 30, 20)
                .build());
        next.active = this.gallerySkins.size() > 1;

        int actionY = bottom - 58;
        Button apply = this.addRenderableWidget(Button.builder(Component.translatable("screen.skinwardrobe.install"), button -> applySelected(false))
                .bounds(left + panelWidth / 2 - 96, actionY, 60, 20)
                .build());
        apply.active = selected() != null;

        Button save = this.addRenderableWidget(Button.builder(Component.translatable("screen.skinwardrobe.save_selected"), button -> applySelected(true))
                .bounds(left + panelWidth / 2 - 32, actionY, 64, 20)
                .build());
        save.active = selected() != null && selected().local() != null;

        Button delete = this.addRenderableWidget(Button.builder(Component.translatable("screen.skinwardrobe.delete"), button -> deleteSelected())
                .bounds(left + panelWidth / 2 + 36, actionY, 60, 20)
                .build());
        delete.active = selected() != null && selected().saved() != null;

        this.addRenderableWidget(Button.builder(Component.translatable("screen.skinwardrobe.reset"), button -> send("reset", "{}"))
                .bounds(left + panelWidth - 142, bottom - 28, 62, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.minecraft.setScreen(this.parent))
                .bounds(left + panelWidth - 76, bottom - 28, 62, 20)
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
        int panelWidth = Math.min(600, this.width - 24);
        int panelHeight = Math.min(330, this.height - 24);
        int left = (this.width - panelWidth) / 2;
        int top = Math.max(12, (this.height - panelHeight) / 2);
        int bottom = top + panelHeight;
        int previewSize = Math.max(132, Math.min(190, panelHeight - 142));
        int previewX = left + (panelWidth - previewSize) / 2;
        int previewY = top + 94;

        graphics.fill(left, top, left + panelWidth, bottom, 0xE0101010);
        graphics.outline(left, top, panelWidth, panelHeight, 0xFF777777);
        graphics.fill(previewX - 8, previewY - 8, previewX + previewSize + 8, previewY + previewSize + 8, 0x70202020);
        graphics.outline(previewX - 8, previewY - 8, previewSize + 16, previewSize + 16, 0xFF505050);
        graphics.centeredText(this.font, TITLE, this.width / 2, top + 12, 0xFFFFFFFF);

        GallerySkin selected = selected();
        if (selected == null) {
            graphics.centeredText(this.font, Component.translatable("screen.skinwardrobe.gallery.empty"), this.width / 2, previewY + previewSize + 14, 0xFFFFE08A);
        } else {
            graphics.centeredText(this.font, Component.literal(selected.name()), this.width / 2, previewY + previewSize + 12, 0xFFFFFFFF);
            graphics.centeredText(this.font, selected.sourceLabel(), this.width / 2, previewY + previewSize + 24, 0xFFBDBDBD);
            graphics.centeredText(this.font, Component.literal((this.selectedIndex + 1) + " / " + this.gallerySkins.size()), this.width / 2, top + 84, 0xFFA0A0A0);
        }

        WardrobeEntry active = ClientWardrobeState.wardrobe().active;
        String activeText = active == null
                ? Component.translatable("screen.skinwardrobe.active.none").getString()
                : Component.translatable("screen.skinwardrobe.active", active.name).getString();
        graphics.text(this.font, activeText, left + 14, bottom - 24, 0xFFB8FFB8);
        if (!this.status.isBlank()) {
            graphics.text(this.font, this.status, left + 14, bottom - 12, 0xFFFFE08A);
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
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

    private void applySelected(boolean save) {
        GallerySkin selected = selected();
        if (selected == null) {
            return;
        }
        if (selected.local() != null) {
            applyLocal(selected.local(), save);
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
        JsonObject json = new JsonObject();
        json.addProperty("url", this.urlBox.getValue());
        json.addProperty("model", this.model.id());
        json.addProperty("save", save);
        json.addProperty("name", this.nameBox.getValue().isBlank() ? "URL skin" : this.nameBox.getValue());
        send("set_url", json.toString());
        this.status = Component.translatable("skinwardrobe.status.working").getString();
    }

    private void applyLocal(LocalSkinScanner.LocalSkin skin, boolean save) {
        this.status = Component.translatable("skinwardrobe.status.working").getString();
        SkinModel selectedModel = this.model;
        MineSkinClient.sign(skin.bytes(), selectedModel).whenComplete((signedSkin, throwable) -> {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.execute(() -> {
                if (throwable != null) {
                    this.status = MineSkinClient.unwrap(throwable).getMessage();
                    return;
                }
                sendSigned(skin, selectedModel, signedSkin, save);
            });
        });
    }

    private void sendSigned(LocalSkinScanner.LocalSkin skin, SkinModel model, SignedSkin signedSkin, boolean save) {
        JsonObject json = new JsonObject();
        json.addProperty("name", this.nameBox.getValue().isBlank() ? skin.name() : this.nameBox.getValue());
        json.addProperty("model", model.id());
        json.addProperty("sourceType", "local");
        json.addProperty("source", skin.path().getFileName().toString());
        json.addProperty("value", signedSkin.value());
        json.addProperty("signature", signedSkin.signature());
        json.addProperty("save", save);
        send("set_signed", json.toString());
    }

    private PlayerSkin selectedPreviewSkin() {
        GallerySkin selected = selected();
        return selected == null ? DefaultPlayerSkin.getDefaultSkin() : selected.preview().get();
    }

    private @Nullable GallerySkin selected() {
        if (this.gallerySkins.isEmpty()) {
            return null;
        }
        clampSelection();
        return this.gallerySkins.get(this.selectedIndex);
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
