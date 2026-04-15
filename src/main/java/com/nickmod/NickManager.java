package com.nickmod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Central store for player nicknames.
 * Nicks are in-memory only and reset on server restart.
 * Add file/database persistence here if you want them to survive restarts.
 */
public class NickManager {

    private static final Map<UUID, String> nicknames = new HashMap<>();

    public static void setNick(UUID uuid, String nick) {
        nicknames.put(uuid, nick);
    }

    public static void clearNick(UUID uuid) {
        nicknames.remove(uuid);
    }

    /** Returns the nick, or null if the player has no nick set. */
    public static String getNick(UUID uuid) {
        return nicknames.get(uuid);
    }

    public static boolean hasNick(UUID uuid) {
        return nicknames.containsKey(uuid);
    }
}
