package ai.herald.clientmod.action.player;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.world.entity.Entity;

/** Port of BlackBoxPro player/PlayerCommandAction.kt — generic player-command dispatcher. */
public class PlayerCommandAction implements ActionExecutor {

    private final Action action;

    public PlayerCommandAction(Action action) {
        this.action = action;
    }

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientPacketListener conn = mc.getConnection();
        if (player == null || conn == null) return McHelper.notInGame();

        Entity entity = player;
        if (params != null && params.has("entityId")) {
            int id = JsonUtil.getIntOrDefault(params, "entityId", player.getId());
            Entity e = mc.level != null ? mc.level.getEntity(id) : null;
            if (e != null) entity = e;
        }
        int data = (action == Action.START_RIDING_JUMP)
            ? JsonUtil.getIntOrDefault(params, "jumpBoost", 100) : 0;
        conn.send(new ServerboundPlayerCommandPacket(entity, action, data));
        // NOTE: We deliberately do NOT mirror sprinting/sneaking via
        // player.setSprinting()/setShiftKeyDown() here — LocalPlayer.aiStep
        // recomputes those flags from KeyboardInput every tick, so any local
        // override is clobbered immediately. To actually sustain a sprint,
        // chain this with `player_input {forward:true, sprint:true}`.
        return ActionResult.ok();
    }
}
