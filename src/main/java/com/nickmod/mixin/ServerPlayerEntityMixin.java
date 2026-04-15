package com.nickmod.mixin;

import com.nickmod.NickStorage;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {

    /**
     * Replaces the player's display name with their nickname wherever
     * Minecraft uses getDisplayName() — chat sender, death messages,
     * advancement broadcasts, and join/leave messages.
     */
    @Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
    private void nickmod$getDisplayName(CallbackInfoReturnable<Text> cir) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        if (NickStorage.hasNick(self.getUuid())) {
            cir.setReturnValue(Text.literal(NickStorage.getNick(self.getUuid())));
        }
    }

    /**
     * Replaces the tab list (player list) name with the nickname.
     * In vanilla this returns null by default, causing the username to be shown.
     * Returning the nick here overrides the tab list entry.
     */
    @Inject(method = "getPlayerListName", at = @At("HEAD"), cancellable = true)
    private void nickmod$getPlayerListName(CallbackInfoReturnable<Text> cir) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        if (NickStorage.hasNick(self.getUuid())) {
            cir.setReturnValue(Text.literal(NickStorage.getNick(self.getUuid())));
        }
    }
}
