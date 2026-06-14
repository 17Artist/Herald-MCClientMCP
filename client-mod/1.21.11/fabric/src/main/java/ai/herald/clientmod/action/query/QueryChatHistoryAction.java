package ai.herald.clientmod.action.query;

import ai.herald.clientmod.HeraldClientMod;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.events.ChatHistoryBuffer;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

/**
 * Port of BlackBoxPro QueryChatHistoryAction.kt to Java + Mojang 1.20.1.
 * Reads from {@link ChatHistoryBuffer} populated by {@code GameEventBridge}.
 * Vanilla {@code ChatComponent.allMessages} stays inaccessible — Herald keeps
 * its own ring buffer of all incoming chat / system messages since join.
 */
public final class QueryChatHistoryAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        int count = JsonUtil.getIntOrDefault(params, "count", 50);
        long since = JsonUtil.getLongOrDefault(params, "since", 0L);
        String filter = JsonUtil.getStringOrDefault(params, "filter", null);

        List<ChatHistoryBuffer.Entry> snap = HeraldClientMod.chatHistory().snapshot(count);

        JsonArray messages = new JsonArray();
        int matched = 0;
        for (ChatHistoryBuffer.Entry e : snap) {
            if (since > 0 && e.timestampMs < since) continue;
            if (filter != null && !e.text.contains(filter)) continue;
            JsonObject m = new JsonObject();
            m.addProperty("text", e.text);
            m.addProperty("timestamp_ms", e.timestampMs);
            messages.add(m);
            matched++;
        }

        JsonObject data = new JsonObject();
        data.add("messages", messages);
        data.addProperty("count", matched);
        data.addProperty("available", true);
        data.addProperty("buffer_size", snap.size());
        return ActionResult.ok(data);
    }
}
