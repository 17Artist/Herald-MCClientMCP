package ai.herald.clientmod.action.debug;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

public final class DebugSetGamemodeAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        String mode = JsonUtil.requireString(params, "mode");
        GameType target = parseGameType(mode);

        Minecraft mc = McHelper.mc();

        // In singleplayer: directly set gamemode on server player (bypasses cheats check)
        if (mc.getSingleplayerServer() != null) {
            ServerPlayer sp = mc.getSingleplayerServer().getPlayerList().getPlayer(player.getUUID());
            if (sp != null) {
                sp.setGameMode(target);
                // Wait for packet to sync to client
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                // Force sync client-side
                if (mc.gameMode != null) {
                    mc.gameMode.setLocalMode(target, target);
                }
                player.getAbilities().mayfly = (target == GameType.CREATIVE || target == GameType.SPECTATOR);
                player.getAbilities().instabuild = (target == GameType.CREATIVE);
            }
        } else {
            // Multiplayer: use command (requires OP)
            player.connection.sendCommand("gamemode " + mode);
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        }

        JsonObject data = new JsonObject();
        data.addProperty("command", "gamemode " + mode);
        data.addProperty("gamemode", target.getName());
        return ActionResult.ok(data);
    }

    private static GameType parseGameType(String mode) {
        switch (mode.toLowerCase()) {
            case "creative": case "1": return GameType.CREATIVE;
            case "adventure": case "2": return GameType.ADVENTURE;
            case "spectator": case "3": return GameType.SPECTATOR;
            default: return GameType.SURVIVAL;
        }
    }
}
