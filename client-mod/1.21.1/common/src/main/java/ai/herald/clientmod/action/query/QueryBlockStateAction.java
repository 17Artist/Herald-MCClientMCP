package ai.herald.clientmod.action.query;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Map;
import java.util.Optional;

/** Port of BlackBoxPro QueryBlockStateAction.kt to Java + Mojang 1.20.1. */
public final class QueryBlockStateAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        ClientLevel level = McHelper.level();
        if (level == null) return McHelper.notInGame();

        int x = JsonUtil.requireInt(params, "x");
        int y = JsonUtil.requireInt(params, "y");
        int z = JsonUtil.requireInt(params, "z");
        BlockPos pos = new BlockPos(x, y, z);

        BlockState state = level.getBlockState(pos);
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());

        JsonObject data = new JsonObject();
        data.addProperty("blockId", blockId != null ? blockId.toString() : "unknown");
        data.addProperty("isAir", state.isAir());

        JsonObject properties = new JsonObject();
        for (Map.Entry<Property<?>, Comparable<?>> entry : state.getValues().entrySet()) {
            properties.addProperty(entry.getKey().getName(), entry.getValue().toString());
        }
        data.add("properties", properties);

        data.addProperty("lightLevel", level.getMaxLocalRawBrightness(pos));
        data.addProperty("blockLight", level.getBrightness(LightLayer.BLOCK, pos));
        data.addProperty("skyLight", level.getBrightness(LightLayer.SKY, pos));

        Holder<Biome> biome = level.getBiome(pos);
        Optional<ResourceKey<Biome>> biomeKey = biome.unwrapKey();
        data.addProperty("biome", biomeKey.map(k -> k.location().toString()).orElse("unknown"));

        data.addProperty("hardness", state.getDestroySpeed(level, pos));
        data.addProperty("x", x);
        data.addProperty("y", y);
        data.addProperty("z", z);

        return ActionResult.ok(data);
    }
}
