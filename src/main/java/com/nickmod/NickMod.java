package com.nickmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NickMod implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("NickMod");

    @Override
    public void onInitialize() {
        // Load saved nicknames from disk
        NickStorage.init(FabricLoader.getInstance().getConfigDir());

        // Register /nick command
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(NickCommand.register());
        });

        // When a player joins, re-apply their saved nametag if they have a nick
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var player = handler.player;
            if (NickStorage.hasNick(player.getUuid())) {
                // Schedule one tick later so the player is fully initialised
                server.execute(() -> NickCommand.applyNametag(player, NickStorage.getNick(player.getUuid())));
            }
        });

        LOGGER.info("NickMod loaded.");
    }
}
