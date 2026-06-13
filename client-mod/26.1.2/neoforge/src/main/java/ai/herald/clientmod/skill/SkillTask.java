package ai.herald.clientmod.skill;

import com.google.gson.JsonObject;

import java.util.concurrent.atomic.AtomicReference;

/** A single long-running task, tracked by id. Thread-safe state transitions. */
public final class SkillTask {

    private final String taskId;
    private final String actionId;
    private final long createdAtMs;
    private final JsonObject params;
    private final AtomicReference<SkillStatus> status = new AtomicReference<>(SkillStatus.RUNNING);
    private volatile JsonObject result;      // set on COMPLETED
    private volatile String errorMessage;    // set on FAILED

    SkillTask(String taskId, String actionId) {
        this(taskId, actionId, null);
    }

    SkillTask(String taskId, String actionId, JsonObject params) {
        this.taskId = taskId;
        this.actionId = actionId;
        this.params = params;
        this.createdAtMs = System.currentTimeMillis();
    }

    public String taskId() { return taskId; }
    public String actionId() { return actionId; }
    public long createdAtMs() { return createdAtMs; }
    public JsonObject params() { return params; }
    public SkillStatus status() { return status.get(); }
    public JsonObject result() { return result; }
    public String errorMessage() { return errorMessage; }

    public boolean isRunning() { return status.get() == SkillStatus.RUNNING; }
    public boolean isCancelled() { return status.get() == SkillStatus.CANCELLED; }

    public boolean complete(JsonObject result) {
        if (status.compareAndSet(SkillStatus.RUNNING, SkillStatus.COMPLETED)) {
            this.result = result != null ? result : new JsonObject();
            return true;
        }
        return false;
    }

    public boolean fail(String message) {
        if (status.compareAndSet(SkillStatus.RUNNING, SkillStatus.FAILED)) {
            this.errorMessage = message != null ? message : "";
            return true;
        }
        return false;
    }

    public boolean cancel() {
        return status.compareAndSet(SkillStatus.RUNNING, SkillStatus.CANCELLED);
    }

    public boolean isTerminal() {
        return status.get() != SkillStatus.RUNNING;
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("task_id", taskId);
        obj.addProperty("action", actionId);
        obj.addProperty("status", status.get().name());
        obj.addProperty("created_at_ms", createdAtMs);
        if (result != null) obj.add("result", result);
        if (errorMessage != null) obj.addProperty("error_message", errorMessage);
        return obj;
    }
}
