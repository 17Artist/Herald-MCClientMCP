package ai.herald.clientmod.action.modtest;

import ai.herald.clientmod.util.McVersionCompat;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Map;

/**
 * Returns detailed information about a block from its registry ID.
 */
public final class RegistryBlockInfoAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        String blockId = JsonUtil.requireString(params, "blockId");
        Identifier loc = Identifier.tryParse(blockId);
        if (loc == null) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Invalid block ID: " + blockId);
        }

        if (!BuiltInRegistries.BLOCK.containsKey(loc)) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Block not found: " + blockId);
        }

        Block block = McVersionCompat.registryGet(BuiltInRegistries.BLOCK, loc);
        BlockState defaultState = block.defaultBlockState();

        JsonObject data = new JsonObject();
        data.addProperty("id", loc.toString());
        data.addProperty("hardness", defaultState.getDestroySpeed(null, null));
        data.addProperty("resistance", block.getExplosionResistance());
        data.addProperty("requiresCorrectTool", defaultState.requiresCorrectToolForDrops());
        data.addProperty("lightLevel", defaultState.getLightEmission());
        data.addProperty("isFlammable", defaultState.ignitedByLava());
        data.addProperty("hasBlockEntity", defaultState.hasBlockEntity());
        data.addProperty("isAir", defaultState.isAir());

        // Sound type
        SoundType sound = defaultState.getSoundType();
        data.addProperty("soundType", sound.toString());

        // Default state properties
        JsonObject props = new JsonObject();
        for (Map.Entry<Property<?>, Comparable<?>> entry : defaultState.getValues().entrySet()) {
            props.addProperty(entry.getKey().getName(), entry.getValue().toString());
        }
        data.add("defaultStateProperties", props);

        return ActionResult.ok(data);
    }
}
