package ai.herald.clientmod.action.debug;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.util.JsonUtil;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.food.FoodData;

public final class DebugSetHungerAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        LocalPlayer player = McHelper.player();
        if (player == null) return McHelper.notInGame();

        int food = JsonUtil.requireInt(params, "food");
        float saturation = (float) JsonUtil.getDoubleOrDefault(params, "saturation", 5.0);

        FoodData foodData = player.getFoodData();
        foodData.setFoodLevel(food);
        foodData.setSaturation(saturation);

        JsonObject data = new JsonObject();
        data.addProperty("food", food);
        data.addProperty("saturation", saturation);
        data.addProperty("note", "Client-side only; server may override");
        return ActionResult.ok(data);
    }
}
