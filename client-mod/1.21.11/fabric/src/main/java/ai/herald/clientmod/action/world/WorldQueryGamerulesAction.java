package ai.herald.clientmod.action.world;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.gamerules.GameRuleTypeVisitor;
import net.minecraft.world.level.gamerules.GameRule;

public class WorldQueryGamerulesAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        ClientLevel level = McHelper.level();
        if (level == null) {
            return ActionResult.error(ErrorCode.NOT_IN_GAME, "Not in a world");
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.getSingleplayerServer() == null) {
            JsonObject data = new JsonObject();
            data.addProperty("count", 0);
            data.add("gamerules", new JsonArray());
            data.addProperty("note", "GameRules are only accessible in singleplayer");
            return ActionResult.ok(data);
        }

        GameRules gameRules = mc.getSingleplayerServer().overworld().getGameRules();
        JsonArray rules = new JsonArray();

        gameRules.visitGameRuleTypes(new GameRuleTypeVisitor() {
            @Override
            public <T> void visit(GameRule<T> rule) {
                JsonObject entry = new JsonObject();
                entry.addProperty("rule", rule.id());
                T value = gameRules.get(rule);
                entry.addProperty("value", value != null ? value.toString() : "");
                rules.add(entry);
            }
        });

        JsonObject data = new JsonObject();
        data.addProperty("count", rules.size());
        data.add("gamerules", rules);
        return ActionResult.ok(data);
    }
}
