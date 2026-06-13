package ai.herald.clientmod.action.test;

import ai.herald.clientmod.HeraldClientMod;
import ai.herald.clientmod.dispatcher.ActionExecutor;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.events.ChatHistoryBuffer;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.util.JsonUtil;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Sync check: search recent chat buffer for a message matching the pattern.
 * Returns ok with message data if found, error ASSERTION_FAILED if not.
 */
public final class WaitChatMessageAction implements ActionExecutor {

    @Override
    public ActionResult execute(JsonObject params) {
        String patternStr = JsonUtil.requireString(params, "pattern");
        int timeoutMs = JsonUtil.getIntOrDefault(params, "timeout", 5000);
        long since = System.currentTimeMillis() - timeoutMs;

        Pattern regex;
        try {
            regex = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
            // Fall back to literal contains matching
            regex = null;
        }

        List<ChatHistoryBuffer.Entry> messages = HeraldClientMod.chatHistory().snapshot(200);

        for (ChatHistoryBuffer.Entry entry : messages) {
            if (entry.timestampMs < since) continue;
            boolean matches = regex != null
                    ? regex.matcher(entry.text).find()
                    : entry.text.contains(patternStr);
            if (matches) {
                JsonObject data = new JsonObject();
                data.addProperty("found", true);
                data.addProperty("pattern", patternStr);
                data.addProperty("text", entry.text);
                data.addProperty("timestamp_ms", entry.timestampMs);
                return ActionResult.ok(data);
            }
        }

        return ActionResult.error(ErrorCode.ASSERTION_FAILED,
                "Chat message not found matching: " + patternStr);
    }
}
