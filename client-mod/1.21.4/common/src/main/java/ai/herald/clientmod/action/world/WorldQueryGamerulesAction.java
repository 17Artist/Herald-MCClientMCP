package ai.herald.clientmod.action.world;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.GameRules;

public class WorldQueryGamerulesAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        ClientLevel level = McHelper.level();
        if (level == null) {
            return ActionResult.error(ErrorCode.NOT_IN_GAME, "Not in a world");
        }

        Minecraft mc = Minecraft.getInstance();
        // In 1.21.4, GameRules are not accessible from ClientLevel.
        // We can only read them from the integrated server in singleplayer.
        if (mc.getSingleplayerServer() == null) {
            JsonObject data = new JsonObject();
            data.addProperty("count", 0);
            data.add("gamerules", new JsonArray());
            data.addProperty("note", "GameRules are only accessible in singleplayer in 1.21.4");
            return ActionResult.ok(data);
        }

        GameRules gameRules = mc.getSingleplayerServer().getGameRules();
        JsonArray rules = new JsonArray();

        gameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
            @Override
            public <T extends GameRules.Value<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
                JsonObject entry = new JsonObject();
                entry.addProperty("rule", key.getId());
                T value = gameRules.getRule(key);
                entry.addProperty("value", value.serialize());
                rules.add(entry);
            }
        });

        JsonObject data = new JsonObject();
        data.addProperty("count", rules.size());
        data.add("gamerules", rules);
        return ActionResult.ok(data);
    }
}
