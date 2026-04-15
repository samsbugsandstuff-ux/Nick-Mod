package com.nickmod.mixin;

import com.nickmod.NickManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Injects into PlayerEntity.getDisplayName() so that all server-generated
 * text messages (death messages, advancement broadcasts, etc.) use the
 * player's nick instead of their real username.
 *
 * Note: This does NOT affect the client-rendered nametag above the player's
 * head, which is based on the GameProfile name. For nametags, see NickUtil.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerDisplayNameMixin {

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void nickmod_injectDisplayName(CallbackInfoReturnable<Text> cir) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        String nick = NickManager.getNick(self.getUuid());
        if (nick == null) return;

        MutableText nickText = Text.literal(nick);

        // Preserve team formatting (team colour, decorations) if the player is in a team
        AbstractTeam team = self.getScoreboardTeam();
        if (team != null) {
            cir.setReturnValue(AbstractTeam.decorateName(team, nickText));
        } else {
            cir.setReturnValue(nickText);
        }
    }
}
