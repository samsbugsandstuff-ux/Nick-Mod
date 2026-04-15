package com.nickmod.mixin;

import com.nickmod.NickManager;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Intercepts the join and quit broadcast messages in PlayerManager and replaces
 * the player's real name with their nick when one is set.
 *
 * Strategy:
 *   1. @Inject at HEAD of onPlayerConnect/remove to capture the player's UUID.
 *   2. @ModifyArg on the broadcast call to swap the Text with a nick-based version.
 *
 * Since Minecraft's server runs on a single thread, a plain @Unique field is safe here.
 */
@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {

    /** Temporarily stores the UUID of the player currently joining/leaving. */
    @Unique
    private UUID nickmod_pendingUuid = null;

    // ── Join ────────────────────────────────────────────────────────────────────

    @Inject(method = "onPlayerConnect", at = @At("HEAD"))
    private void nickmod_captureJoiningPlayer(ClientConnection connection,
                                               ServerPlayerEntity player,
                                               ConnectedClientData clientData,
                                               CallbackInfo ci) {
        this.nickmod_pendingUuid = player.getUuid();
    }

    /**
     * Replaces the Text passed to the first broadcast() call inside onPlayerConnect.
     * Vanilla format: "%s joined the game" — we swap the player name for the nick.
     */
    @ModifyArg(
        method = "onPlayerConnect",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Z)V"),
        index = 0
    )
    private Text nickmod_modifyJoinMessage(Text original) {
        if (nickmod_pendingUuid == null) return original;
        String nick = NickManager.getNick(nickmod_pendingUuid);
        if (nick == null) return original;
        return Text.translatable("multiplayer.player.joined", Text.literal(nick))
                   .formatted(Formatting.YELLOW);
    }

    // ── Quit ─────────────────────────────────────────────────────────────────────

    @Inject(method = "remove", at = @At("HEAD"))
    private void nickmod_captureLeavingPlayer(ServerPlayerEntity player, CallbackInfo ci) {
        this.nickmod_pendingUuid = player.getUuid();
    }

    /**
     * Replaces the Text passed to the broadcast() call inside remove().
     * Vanilla format: "%s left the game" — we swap the player name for the nick.
     */
    @ModifyArg(
        method = "remove",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Z)V"),
        index = 0
    )
    private Text nickmod_modifyQuitMessage(Text original) {
        if (nickmod_pendingUuid == null) return original;
        String nick = NickManager.getNick(nickmod_pendingUuid);
        if (nick == null) return original;
        return Text.translatable("multiplayer.player.left", Text.literal(nick))
                   .formatted(Formatting.YELLOW);
    }
}
