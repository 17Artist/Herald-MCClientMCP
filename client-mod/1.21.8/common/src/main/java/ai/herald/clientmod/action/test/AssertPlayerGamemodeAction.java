package ai.herald.clientmod.action.test;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.Locale;

/**
 * Asserts that the player is in the specified game mode.
 */
public final class AssertPlayerGamemodeAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        String expected = JsonUtil.requireString(params, "mode").toLowerCase(Locale.ROOT);

        Minecraft mc = McHelper.mc();
        MultiPlayerGameMode gm = mc.gameMode;
        if (gm == null) {
            return ActionResult.error(ErrorCode.ASSERTION_FAILED,
                "Expected gamemode " + expected + " but gameMode is unavailable");
        }

        GameType actual = gm.getPlayerMode();

        // In singleplayer, the client-side mode may not be synced yet; read from integrated server
        if (mc.getSingleplayerServer() != null && mc.player != null) {
            ServerPlayer sp = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
            if (sp != null) {
                actual = sp.gameMode.getGameModeForPlayer();
            }
        }

        if (actual == null) {
            return ActionResult.error(ErrorCode.ASSERTION_FAILED,
                "Expected gamemode " + expected + " but gameMode is unavailable");
        }
        String actualName = actual.getName(); // canonical lowercase name ("survival","creative",...)

        // Accept both enum name ("CREATIVE") and canonical name ("creative")
        GameType expectedType = GameType.byName(expected, null);
        if (expectedType == null) {
            try { expectedType = GameType.valueOf(expected.toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException ignored) {}
        }
        if (expectedType == null) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                "Unknown gamemode: " + expected + ". Use: survival, creative, adventure, spectator");
        }

        if (actual != expectedType) {
            return ActionResult.error(ErrorCode.ASSERTION_FAILED,
                "Expected gamemode " + expected + " but got " + actualName);
        }

        JsonObject data = new JsonObject();
        data.addProperty("pass", true);
        data.addProperty("message", "Player gamemode is " + actual.getName());
        return ActionResult.ok(data);
    }
}
