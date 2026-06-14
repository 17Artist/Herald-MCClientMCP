package ai.herald.clientmod.testing;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Static registry of active test runs. Actions in {@code ai.herald.clientmod.action.test}
 * use this to track test lifecycle (begin → checkpoint → end).
 */
public final class TestContext {

    private static final Map<String, TestRun> ACTIVE_TESTS = new ConcurrentHashMap<>();

    private TestContext() {}

    public static void begin(String testId, String description) {
        ACTIVE_TESTS.put(testId, new TestRun(testId, description, System.currentTimeMillis()));
    }

    public static TestRun end(String testId) {
        return ACTIVE_TESTS.remove(testId);
    }

    public static void addCheckpoint(String testId, String label, JsonObject data) {
        TestRun run = ACTIVE_TESTS.get(testId);
        if (run != null) run.addCheckpoint(label, data);
    }

    public static TestRun get(String testId) {
        return ACTIVE_TESTS.get(testId);
    }

    public static class TestRun {
        public final String testId;
        public final String description;
        public final long startTime;
        public final List<Checkpoint> checkpoints = new ArrayList<>();

        public TestRun(String testId, String description, long startTime) {
            this.testId = testId;
            this.description = description;
            this.startTime = startTime;
        }

        public synchronized void addCheckpoint(String label, JsonObject data) {
            checkpoints.add(new Checkpoint(label, System.currentTimeMillis(), data));
        }

        public synchronized int checkpointCount() {
            return checkpoints.size();
        }
    }

    public static class Checkpoint {
        public final String label;
        public final long timestamp;
        public final JsonObject data;

        public Checkpoint(String label, long timestamp, JsonObject data) {
            this.label = label;
            this.timestamp = timestamp;
            this.data = data;
        }
    }
}
