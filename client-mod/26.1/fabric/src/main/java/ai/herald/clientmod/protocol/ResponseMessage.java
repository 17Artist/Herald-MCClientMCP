package ai.herald.clientmod.protocol;

import com.google.gson.JsonObject;

/**
 * Outbound envelope returned by every HTTP endpoint.
 *
 * <p>Wire formats (see {@code S2_CLIENT_MOD_TECH.md §3.2 / §3.3}):
 * <pre>{@code
 *   {"status":"success","data":{...}}
 *   {"status":"async","task_id":"<uuid>"}
 *   {"status":"error","error":{"code":"...","message":"..."}}
 * }</pre>
 */
public final class ResponseMessage {

    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_ASYNC = "async";
    public static final String STATUS_ERROR = "error";

    private final String status;
    private final JsonObject data;      // for success
    private final String taskId;        // for async
    private final ErrorCode errorCode;  // for error
    private final String errorMessage;  // for error

    private ResponseMessage(String status, JsonObject data, String taskId, ErrorCode errorCode, String errorMessage) {
        this.status = status;
        this.data = data;
        this.taskId = taskId;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public static ResponseMessage success() {
        return new ResponseMessage(STATUS_SUCCESS, new JsonObject(), null, null, null);
    }

    public static ResponseMessage success(JsonObject data) {
        return new ResponseMessage(STATUS_SUCCESS, data != null ? data : new JsonObject(), null, null, null);
    }

    public static ResponseMessage async(String taskId) {
        if (taskId == null || taskId.isEmpty()) {
            throw new IllegalArgumentException("taskId must not be empty");
        }
        return new ResponseMessage(STATUS_ASYNC, null, taskId, null, null);
    }

    public static ResponseMessage error(ErrorCode code, String message) {
        if (code == null) {
            throw new IllegalArgumentException("error code must not be null");
        }
        return new ResponseMessage(STATUS_ERROR, null, null, code, message != null ? message : "");
    }

    public String status() { return status; }
    public JsonObject data() { return data; }
    public String taskId() { return taskId; }
    public ErrorCode errorCode() { return errorCode; }
    public String errorMessage() { return errorMessage; }

    public int httpStatus() {
        if (STATUS_ERROR.equals(status)) {
            return errorCode.httpStatus();
        }
        return 200;
    }

    /**
     * Serialise to the wire JSON. Field order is fixed to match §3.2/§3.3.
     */
    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("status", status);
        switch (status) {
            case STATUS_SUCCESS:
                obj.add("data", data);
                break;
            case STATUS_ASYNC:
                obj.addProperty("task_id", taskId);
                break;
            case STATUS_ERROR:
                JsonObject err = new JsonObject();
                err.addProperty("code", errorCode.name());
                err.addProperty("message", errorMessage);
                obj.add("error", err);
                break;
            default:
                throw new IllegalStateException("Unknown status: " + status);
        }
        return obj;
    }
}
