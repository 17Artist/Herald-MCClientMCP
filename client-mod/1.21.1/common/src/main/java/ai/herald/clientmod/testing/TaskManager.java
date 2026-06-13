package ai.herald.clientmod.testing;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class TaskManager {
    private static final Map<String, TaskEntry> TASKS = new ConcurrentHashMap<>();

    public static String create(String name, JsonArray steps, String failurePolicy) {
        String id = name + "_" + UUID.randomUUID().toString().substring(0, 6);
        TaskEntry entry = new TaskEntry(id, name, steps, failurePolicy);
        TASKS.put(id, entry);
        return id;
    }

    public static TaskEntry get(String taskId) { return TASKS.get(taskId); }
    public static TaskEntry remove(String taskId) { return TASKS.remove(taskId); }

    public static List<TaskEntry> list(String statusFilter) {
        List<TaskEntry> result = new ArrayList<>();
        for (TaskEntry e : TASKS.values()) {
            if (statusFilter == null || e.status.equals(statusFilter)) result.add(e);
        }
        return result;
    }

    public static class TaskEntry {
        public final String id;
        public final String name;
        public final JsonArray steps;
        public final String failurePolicy;
        public volatile String status = "pending";
        public volatile int currentStep = 0;
        public final long createdAt = System.currentTimeMillis();
        public volatile long updatedAt = System.currentTimeMillis();
        public volatile String error;

        public TaskEntry(String id, String name, JsonArray steps, String failurePolicy) {
            this.id = id; this.name = name; this.steps = steps; this.failurePolicy = failurePolicy;
        }

        public JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", id);
            obj.addProperty("name", name);
            obj.addProperty("status", status);
            obj.addProperty("currentStep", currentStep);
            obj.addProperty("totalSteps", steps.size());
            obj.addProperty("failurePolicy", failurePolicy);
            obj.addProperty("createdAt", createdAt);
            obj.addProperty("updatedAt", updatedAt);
            if (error != null) obj.addProperty("error", error);
            return obj;
        }
    }
}
