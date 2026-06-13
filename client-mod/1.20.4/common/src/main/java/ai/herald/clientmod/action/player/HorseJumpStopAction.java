package ai.herald.clientmod.action.player;

import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;

/** Port of BlackBoxPro player/HorseJumpStopAction.kt. */
public final class HorseJumpStopAction extends PlayerCommandAction {
    public HorseJumpStopAction() { super(Action.STOP_RIDING_JUMP); }
}
