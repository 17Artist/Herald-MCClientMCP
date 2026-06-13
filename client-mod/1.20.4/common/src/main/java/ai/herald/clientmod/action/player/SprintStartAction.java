package ai.herald.clientmod.action.player;

import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;

/** Port of BlackBoxPro player/SprintStartAction.kt. */
public final class SprintStartAction extends PlayerCommandAction {
    public SprintStartAction() { super(Action.START_SPRINTING); }
}
