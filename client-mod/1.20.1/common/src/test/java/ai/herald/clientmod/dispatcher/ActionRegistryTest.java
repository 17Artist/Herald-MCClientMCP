package ai.herald.clientmod.dispatcher;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ActionRegistryTest {

    private static ActionExecutor nop() {
        return (JsonObject p) -> ActionResult.ok();
    }

    @Test
    void registerAndFindRoundtrip() {
        ActionRegistry r = new ActionRegistry();
        r.register("foo", nop());
        assertNotNull(r.find("foo"));
        assertTrue(r.contains("foo"));
        assertEquals(1, r.size());
    }

    @Test
    void duplicateRegistrationThrows() {
        ActionRegistry r = new ActionRegistry();
        r.register("foo", nop());
        assertThrows(IllegalStateException.class, () -> r.register("foo", nop()));
    }

    @Test
    void registerAfterFreezeThrows() {
        ActionRegistry r = new ActionRegistry();
        r.freeze(Set.of());
        assertThrows(IllegalStateException.class, () -> r.register("foo", nop()));
    }

    @Test
    void nullOrEmptyArgumentsRejected() {
        ActionRegistry r = new ActionRegistry();
        assertThrows(IllegalArgumentException.class, () -> r.register(null, nop()));
        assertThrows(IllegalArgumentException.class, () -> r.register("", nop()));
        assertThrows(IllegalArgumentException.class, () -> r.register("foo", null));
    }

    @Test
    void freezeIsIdempotent() {
        ActionRegistry r = new ActionRegistry();
        r.register("foo", nop());
        r.freeze(Set.of("foo"));
        r.freeze(Set.of("foo"));
        assertTrue(r.isFrozen());
    }

    @Test
    void findReturnsNullForMissing() {
        ActionRegistry r = new ActionRegistry();
        assertNull(r.find("missing"));
        assertFalse(r.contains("missing"));
    }
}
