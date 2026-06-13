package ai.herald.clientmod.action.world;

import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.McHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.GameRules;

public class WorldQueryGamerulesAction implements ActionExecutor {

    private static final String[][] KNOWN_GAMERULES = {
        {"doDaylightCycle", "boolean"},
        {"doWeatherCycle", "boolean"},
        {"doMobSpawning", "boolean"},
        {"doMobLoot", "boolean"},
        {"doTileDrops", "boolean"},
        {"doFireTick", "boolean"},
        {"mobGriefing", "boolean"},
        {"keepInventory", "boolean"},
        {"doInsomnia", "boolean"},
        {"doImmediateRespawn", "boolean"},
        {"drowningDamage", "boolean"},
        {"fallDamage", "boolean"},
        {"fireDamage", "boolean"},
        {"freezeDamage", "boolean"},
        {"naturalRegeneration", "boolean"},
        {"doLimitedCrafting", "boolean"},
        {"announceAdvancements", "boolean"},
        {"disableRaids", "boolean"},
        {"showDeathMessages", "boolean"},
        {"sendCommandFeedback", "boolean"},
        {"commandBlockOutput", "boolean"},
        {"logAdminCommands", "boolean"},
        {"doPatrolSpawning", "boolean"},
        {"doTraderSpawning", "boolean"},
        {"doWardenSpawning", "boolean"},
        {"forgiveDeadPlayers", "boolean"},
        {"universalAnger", "boolean"},
        {"playersSleepingPercentage", "int"},
        {"randomTickSpeed", "int"},
        {"spawnRadius", "int"},
        {"maxEntityCramming", "int"},
        {"maxCommandChainLength", "int"},
        {"spectatorsGenerateChunks", "boolean"},
        {"reducedDebugInfo", "boolean"},
    };

    @Override
    public ActionResult execute(JsonObject params) {
        ClientLevel level = McHelper.level();
        if (level == null) {
            return ActionResult.error(ErrorCode.NOT_IN_GAME, "Not in a world");
        }

        GameRules gameRules = level.getGameRules();
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
