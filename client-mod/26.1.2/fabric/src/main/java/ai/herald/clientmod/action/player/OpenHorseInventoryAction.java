package ai.herald.clientmod.action.player;

import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;

/** Port of BlackBoxPro player/OpenHorseInventoryAction.kt. */
public final class OpenHorseInventoryAction extends PlayerCommandAction {
    public OpenHorseInventoryAction() { super(Action.OPEN_INVENTORY); }
}
