package com.twily.skinwardrobe;

import com.twily.skinwardrobe.command.SkinWardrobeCommands;
import com.twily.skinwardrobe.network.SkinWardrobeNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(SkinWardrobe.MOD_ID)
public final class SkinWardrobe {
    public static final String MOD_ID = "skinwardrobe";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public SkinWardrobe(IEventBus modBus, ModContainer modContainer) {
        modBus.addListener(SkinWardrobeNetwork::registerPayloads);
        NeoForge.EVENT_BUS.addListener(SkinWardrobeCommands::register);
        NeoForge.EVENT_BUS.addListener(SkinWardrobeCommands::onPlayerLoggedIn);
    }
}
