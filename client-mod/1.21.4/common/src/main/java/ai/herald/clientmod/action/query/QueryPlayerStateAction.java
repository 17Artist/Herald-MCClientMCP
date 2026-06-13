package ai.herald.clientmod.action.query;

import ai.herald.clientmod.util.McVersionCompat;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;

import java.util.Optional;

/** Port of BlackBoxPro QueryPlayerStateAction.kt to Java + Mojang 1.20.1. */
public final class QueryPlayerStateAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        Minecraft mc = McHelper.mc();
        LocalPlayer player = mc.player;
        if (player == null) return McHelper.notInGame();

        JsonObject data = new JsonObject();
        data.addProperty("x", player.getX());
        data.addProperty("y", player.getY());
        data.addProperty("z", player.getZ());
        data.addProperty("yaw", player.getYRot());
        data.addProperty("pitch", player.getXRot());
        data.addProperty("health", player.getHealth());
        data.addProperty("maxHealth", player.getMaxHealth());
        data.addProperty("food", player.getFoodData().getFoodLevel());
        data.addProperty("saturation", player.getFoodData().getSaturationLevel());

        MultiPlayerGameMode gm = mc.gameMode;
        data.addProperty("gameMode", gm != null && gm.getPlayerMode() != null
            ? gm.getPlayerMode().name() : "unknown");

        data.addProperty("onGround", player.onGround());
        data.addProperty("sneaking", player.isShiftKeyDown());
        data.addProperty("sprinting", player.isSprinting());
        data.addProperty("flying", player.getAbilities().flying);
        data.addProperty("dead", player.isDeadOrDying());
        data.addProperty("selectedSlot", McVersionCompat.getSelectedSlot(player.getInventory()));
        data.addProperty("experienceLevel", player.experienceLevel);
        data.addProperty("experienceProgress", player.experienceProgress);

        data.addProperty("absorption", player.getAbsorptionAmount());
        data.addProperty("armorValue", player.getArmorValue());
        data.addProperty("airSupply", player.getAirSupply());
        data.addProperty("maxAirSupply", player.getMaxAirSupply());
        data.addProperty("isSwimming", player.isSwimming());
        data.addProperty("isUsingItem", player.isUsingItem());
        data.addProperty("isFallFlying", player.isFallFlying());
        data.addProperty("fallDistance", player.fallDistance);

        Entity vehicle = player.getVehicle();
        data.addProperty("vehicleId", vehicle != null ? vehicle.getId() : -1);

        ClientLevel level = mc.level;
        if (level != null) {
            data.addProperty("dimension", level.dimension().location().toString());
            Holder<Biome> biome = level.getBiome(player.blockPosition());
            Optional<ResourceKey<Biome>> biomeKey = biome.unwrapKey();
            data.addProperty("biome", biomeKey.map(k -> k.location().toString()).orElse("unknown"));
        } else {
            data.addProperty("dimension", "unknown");
            data.addProperty("biome", "unknown");
        }

        ItemStack mainItem = player.getMainHandItem();
        if (mainItem.isEmpty()) {
            data.addProperty("mainHandItem", "empty");
        } else {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(mainItem.getItem());
            data.addProperty("mainHandItem", itemId != null ? itemId.toString() : "unknown");
        }

        return ActionResult.ok(data);
    }
}
