package ai.herald.clientmod.skill;

import com.google.gson.JsonObject;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Tracks long-running async tasks. Phase 1 ships the engine but no
 * action consumer yet; the {@code /skill/<id>} endpoints exercise this
 * end-to-end via integration tests.
 */
public final class SkillEngine {

    private final ConcurrentMap<String, SkillTask> tasks = new ConcurrentHashMap<>();

    /** Create a new task and return its handle. */
    public SkillTask create(String actionId) {
        return create(actionId, null);
    }

    /** Create a new task with parameters (retained for introspection). */
    public SkillTask create(String actionId, JsonObject params) {
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        SkillTask task = new SkillTask(id, actionId, params);
        tasks.put(id, task);
        return task;
    }

    public SkillTask get(String taskId) {
        return tasks.get(taskId);
    }

    public boolean isCancelled(String taskId) {
        SkillTask t = tasks.get(taskId);
        return t != null && t.isCancelled();
    }

    public boolean isRunning(String taskId) {
        SkillTask t = tasks.get(taskId);
        return t != null && t.isRunning();
    }

    public boolean complete(String taskId, JsonObject result) {
        SkillTask t = tasks.get(taskId);
        return t != null && t.complete(result);
    }

    public boolean fail(String taskId, String message) {
        SkillTask t = tasks.get(taskId);
        return t != null && t.fail(message);
    }

    public boolean cancel(String taskId) {
        SkillTask t = tasks.get(taskId);
        return t != null && t.cancel();
    }

    public int size() {
        return tasks.size();
    }

    /** Drop tasks that finished more than {@code ttlMs} ago. */
    public int prune(long ttlMs) {
        long now = System.currentTimeMillis();
        int removed = 0;
        for (var entry : tasks.entrySet()) {
            SkillTask t = entry.getValue();
            if (t.isTerminal() && (now - t.createdAtMs()) > ttlMs) {
                if (tasks.remove(entry.getKey(), t)) removed++;
            }
        }
        return removed;
    }
}
