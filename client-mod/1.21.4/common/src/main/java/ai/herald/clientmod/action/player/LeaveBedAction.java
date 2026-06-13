package ai.herald.clientmod.action.player;

import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;

/** Port of BlackBoxPro player/LeaveBedAction.kt. */
public final class LeaveBedAction extends PlayerCommandAction {
    public LeaveBedAction() { super(Action.STOP_SLEEPING); }
}
