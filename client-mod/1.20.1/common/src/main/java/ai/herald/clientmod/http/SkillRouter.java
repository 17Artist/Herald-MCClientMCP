package ai.herald.clientmod.http;

import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.protocol.HttpEndpoints;
import ai.herald.clientmod.protocol.ResponseMessage;
import ai.herald.clientmod.skill.SkillEngine;
import ai.herald.clientmod.skill.SkillTask;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Locale;

/**
 * {@code GET /skill/<taskId>} returns current state;
 * {@code POST /skill/<taskId>/cancel} transitions RUNNING → CANCELLED.
 */
public final class SkillRouter implements HttpHandler {

    private final SkillEngine engine;

    public SkillRouter(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);

        if (!path.startsWith(HttpEndpoints.SKILL_PREFIX)) {
            writeError(exchange, ErrorCode.ACTION_NOT_FOUND, "Not a skill endpoint");
            return;
        }
        String tail = path.substring(HttpEndpoints.SKILL_PREFIX.length());
        if (tail.isEmpty()) {
            writeError(exchange, ErrorCode.INVALID_PARAMS, "Missing task id");
            return;
        }

        boolean isCancel = tail.endsWith(HttpEndpoints.SKILL_CANCEL_SUFFIX);
        String taskId = isCancel
            ? tail.substring(0, tail.length() - HttpEndpoints.SKILL_CANCEL_SUFFIX.length())
            : tail;

        SkillTask task = engine.get(taskId);
        if (task == null) {
            writeError(exchange, ErrorCode.ACTION_NOT_FOUND, "Unknown task id: " + taskId);
            return;
        }

        if (isCancel) {
            if (!method.equals("POST")) {
                writeError(exchange, ErrorCode.INVALID_PARAMS, "Use POST for cancel");
                return;
            }
            boolean cancelled = engine.cancel(taskId);
            JsonObject data = new JsonObject();
            data.addProperty("ok", cancelled);
            data.addProperty("task_id", taskId);
            data.addProperty("status", task.status().name());
            HttpResponses.writeJson(exchange, 200, ResponseMessage.success(data).toJson());
        } else {
            if (!method.equals("GET")) {
                writeError(exchange, ErrorCode.INVALID_PARAMS, "Use GET for status");
                return;
            }
            HttpResponses.writeJson(exchange, 200, ResponseMessage.success(task.toJson()).toJson());
        }
    }

    private static void writeError(HttpExchange exchange, ErrorCode code, String msg) throws IOException {
        ResponseMessage r = ResponseMessage.error(code, msg);
        HttpResponses.writeJson(exchange, r.httpStatus(), r.toJson());
    }
}
