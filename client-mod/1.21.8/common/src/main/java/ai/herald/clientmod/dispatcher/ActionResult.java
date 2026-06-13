package ai.herald.clientmod.dispatcher;

import ai.herald.clientmod.protocol.ErrorCode;
import com.google.gson.JsonObject;

/**
 * Outcome of a single action invocation. Four shapes:
 * <ul>
 *   <li>{@link #ok(JsonObject)} — immediate success with payload</li>
 *   <li>{@link #ok()} — immediate success with no payload</li>
 *   <li>{@link #error(ErrorCode, String)} — typed failure</li>
 *   <li>{@link #async(String)} — long-running; skill/{taskId} tracks it</li>
 * </ul>
 */
public final class ActionResult {

    public enum Kind { SUCCESS, ERROR, ASYNC }

    private final Kind kind;
    private final JsonObject data;
    private final ErrorCode errorCode;
    private final String message;
    private final String taskId;

    private ActionResult(Kind kind, JsonObject data, ErrorCode errorCode, String message, String taskId) {
        this.kind = kind;
        this.data = data;
        this.errorCode = errorCode;
        this.message = message;
        this.taskId = taskId;
    }

    public static ActionResult ok() {
        return new ActionResult(Kind.SUCCESS, new JsonObject(), null, null, null);
    }

    public static ActionResult ok(JsonObject data) {
        return new ActionResult(Kind.SUCCESS, data != null ? data : new JsonObject(), null, null, null);
    }

    public static ActionResult ok(String messageKey, String messageValue) {
        JsonObject d = new JsonObject();
        d.addProperty(messageKey, messageValue);
        return new ActionResult(Kind.SUCCESS, d, null, null, null);
    }

    public static ActionResult error(ErrorCode code, String message) {
        return new ActionResult(Kind.ERROR, null, code, message != null ? message : code.name(), null);
    }

    public static ActionResult async(String taskId) {
        if (taskId == null || taskId.isEmpty()) {
            throw new IllegalArgumentException("taskId must not be empty");
        }
        return new ActionResult(Kind.ASYNC, null, null, null, taskId);
    }

    public Kind kind() { return kind; }
    public JsonObject data() { return data; }
    public ErrorCode errorCode() { return errorCode; }
    public String message() { return message; }
    public String taskId() { return taskId; }

    public boolean isSuccess() { return kind == Kind.SUCCESS; }
    public boolean isError() { return kind == Kind.ERROR; }
    public boolean isAsync() { return kind == Kind.ASYNC; }
}
