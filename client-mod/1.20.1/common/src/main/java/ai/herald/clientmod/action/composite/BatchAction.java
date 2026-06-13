package ai.herald.clientmod.action.composite;

import ai.herald.clientmod.HeraldClientMod;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.dispatcher.CommandDispatcher;
import ai.herald.clientmod.protocol.ErrorCode;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Run a list of sub-actions sequentially on the client thread, collecting
 * each ActionResult into a {@code results} array. Nested {@code batch} is
 * disallowed; the {@code wait} action is supported as a no-op (its tick
 * delay is ignored — async waits should use {@code wait} via the skill API).
 */
public final class BatchAction implements ActionExecutor {

    private static final int MAX_BATCH = 64;

    @Override
    public ActionResult execute(JsonObject params) {
        if (params == null || !params.has("actions") || !params.get("actions").isJsonArray()) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS, "Missing required field: actions");
        }
        JsonArray actions = params.getAsJsonArray("actions");
        if (actions.size() > MAX_BATCH) {
            return ActionResult.error(ErrorCode.INVALID_PARAMS,
                "Batch too large: " + actions.size() + " > " + MAX_BATCH);
        }

        CommandDispatcher dispatcher = HeraldClientMod.dispatcher();
        if (dispatcher == null || dispatcher.registry() == null) {
            return ActionResult.error(ErrorCode.MAINTHREAD_FAILURE, "Dispatcher not available");
        }

        JsonArray results = new JsonArray();
        for (int i = 0; i < actions.size(); i++) {
            JsonElement el = actions.get(i);
            JsonObject step = el.isJsonObject() ? el.getAsJsonObject() : null;
            JsonObject entry = new JsonObject();
            entry.addProperty("index", i);

            if (step == null || !step.has("action") || !step.get("action").isJsonPrimitive()) {
                entry.addProperty("status", "ERROR");
                entry.addProperty("error", "step missing 'action' field");
                results.add(entry);
                continue;
            }
            String actionId = step.get("action").getAsString();
            JsonObject p = step.has("params") && step.get("params").isJsonObject()
                ? step.getAsJsonObject("params") : new JsonObject();

            entry.addProperty("action", actionId);

            if ("batch".equals(actionId)) {
                entry.addProperty("status", "ERROR");
                entry.addProperty("error", "Nested batch is not allowed");
                results.add(entry);
                continue;
            }

            ActionExecutor sub = dispatcher.registry().find(actionId);
            if (sub == null) {
                entry.addProperty("status", "ERROR");
                entry.addProperty("error", "Unknown action: " + actionId);
                results.add(entry);
                continue;
            }

            try {
                ActionResult r = sub.execute(p);
                if (r == null) {
                    entry.addProperty("status", "ERROR");
                    entry.addProperty("error", "null ActionResult");
                } else {
                    switch (r.kind()) {
                        case SUCCESS:
                            entry.addProperty("status", "SUCCESS");
                            if (r.data() != null) entry.add("data", r.data());
                            break;
                        case ASYNC:
                            entry.addProperty("status", "ASYNC");
                            entry.addProperty("task_id", r.taskId());
                            break;
                        case ERROR:
                        default:
                            entry.addProperty("status", "ERROR");
                            entry.addProperty("error_code", r.errorCode() != null ? r.errorCode().name() : "UNKNOWN");
                            entry.addProperty("error", r.message() != null ? r.message() : "");
                    }
                }
            } catch (Throwable t) {
                entry.addProperty("status", "ERROR");
                entry.addProperty("error", t.getClass().getSimpleName() + ": " + t.getMessage());
            }
            results.add(entry);
        }

        JsonObject data = new JsonObject();
        data.add("results", results);
        data.addProperty("count", actions.size());
        return ActionResult.ok(data);
    }
}
