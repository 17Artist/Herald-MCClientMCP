package ai.herald.clientmod.action.composite;

import ai.herald.clientmod.util.McVersionCompat;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.DirectionUtil;
import ai.herald.clientmod.util.HandUtil;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Pick a hotbar slot (optional), aim at the chosen face of the target
 * block, then call {@link MultiPlayerGameMode#useItemOn} to place.
 */
public final class PlaceBlockAtAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        MultiPlayerGameMode gm = mc.gameMode;
        ClientPacketListener conn = mc.getConnection();
        if (player == null || level == null || gm == null) {
            return ActionResult.error(ErrorCode.NOT_IN_GAME, "Player not in world");
        }
        if (conn == null) return ActionResult.error(ErrorCode.NOT_IN_GAME, "Not connected");

        int x = JsonUtil.requireInt(params, "x");
        int y = JsonUtil.requireInt(params, "y");
        int z = JsonUtil.requireInt(params, "z");
        Direction face = DirectionUtil.fromString(JsonUtil.getStringOrDefault(params, "face", "up"));
        InteractionHand hand = HandUtil.fromString(JsonUtil.getStringOrDefault(params, "hand", "main_hand"));
        int slot = JsonUtil.getIntOrDefault(params, "slot", -1);

        if (slot >= 0) {
            if (slot > 8) {
                return ActionResult.error(ErrorCode.INVALID_PARAMS, "slot must be 0..8 (hotbar)");
            }
            Inventory inv = player.getInventory();
            if (McVersionCompat.getSelectedSlot(inv) != slot) {
                McVersionCompat.setSelectedSlot(inv, slot);
                conn.send(new ServerboundSetCarriedItemPacket(slot));
            }
        }

        // Aim at the face-centre.
        JsonObject lookParams = new JsonObject();
        lookParams.addProperty("x", x);
        lookParams.addProperty("y", y);
        lookParams.addProperty("z", z);
        lookParams.addProperty("face", face.getName());
        new LookAtBlockAction().execute(lookParams);

        BlockPos pos = new BlockPos(x, y, z);
        Vec3 cursor = new Vec3(x + 0.5 + face.getStepX() * 0.5,
                               y + 0.5 + face.getStepY() * 0.5,
                               z + 0.5 + face.getStepZ() * 0.5);
        BlockHitResult hit = new BlockHitResult(cursor, face, pos, false);
        InteractionResult result = gm.useItemOn(player, hand, hit);

        JsonObject data = new JsonObject();
        data.addProperty("result", result.toString());
        data.addProperty("consumed", result.consumesAction());
        data.addProperty("face", face.getName());
        if (slot >= 0) data.addProperty("slot", slot);
        return ActionResult.ok(data);
    }
}
