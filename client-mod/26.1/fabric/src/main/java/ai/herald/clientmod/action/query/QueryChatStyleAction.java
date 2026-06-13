package ai.herald.clientmod.action.query;

import ai.herald.clientmod.HeraldClientMod;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.events.ChatHistoryBuffer;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonObject;

import java.util.List;

/**
 * Port of BlackBoxPro QueryChatStyleAction.kt to Java + Mojang 1.20.1.
 * Searches Herald's chat history (see {@link ChatHistoryBuffer}) for the
 * {@code index}-th message containing {@code match} and returns its plain text.
 * Click-event metadata is unavailable on 1.20.1 (vanilla strips it before
 * surfacing to mods), so {@code style.click_event} is always null.
 */
public final class QueryChatStyleAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        String match = JsonUtil.requireString(params, "match");
        int wantedIndex = JsonUtil.getIntOrDefault(params, "index", 0);

        List<ChatHistoryBuffer.Entry> snap = HeraldClientMod.chatHistory().snapshot(0);
        int seen = 0;
        ChatHistoryBuffer.Entry hit = null;
        for (ChatHistoryBuffer.Entry e : snap) {
            if (e.text.contains(match)) {
                if (seen == wantedIndex) { hit = e; break; }
                seen++;
            }
        }

        JsonObject data = new JsonObject();
        data.addProperty("available", true);
        data.addProperty("query", match);
        data.addProperty("index", wantedIndex);
        if (hit == null) {
            data.addProperty("matched", false);
        } else {
            data.addProperty("matched", true);
            data.addProperty("text", hit.text);
            data.addProperty("timestamp_ms", hit.timestampMs);
            JsonObject style = new JsonObject();
            style.add("click_event", null);   // not exposed by vanilla 1.20.1 chat path
            style.add("hover_event", null);
            data.add("style", style);
        }
        return ActionResult.ok(data);
    }
}
