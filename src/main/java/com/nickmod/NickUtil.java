package com.nickmod;

import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Handles scoreboard team management for nametags above players' heads.
 *
 * LIMITATION: Pure server-side Fabric mods cannot fully replace the nametag without
 * a client-side component. This uses the scoreboard team prefix trick:
 *   [prefix = nick][real username]
 * The real username will still appear after the nick. To avoid confusion, the prefix
 * is formatted so the nick stands out (e.g. coloured or bracketed).
 *
 * If you want ONLY the nick to show (hiding the real name), you would need either:
 *   1. A companion client-side mod
 *   2. Using NameTagVisibility = NEVER + a Text Display entity above the player (complex)
 */
public class NickUtil {

    private static final String TEAM_PREFIX = "nm_";

    public static void applyNametag(ServerPlayerEntity player, String nick) {
        Scoreboard scoreboard = player.getServer().getScoreboard();
        String teamName = getTeamName(player);

        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.addTeam(teamName);
        }

        // Show nick as a colored prefix, followed by the real name
        // Format: [Nick] RealName
        team.setPrefix(Text.literal("[" + nick + "§r] "));
        team.setSuffix(Text.empty());

        String playerName = player.getGameProfile().getName();
        if (!team.isPartOf(scoreboard.getPlayerTeam(playerName))) {
            scoreboard.addPlayerToTeam(playerName, team);
        }
    }

    public static void clearNametag(ServerPlayerEntity player) {
        Scoreboard scoreboard = player.getServer().getScoreboard();
        String teamName = getTeamName(player);
        Team team = scoreboard.getTeam(teamName);
        if (team != null) {
            scoreboard.removeTeam(team);
        }
    }

    private static String getTeamName(ServerPlayerEntity player) {
        // Team names max 16 chars
        return TEAM_PREFIX + player.getUuidAsString().replace("-", "").substring(0, 13);
    }
}
