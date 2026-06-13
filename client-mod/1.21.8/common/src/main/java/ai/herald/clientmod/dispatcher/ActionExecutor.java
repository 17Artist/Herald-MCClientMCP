package ai.herald.clientmod.dispatcher;

import com.google.gson.JsonObject;

/**
 * Single-method functional interface implemented by every action class.
 * Implementations run on the Minecraft client thread (post-dispatch) and
 * MUST NOT block on I/O; long work goes through {@link ActionResult#async}
 * + the SkillEngine.
 */
@FunctionalInterface
public interface ActionExecutor {
    ActionResult execute(JsonObject params);
}
