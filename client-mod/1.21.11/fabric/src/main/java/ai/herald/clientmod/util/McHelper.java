package ai.herald.clientmod.util;

import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;

/**
 * Small helpers for Minecraft client-side state access + standard
 * {@link ActionResult#error(ErrorCode, String)} returns when the client
 * is not in a playable state.
 */
public final class McHelper {

    private McHelper() {}

    public static Minecraft mc() {
        return Minecraft.getInstance();
    }

    public static LocalPlayer player() {
        return Minecraft.getInstance().player;
    }

    public static ClientLevel level() {
        return Minecraft.getInstance().level;
    }

    public static ClientPacketListener connection() {
        return Minecraft.getInstance().getConnection();
    }

    public static MultiPlayerGameMode gameMode() {
        return Minecraft.getInstance().gameMode;
    }

    public static ActionResult notInGame() {
        return ActionResult.error(ErrorCode.NOT_IN_GAME, "Player not in a world");
    }

    public static ActionResult notConnected() {
        return ActionResult.error(ErrorCode.NOT_IN_GAME, "Not connected to server");
    }
}
