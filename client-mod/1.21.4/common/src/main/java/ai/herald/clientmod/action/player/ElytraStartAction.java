package ai.herald.clientmod.action.player;

import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;

/** Port of BlackBoxPro player/ElytraStartAction.kt. */
public final class ElytraStartAction extends PlayerCommandAction {
    public ElytraStartAction() { super(Action.START_FALL_FLYING); }
}
