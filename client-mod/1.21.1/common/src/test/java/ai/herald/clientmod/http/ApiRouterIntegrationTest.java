package ai.herald.clientmod.http;

import ai.herald.clientmod.dispatcher.ActionRegistry;
import ai.herald.clientmod.dispatcher.ActionResult;
import ai.herald.clientmod.dispatcher.CommandDispatcher;
import ai.herald.clientmod.dispatcher.MainThreadExecutor;
import ai.herald.clientmod.dispatcher.ResponseFutureRegistry;
import ai.herald.clientmod.events.EventBus;
import ai.herald.clientmod.skill.SkillEngine;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Spin up a real {@link HeraldHttpServer} with an echo action and exercise
 * the public HTTP surface end-to-end. Proves:
 * <ul>
 *   <li>token filter accepts both Bearer and {@code ?token=}</li>
 *   <li>missing/invalid token → 401 + TOKEN_INVALID</li>
 *   <li>{@code /action/<id>} happy path round-trips</li>
 *   <li>unknown action → 404 + ACTION_NOT_FOUND</li>
 *   <li>{@code /ping} and {@code /actions} return success envelope</li>
 * </ul>
 */
class ApiRouterIntegrationTest {

    private HeraldHttpServer server;
    private String token;
    private int port;
    private final HttpClient client = HttpClient.newBuilder().build();

    @BeforeEach
    void setUp() throws IOException {
        ActionRegistry registry = new ActionRegistry();
        // Echo action: returns {"echoed": <message>}
        registry.register("echo", (JsonObject params) -> {
            String msg = params.has("message") ? params.get("message").getAsString() : "";
            JsonObject data = new JsonObject();
            data.addProperty("echoed", msg);
            return ActionResult.ok(data);
        });
        registry.freeze(Set.of("echo"));

        ResponseFutureRegistry futures = new ResponseFutureRegistry();
        // Run "main thread" on a worker for the test.
        MainThreadExecutor mainThread = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "test-main-thread");
            t.setDaemon(true);
            return t;
        })::execute;
        CommandDispatcher dispatcher = new CommandDispatcher(registry, futures, mainThread);

        ApiRouter apiRouter = new ApiRouter(registry, dispatcher, futures, "test", null);
        SkillRouter skillRouter = new SkillRouter(new SkillEngine());
        EventsHandler eventsHandler = new EventsHandler(new EventBus());

        token = UUID.randomUUID().toString();
        port = PortPicker.pick(40000, 40100);
        server = new HeraldHttpServer(port, token, apiRouter, skillRouter, eventsHandler);
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) server.stop();
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    @Test
    void pingSucceedsWithBearer() throws Exception {
        HttpResponse<String> resp = client.send(
            HttpRequest.newBuilder(uri("/ping"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        JsonObject json = JsonParser.parseString(resp.body()).getAsJsonObject();
        assertEquals("success", json.get("status").getAsString());
        assertTrue(json.getAsJsonObject("data").get("ok").getAsBoolean());
    }

    @Test
    void pingSucceedsWithQueryToken() throws Exception {
        HttpResponse<String> resp = client.send(
            HttpRequest.newBuilder(uri("/ping?token=" + token)).GET().build(),
            HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
    }

    @Test
    void missingTokenReturns401AndTokenInvalid() throws Exception {
        HttpResponse<String> resp = client.send(
            HttpRequest.newBuilder(uri("/ping")).GET().build(),
            HttpResponse.BodyHandlers.ofString());
        assertEquals(401, resp.statusCode());
        JsonObject json = JsonParser.parseString(resp.body()).getAsJsonObject();
        assertEquals("error", json.get("status").getAsString());
        assertEquals("TOKEN_INVALID", json.getAsJsonObject("error").get("code").getAsString());
    }

    @Test
    void echoActionRoundTrips() throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("message", "Hello from Herald");

        HttpResponse<String> resp = client.send(
            HttpRequest.newBuilder(uri("/action/echo?token=" + token))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build(),
            HttpResponse.BodyHandlers.ofString());

        assertEquals(200, resp.statusCode());
        JsonObject json = JsonParser.parseString(resp.body()).getAsJsonObject();
        assertEquals("success", json.get("status").getAsString());
        assertEquals("Hello from Herald", json.getAsJsonObject("data").get("echoed").getAsString());
    }

    @Test
    void unknownActionReturns404() throws Exception {
        HttpResponse<String> resp = client.send(
            HttpRequest.newBuilder(uri("/action/nope?token=" + token))
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build(),
            HttpResponse.BodyHandlers.ofString());
        assertEquals(404, resp.statusCode());
        JsonObject json = JsonParser.parseString(resp.body()).getAsJsonObject();
        assertEquals("error", json.get("status").getAsString());
        assertEquals("ACTION_NOT_FOUND", json.getAsJsonObject("error").get("code").getAsString());
    }

    @Test
    void actionsListIncludesRegistered() throws Exception {
        HttpResponse<String> resp = client.send(
            HttpRequest.newBuilder(uri("/actions?token=" + token)).GET().build(),
            HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        JsonObject json = JsonParser.parseString(resp.body()).getAsJsonObject();
        var arr = json.getAsJsonObject("data").getAsJsonArray("actions");
        assertEquals(1, arr.size());
        assertEquals("echo", arr.get(0).getAsString());
    }
}
