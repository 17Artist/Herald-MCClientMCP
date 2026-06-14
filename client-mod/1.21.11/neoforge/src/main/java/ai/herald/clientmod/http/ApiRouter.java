package ai.herald.clientmod.http;

import ai.herald.clientmod.HeraldConstants;
import ai.herald.clientmod.dispatcher.ActionRegistry;
import ai.herald.clientmod.dispatcher.CommandDispatcher;
import ai.herald.clientmod.dispatcher.ResponseFutureRegistry;
import ai.herald.clientmod.protocol.CommandMessage;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.protocol.HttpEndpoints;
import ai.herald.clientmod.protocol.ResponseMessage;
import ai.herald.clientmod.util.HeraldLogger;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * Handler for the synchronous endpoints: {@code /ping}, {@code /actions},
 * {@code /action/<id>}, {@code /shutdown}.
 */
public final class ApiRouter implements HttpHandler {

    private static final Logger LOG = HeraldLogger.of(ApiRouter.class);

    private final ActionRegistry registry;
    private final CommandDispatcher dispatcher;
    private final ResponseFutureRegistry futures;
    private final String loaderName;
    private final Runnable shutdownHook;

    public ApiRouter(ActionRegistry registry,
                     CommandDispatcher dispatcher,
                     ResponseFutureRegistry futures,
                     String loaderName,
                     Runnable shutdownHook) {
        this.registry = registry;
        this.dispatcher = dispatcher;
        this.futures = futures;
        this.loaderName = loaderName;
        this.shutdownHook = shutdownHook;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
        try {
            if (path.equals(HttpEndpoints.PING) && method.equals("GET")) {
                handlePing(exchange);
            } else if (path.equals(HttpEndpoints.ACTIONS) && method.equals("GET")) {
                handleActions(exchange);
            } else if (path.startsWith(HttpEndpoints.ACTION_PREFIX) && method.equals("POST")) {
                handleAction(exchange, path.substring(HttpEndpoints.ACTION_PREFIX.length()));
            } else if (path.equals(HttpEndpoints.SHUTDOWN) && method.equals("POST")) {
                handleShutdown(exchange);
            } else {
                writeError(exchange, ErrorCode.ACTION_NOT_FOUND, "No handler for " + method + " " + path);
            }
        } catch (Throwable t) {
            LOG.error("ApiRouter crashed on {} {}", method, path, t);
            writeError(exchange, ErrorCode.MAINTHREAD_FAILURE, t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    private void handlePing(HttpExchange exchange) throws IOException {
        JsonObject data = new JsonObject();
        data.addProperty("ok", true);
        data.addProperty("mod_version", HeraldConstants.MOD_VERSION);
        data.addProperty("mc_version", HeraldConstants.MC_VERSION);
        data.addProperty("loader", loaderName);
        data.addProperty("registered_actions", registry.size());
        HttpResponses.writeJson(exchange, 200, ResponseMessage.success(data).toJson());
    }

    private void handleActions(HttpExchange exchange) throws IOException {
        JsonArray arr = new JsonArray();
        for (String id : registry.registeredIds()) arr.add(id);
        JsonObject data = new JsonObject();
        data.add("actions", arr);
        HttpResponses.writeJson(exchange, 200, ResponseMessage.success(data).toJson());
    }

    private void handleAction(HttpExchange exchange, String actionId) throws IOException {
        if (actionId.isEmpty()) {
            writeError(exchange, ErrorCode.ACTION_NOT_FOUND, "Empty action id");
            return;
        }
        if (!registry.contains(actionId)) {
            writeError(exchange, ErrorCode.ACTION_NOT_FOUND, "Unknown action: " + actionId);
            return;
        }

        JsonObject params;
        String raw = HttpResponses.readBody(exchange);
        if (raw.isEmpty()) {
            params = new JsonObject();
        } else {
            try {
                params = JsonParser.parseString(raw).getAsJsonObject();
            } catch (Exception e) {
                writeError(exchange, ErrorCode.INVALID_PARAMS, "Malformed JSON body: " + e.getMessage());
                return;
            }
        }

        String commandId = UUID.randomUUID().toString();
        var fut = futures.register(commandId);
        dispatcher.dispatch(new CommandMessage(commandId, actionId, params));

        ResponseMessage response;
        try {
            response = futures.await(commandId, HeraldConstants.ACTION_TIMEOUT_MS);
        } catch (TimeoutException e) {
            response = ResponseMessage.error(ErrorCode.MAINTHREAD_FAILURE,
                "Action timed out after " + HeraldConstants.ACTION_TIMEOUT_MS + "ms");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            response = ResponseMessage.error(ErrorCode.MAINTHREAD_FAILURE, "Interrupted");
        }
        HttpResponses.writeJson(exchange, response.httpStatus(), response.toJson());
    }

    private void handleShutdown(HttpExchange exchange) throws IOException {
        JsonObject data = new JsonObject();
        data.addProperty("ok", true);
        HttpResponses.writeJson(exchange, 200, ResponseMessage.success(data).toJson());
        if (shutdownHook != null) {
            // Defer so the response finishes writing first.
            new Thread(shutdownHook, "Herald-Shutdown-Trigger").start();
        }
    }

    private static void writeError(HttpExchange exchange, ErrorCode code, String message) throws IOException {
        ResponseMessage msg = ResponseMessage.error(code, message);
        HttpResponses.writeJson(exchange, msg.httpStatus(), msg.toJson());
    }
}
