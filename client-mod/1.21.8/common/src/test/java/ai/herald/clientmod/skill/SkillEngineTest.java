package ai.herald.clientmod.skill;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SkillEngineTest {

    @Test
    void createReturnsRunningTask() {
        SkillEngine e = new SkillEngine();
        SkillTask t = e.create("pathfind_to");
        assertEquals(SkillStatus.RUNNING, t.status());
        assertEquals("pathfind_to", t.actionId());
        assertNotNull(t.taskId());
        assertSame(t, e.get(t.taskId()));
    }

    @Test
    void completeTransitionsOnceOnly() {
        SkillEngine e = new SkillEngine();
        SkillTask t = e.create("a");
        JsonObject payload = new JsonObject();
        payload.addProperty("k", "v");

        assertTrue(e.complete(t.taskId(), payload));
        assertEquals(SkillStatus.COMPLETED, t.status());
        assertEquals("v", t.result().get("k").getAsString());

        assertFalse(e.complete(t.taskId(), payload));
        assertFalse(e.fail(t.taskId(), "nope"));
    }

    @Test
    void cancelFromRunning() {
        SkillEngine e = new SkillEngine();
        SkillTask t = e.create("a");
        assertTrue(e.cancel(t.taskId()));
        assertEquals(SkillStatus.CANCELLED, t.status());
        assertFalse(e.cancel(t.taskId()));
    }

    @Test
    void failRecordsMessage() {
        SkillEngine e = new SkillEngine();
        SkillTask t = e.create("a");
        assertTrue(e.fail(t.taskId(), "oops"));
        assertEquals(SkillStatus.FAILED, t.status());
        assertEquals("oops", t.errorMessage());
    }

    @Test
    void toJsonExposesCanonicalFields() {
        SkillEngine e = new SkillEngine();
        SkillTask t = e.create("a");
        JsonObject json = t.toJson();
        assertEquals(t.taskId(), json.get("task_id").getAsString());
        assertEquals("a", json.get("action").getAsString());
        assertEquals("RUNNING", json.get("status").getAsString());
    }
}
