package com.twily.skinwardrobe.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.twily.skinwardrobe.skin.PlayerSkinService;
import com.twily.skinwardrobe.skin.SkinModel;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class SkinWardrobeCommands {
    private SkinWardrobeCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("skinwardrobe")
                .then(Commands.literal("seturl")
                        .then(Commands.argument("url", StringArgumentType.string())
                                .executes(ctx -> setUrl(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "url"), SkinModel.CLASSIC))
                                .then(Commands.argument("model", StringArgumentType.word())
                                        .executes(ctx -> setUrl(
                                                ctx.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(ctx, "url"),
                                                parseModel(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "model")))))))
                .then(Commands.literal("saveurl")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .then(Commands.argument("url", StringArgumentType.string())
                                        .executes(ctx -> saveUrl(
                                                ctx.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(ctx, "name"),
                                                StringArgumentType.getString(ctx, "url"),
                                                SkinModel.CLASSIC))
                                        .then(Commands.argument("model", StringArgumentType.word())
                                                .executes(ctx -> saveUrl(
                                                        ctx.getSource().getPlayerOrException(),
                                                        StringArgumentType.getString(ctx, "name"),
                                                        StringArgumentType.getString(ctx, "url"),
                                                        parseModel(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "model"))))))))
                .then(Commands.literal("use")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .executes(ctx -> {
                                    PlayerSkinService.useSaved(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "name"));
                                    return 1;
                                })))
                .then(Commands.literal("delete")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .executes(ctx -> {
                                    PlayerSkinService.deleteSaved(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "name"));
                                    return 1;
                                })))
                .then(Commands.literal("list")
                        .executes(ctx -> {
                            PlayerSkinService.sendList(ctx.getSource().getPlayerOrException());
                            return 1;
                        }))
                .then(Commands.literal("reset")
                        .executes(ctx -> {
                            PlayerSkinService.reset(ctx.getSource().getPlayerOrException());
                            return 1;
                        }))
                .then(Commands.literal("current")
                        .executes(ctx -> {
                            PlayerSkinService.sendCurrent(ctx.getSource().getPlayerOrException());
                            return 1;
                        })));
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerSkinService.applyActiveOnLogin(player);
        }
    }

    private static int setUrl(ServerPlayer player, String url, SkinModel model) {
        PlayerSkinService.setUrl(player, url, model, false, "");
        return 1;
    }

    private static int saveUrl(ServerPlayer player, String name, String url, SkinModel model) {
        PlayerSkinService.setUrl(player, url, model, true, name);
        return 1;
    }

    private static SkinModel parseModel(ServerPlayer player, String raw) {
        return SkinModel.parse(raw).orElseGet(() -> {
            player.sendSystemMessage(Component.translatable("skinwardrobe.error.model", raw));
            return SkinModel.CLASSIC;
        });
    }
}
