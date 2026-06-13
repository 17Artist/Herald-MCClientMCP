package ai.herald.clientmod.action.player;

import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;

/** Port of BlackBoxPro player/SprintStopAction.kt. */
public final class SprintStopAction extends PlayerCommandAction {
    public SprintStopAction() { super(Action.STOP_SPRINTING); }
}
