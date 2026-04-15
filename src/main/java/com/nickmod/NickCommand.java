package com.nickmod;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;

public class NickCommand {

    /**
     * Registers the /nick command.
     * Usage:
     *   /nick            → reset your nickname
     *   /nick <name>     → set your nickname
     *   /nick <player> <name|reset>   → ops only, set another player's nick
     */
    public static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> register() {
        return CommandManager.literal("nick")
            // /nick  (no args → reset self)
            .executes(NickCommand::resetSelf)

            // /nick <name>
            .then(CommandManager.argument("nickname", StringArgumentType.word())
                .executes(NickCommand::setSelf)

                // /nick <player> <name|reset>  (requires op)
                .then(CommandManager.argument("target", StringArgumentType.word())
                    .requires(src -> src.hasPermissionLevel(2))
                    .executes(NickCommand::setOther)
                )
            );
    }

    // /nick  → reset own nick
    private static int resetSelf(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("This command must be run by a player."));
            return 0;
        }
        if (!NickStorage.hasNick(player.getUuid())) {
            ctx.getSource().sendFeedback(() ->
                Text.literal("You don't have a nickname set.").formatted(Formatting.YELLOW), false);
            return 1;
        }
        NickStorage.removeNick(player.getUuid());
        clearNametag(player);
        ctx.getSource().sendFeedback(() ->
            Text.literal("Your nickname has been reset to ").formatted(Formatting.GREEN)
                .append(Text.literal(player.getName().getString()).formatted(Formatting.WHITE))
                .append(Text.literal(".").formatted(Formatting.GREEN)),
            false);
        return 1;
    }

    // /nick <name>  → set own nick
    private static int setSelf(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("This command must be run by a player."));
            return 0;
        }
        String raw = StringArgumentType.getString(ctx, "nickname");
        return applyNick(ctx.getSource(), player, raw);
    }

    // /nick <name> <target>  → op sets another player's nick
    private static int setOther(CommandContext<ServerCommandSource> ctx) {
        String raw  = StringArgumentType.getString(ctx, "nickname");
        String targetName = StringArgumentType.getString(ctx, "target");

        ServerPlayerEntity target = ctx.getSource().getServer()
            .getPlayerManager().getPlayer(targetName);

        if (target == null) {
            ctx.getSource().sendError(Text.literal("Player not found: " + targetName));
            return 0;
        }

        if (raw.equalsIgnoreCase("reset")) {
            NickStorage.removeNick(target.getUuid());
            clearNametag(target);
            ctx.getSource().sendFeedback(() ->
                Text.literal("Reset " + target.getName().getString() + "'s nickname.").formatted(Formatting.GREEN),
                true);
            target.sendMessage(Text.literal("Your nickname was reset by " + ctx.getSource().getName() + ".").formatted(Formatting.YELLOW));
            return 1;
        }

        return applyNick(ctx.getSource(), target, raw);
    }

    // ── Shared helpers ─────────────────────────────────────────────────────────

    private static int applyNick(ServerCommandSource source, ServerPlayerEntity player, String raw) {
        // Validate
        if (raw.length() > 32) {
            source.sendError(Text.literal("Nickname too long (max 32 characters)."));
            return 0;
        }
        if (!raw.matches("[a-zA-Z0-9_ ]+")) {
            source.sendError(Text.literal("Nickname may only contain letters, numbers, underscores, and spaces."));
            return 0;
        }

        NickStorage.setNick(player.getUuid(), raw);
        applyNametag(player, raw);

        boolean isSelf = source.getPlayer() == player;
        Text feedback = Text.literal("Nickname set to ").formatted(Formatting.GREEN)
            .append(Text.literal(raw).formatted(Formatting.WHITE))
            .append(Text.literal(".").formatted(Formatting.GREEN));

        source.sendFeedback(() -> feedback, false);
        if (!isSelf) {
            player.sendMessage(Text.literal("Your nickname was set to ").formatted(Formatting.YELLOW)
                .append(Text.literal(raw).formatted(Formatting.WHITE))
                .append(Text.literal(" by " + source.getName() + ".").formatted(Formatting.YELLOW)));
        }
        return 1;
    }

    /**
     * Nametag above head:
     * Hide the real scoreboard nametag via a team, then show the nick
     * as the entity's custom name (floating text above head).
     */
    public static void applyNametag(ServerPlayerEntity player, String nick) {
        ServerScoreboard scoreboard = player.getServer().getScoreboard();
        String teamName = "nm_" + player.getUuid().toString().replace("-", "").substring(0, 13);

        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.addTeam(teamName);
        }
        team.setNameTagVisibilityRule(AbstractTeam.VisibilityRule.NEVER);

        String entry = player.getNameForScoreboard();
        if (!team.getPlayerList().contains(entry)) {
            scoreboard.addPlayerToTeam(entry, team);
        }

        player.setCustomName(Text.literal(nick));
        player.setCustomNameVisible(true);
    }

    /**
     * Restores the real nametag by re-enabling team tag visibility
     * and clearing the custom name.
     */
    public static void clearNametag(ServerPlayerEntity player) {
        ServerScoreboard scoreboard = player.getServer().getScoreboard();
        String teamName = "nm_" + player.getUuid().toString().replace("-", "").substring(0, 13);

        Team team = scoreboard.getTeam(teamName);
        if (team != null) {
            team.setNameTagVisibilityRule(AbstractTeam.VisibilityRule.ALWAYS);
        }
        player.setCustomName(null);
        player.setCustomNameVisible(false);
    }
}
