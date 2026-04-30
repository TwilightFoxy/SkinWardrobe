package com.twily.skinwardrobe.client;

import com.google.gson.JsonObject;
import com.twily.skinwardrobe.SkinWardrobe;
import com.twily.skinwardrobe.network.SkinWardrobeCommandPayload;
import com.twily.skinwardrobe.network.SkinWardrobeNetwork;
import com.twily.skinwardrobe.skin.MineSkinClient;
import com.twily.skinwardrobe.skin.SignedSkin;
import com.twily.skinwardrobe.skin.SkinModel;
import com.twily.skinwardrobe.storage.WardrobeEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.Nullable;

public final class WardrobeScreen extends Screen {
    private static final Component TITLE = Component.translatable("screen.skinwardrobe.title");
    private final @Nullable Screen parent;
    private final List<LocalSkinScanner.LocalSkin> localSkins = new ArrayList<>();
    private EditBox urlBox;
    private EditBox nameBox;
    private SkinModel model = SkinModel.CLASSIC;
    private @Nullable LocalSkinScanner.LocalSkin selectedLocal;
    private @Nullable String selectedSavedName;
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
        this.clearWidgets();

        int panelWidth = Math.min(520, this.width - 32);
        int left = (this.width - panelWidth) / 2;
        int top = 32;
        int fieldWidth = panelWidth - 160;

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
                .create(left + panelWidth - 136, top + 34, 122, 20, Component.translatable("screen.skinwardrobe.model"), (button, value) -> this.model = value));

        this.addRenderableWidget(Button.builder(Component.translatable("screen.skinwardrobe.apply_url"), button -> applyUrl(false))
                .bounds(left + panelWidth - 136, top + 62, 58, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("screen.skinwardrobe.save_url"), button -> applyUrl(true))
                .bounds(left + panelWidth - 74, top + 62, 60, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.translatable("screen.skinwardrobe.reset"), button -> send("reset", "{}"))
                .bounds(left + panelWidth - 136, this.height - 34, 58, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.minecraft.setScreen(this.parent))
                .bounds(left + panelWidth - 74, this.height - 34, 60, 20)
                .build());

        addSavedButtons(left + 14, top + 112, 210);
        addLocalButtons(left + panelWidth - 224, top + 112, 210);

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
        int panelWidth = Math.min(520, this.width - 32);
        int left = (this.width - panelWidth) / 2;
        int top = 32;
        int bottom = this.height - 44;

        graphics.fill(left, top, left + panelWidth, bottom, 0xD0101010);
        graphics.outline(left, top, panelWidth, bottom - top, 0xFF6D6D6D);
        graphics.centeredText(this.font, TITLE, this.width / 2, top + 12, 0xFFFFFFFF);
        graphics.text(this.font, Component.translatable("screen.skinwardrobe.saved"), left + 14, top + 96, 0xFFE0E0E0);
        graphics.text(this.font, Component.translatable("screen.skinwardrobe.local"), left + panelWidth - 224, top + 96, 0xFFE0E0E0);

        WardrobeEntry active = ClientWardrobeState.wardrobe().active;
        String activeText = active == null
                ? Component.translatable("screen.skinwardrobe.active.none").getString()
                : Component.translatable("screen.skinwardrobe.active", active.name).getString();
        graphics.text(this.font, activeText, left + 14, bottom - 24, 0xFFB8FFB8);
        if (!this.status.isBlank()) {
            graphics.text(this.font, this.status, left + 14, bottom - 12, 0xFFFFE08A);
        }

        if (this.selectedLocal != null) {
            int previewX = left + panelWidth / 2 - 48;
            int previewY = top + 110;
            graphics.outline(previewX - 3, previewY - 3, 102, 102, 0xFF808080);
            graphics.blit(RenderPipelines.GUI_TEXTURED, this.selectedLocal.textureId(), previewX, previewY, 0.0F, 0.0F, 96, 96, 64, 64);
            graphics.centeredText(this.font, this.selectedLocal.name(), previewX + 48, previewY + 106, 0xFFFFFFFF);
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void addSavedButtons(int x, int y, int width) {
        int index = 0;
        for (Map.Entry<String, WardrobeEntry> entry : ClientWardrobeState.wardrobe().entries.entrySet()) {
            if (index >= 8) {
                break;
            }
            String key = entry.getKey();
            WardrobeEntry value = entry.getValue();
            int rowY = y + index * 23;
            this.addRenderableWidget(Button.builder(Component.literal(value.name), button -> {
                        this.selectedSavedName = key;
                        this.nameBox.setValue(value.name);
                        send("use", nameJson(value.name));
                    })
                    .bounds(x, rowY, width - 44, 20)
                    .build());
            this.addRenderableWidget(Button.builder(Component.literal("X"), button -> send("delete", nameJson(value.name)))
                    .bounds(x + width - 40, rowY, 40, 20)
                    .build());
            index++;
        }
    }

    private void addLocalButtons(int x, int y, int width) {
        for (int i = 0; i < Math.min(8, this.localSkins.size()); i++) {
            LocalSkinScanner.LocalSkin skin = this.localSkins.get(i);
            int rowY = y + i * 23;
            this.addRenderableWidget(Button.builder(Component.literal(skin.name()), button -> {
                        this.selectedLocal = skin;
                        this.nameBox.setValue(skin.name());
                    })
                    .bounds(x, rowY, width - 58, 20)
                    .build());
            this.addRenderableWidget(Button.builder(Component.translatable("screen.skinwardrobe.apply"), button -> applyLocal(skin, false))
                    .bounds(x + width - 54, rowY, 54, 20)
                    .build());
        }
        this.addRenderableWidget(Button.builder(Component.translatable("screen.skinwardrobe.save_local"), button -> {
                    if (this.selectedLocal != null) {
                        applyLocal(this.selectedLocal, true);
                    }
                })
                .bounds(x, y + 8 * 23 + 4, width, 20)
                .build());
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
        this.selectedLocal = skin;
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
}
