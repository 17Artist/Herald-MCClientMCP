package ai.herald.clientmod.stream;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallbackI;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.lwjgl.glfw.GLFWMouseButtonCallbackI;
import org.lwjgl.glfw.GLFWScrollCallbackI;
import org.lwjgl.glfw.GLFWCharCallbackI;

/**
 * Receives input events from the browser WebSocket and injects them
 * into Minecraft via GLFW callback simulation. Runs on the client thread.
 */
public final class RemoteInputHandler implements StreamServer.InputHandler {

    @Override
    public void onInput(String jsonMessage) {
        try {
            JsonObject msg = JsonParser.parseString(jsonMessage).getAsJsonObject();
            String type = msg.get("type").getAsString();
            Minecraft mc = Minecraft.getInstance();
            long window = mc.getWindow().getWindow();

            mc.execute(() -> {
                switch (type) {
                    case "mousemove": {
                        double x = msg.get("x").getAsDouble();
                        double y = msg.get("y").getAsDouble();
                        // Scale from stream coordinates to actual window size
                        double scaleX = (double) mc.getWindow().getScreenWidth() / msg.get("sw").getAsDouble();
                        double scaleY = (double) mc.getWindow().getScreenHeight() / msg.get("sh").getAsDouble();
                        // Invoke the GLFW cursor pos callback directly
                        GLFWCursorPosCallbackI cb = GLFW.glfwSetCursorPosCallback(window, null);
                        if (cb != null) {
                            GLFW.glfwSetCursorPosCallback(window, cb);
                            cb.invoke(window, x * scaleX, y * scaleY);
                        }
                        break;
                    }
                    case "mousedown": {
                        int button = msg.get("button").getAsInt();
                        GLFWMouseButtonCallbackI cb = GLFW.glfwSetMouseButtonCallback(window, null);
                        if (cb != null) {
                            GLFW.glfwSetMouseButtonCallback(window, cb);
                            cb.invoke(window, button, GLFW.GLFW_PRESS, 0);
                        }
                        break;
                    }
                    case "mouseup": {
                        int button = msg.get("button").getAsInt();
                        GLFWMouseButtonCallbackI cb = GLFW.glfwSetMouseButtonCallback(window, null);
                        if (cb != null) {
                            GLFW.glfwSetMouseButtonCallback(window, cb);
                            cb.invoke(window, button, GLFW.GLFW_RELEASE, 0);
                        }
                        break;
                    }
                    case "scroll": {
                        double dx = msg.has("dx") ? msg.get("dx").getAsDouble() : 0;
                        double dy = msg.get("dy").getAsDouble();
                        GLFWScrollCallbackI cb = GLFW.glfwSetScrollCallback(window, null);
                        if (cb != null) {
                            GLFW.glfwSetScrollCallback(window, cb);
                            cb.invoke(window, dx, dy);
                        }
                        break;
                    }
                    case "keydown": {
                        int keyCode = msg.get("keyCode").getAsInt();
                        int scanCode = msg.has("scanCode") ? msg.get("scanCode").getAsInt() : 0;
                        GLFWKeyCallbackI cb = GLFW.glfwSetKeyCallback(window, null);
                        if (cb != null) {
                            GLFW.glfwSetKeyCallback(window, cb);
                            cb.invoke(window, keyCode, scanCode, GLFW.GLFW_PRESS, 0);
                        }
                        break;
                    }
                    case "keyup": {
                        int keyCode = msg.get("keyCode").getAsInt();
                        int scanCode = msg.has("scanCode") ? msg.get("scanCode").getAsInt() : 0;
                        GLFWKeyCallbackI cb = GLFW.glfwSetKeyCallback(window, null);
                        if (cb != null) {
                            GLFW.glfwSetKeyCallback(window, cb);
                            cb.invoke(window, keyCode, scanCode, GLFW.GLFW_RELEASE, 0);
                        }
                        break;
                    }
                    case "char": {
                        int codePoint = msg.get("codePoint").getAsInt();
                        GLFWCharCallbackI cb = GLFW.glfwSetCharCallback(window, null);
                        if (cb != null) {
                            GLFW.glfwSetCharCallback(window, cb);
                            cb.invoke(window, codePoint);
                        }
                        break;
                    }
                }
            });
        } catch (Exception e) {
            // Ignore malformed input
        }
    }
}
