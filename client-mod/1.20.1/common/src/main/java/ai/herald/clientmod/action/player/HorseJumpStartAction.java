package ai.herald.clientmod.action.player;

import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;

/** Port of BlackBoxPro player/HorseJumpStartAction.kt. */
public final class HorseJumpStartAction extends PlayerCommandAction {
    public HorseJumpStartAction() { super(Action.START_RIDING_JUMP); }
}
