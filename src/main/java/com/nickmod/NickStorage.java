package com.nickmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Static nickname store. Used by the mixin and command classes.
 * Persists nicknames to a JSON file so they survive server restarts.
 */
public class NickStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger("NickMod");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();

    // UUID → formatted nickname string
    private static final Map<UUID, String> nicknames = new HashMap<>();
    private static Path dataFile;

    public static void init(Path configDir) {
        dataFile = configDir.resolve("nickmod_data.json");
        load();
    }

    public static void setNick(UUID uuid, String nick) {
        nicknames.put(uuid, nick);
        save();
    }

    public static void removeNick(UUID uuid) {
        nicknames.remove(uuid);
        save();
    }

    public static String getNick(UUID uuid) {
        return nicknames.get(uuid);
    }

    public static boolean hasNick(UUID uuid) {
        return nicknames.containsKey(uuid);
    }

    // ── Persistence ────────────────────────────────────────────────────────────

    private static void load() {
        if (dataFile == null || !Files.exists(dataFile)) return;
        try (Reader reader = Files.newBufferedReader(dataFile)) {
            Map<String, String> raw = GSON.fromJson(reader, MAP_TYPE);
            if (raw != null) {
                raw.forEach((key, value) -> {
                    try {
                        nicknames.put(UUID.fromString(key), value);
                    } catch (IllegalArgumentException ignored) {}
                });
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load NickMod data: {}", e.getMessage());
        }
    }

    private static void save() {
        if (dataFile == null) return;
        try {
            Files.createDirectories(dataFile.getParent());
            Map<String, String> raw = new HashMap<>();
            nicknames.forEach((uuid, nick) -> raw.put(uuid.toString(), nick));
            try (Writer writer = Files.newBufferedWriter(dataFile)) {
                GSON.toJson(raw, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save NickMod data: {}", e.getMessage());
        }
    }
}
