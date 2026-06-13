package ai.herald.clientmod.catalog;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sanity check the catalog itself. We cannot import ActionRegistration here
 * because it pulls in Minecraft classes — but we can verify the catalog has
 * the expected size and no duplicates.
 */
class ActionCatalogTest {

    @Test
    void idsReturnsAtLeast100() {
        Set<String> ids = ActionCatalog.ids();
        assertTrue(ids.size() >= 100,
            "Catalog should declare at least 100 actions, got " + ids.size());
        assertEquals(ids.size(), ActionCatalog.size());
    }

    @Test
    void paramsKnownForEveryId() {
        for (String id : ActionCatalog.ids()) {
            assertNotNull(ActionCatalog.params(id),
                "params missing for catalog id: " + id);
            assertTrue(ActionCatalog.contains(id));
        }
    }

    @Test
    void coreMvpIdsPresent() {
        Set<String> ids = ActionCatalog.ids();
        for (String core : new String[] {
                "chat_message", "chat_command", "jump", "sneak_start", "sneak_stop",
                "player_move", "player_look", "place_block", "use_item", "attack_entity",
                "batch", "wait", "navigate_to", "query_player_state"
        }) {
            assertTrue(ids.contains(core), "Catalog missing core action: " + core);
        }
    }
}
