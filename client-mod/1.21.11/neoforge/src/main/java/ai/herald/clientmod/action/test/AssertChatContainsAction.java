package ai.herald.clientmod.action.test;

import ai.herald.clientmod.HeraldClientMod;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.events.ChatHistoryBuffer;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonObject;

import java.util.List;

/**
 * Asserts that a recent chat message contains the given pattern string.
 * Searches the last N messages (default 10) in the chat history buffer.
 */
public final class AssertChatContainsAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        String pattern = JsonUtil.requireString(params, "pattern");
        int within = JsonUtil.getIntOrDefault(params, "within", 10);

        List<ChatHistoryBuffer.Entry> recent = HeraldClientMod.chatHistory().snapshot(within);

        for (ChatHistoryBuffer.Entry entry : recent) {
            if (entry.text.contains(pattern)) {
                JsonObject data = new JsonObject();
                data.addProperty("pass", true);
                data.addProperty("message", "Found pattern '" + pattern + "' in chat: " + entry.text);
                return ActionResult.ok(data);
            }
        }

        return ActionResult.error(ErrorCode.ASSERTION_FAILED,
            "Pattern '" + pattern + "' not found in last " + within + " chat messages");
    }
}
