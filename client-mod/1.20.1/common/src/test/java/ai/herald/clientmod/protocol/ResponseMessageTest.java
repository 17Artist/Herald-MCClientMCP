package ai.herald.clientmod.protocol;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResponseMessageTest {

    @Test
    void successSerialisesWithDataField() {
        JsonObject data = new JsonObject();
        data.addProperty("echoed", "Hello");
        JsonObject json = ResponseMessage.success(data).toJson();

        assertEquals("success", json.get("status").getAsString());
        assertTrue(json.has("data"));
        assertEquals("Hello", json.getAsJsonObject("data").get("echoed").getAsString());
        assertFalse(json.has("task_id"));
        assertFalse(json.has("error"));
    }

    @Test
    void asyncSerialisesWithTaskId() {
        JsonObject json = ResponseMessage.async("a3f2c891").toJson();
        assertEquals("async", json.get("status").getAsString());
        assertEquals("a3f2c891", json.get("task_id").getAsString());
        assertFalse(json.has("data"));
        assertFalse(json.has("error"));
    }

    @Test
    void errorSerialisesWithCodeAndMessage() {
        JsonObject json = ResponseMessage
            .error(ErrorCode.ACTION_NOT_FOUND, "Unknown action: foo")
            .toJson();

        assertEquals("error", json.get("status").getAsString());
        JsonObject err = json.getAsJsonObject("error");
        assertEquals("ACTION_NOT_FOUND", err.get("code").getAsString());
        assertEquals("Unknown action: foo", err.get("message").getAsString());
        assertFalse(json.has("data"));
        assertFalse(json.has("task_id"));
    }

    @Test
    void httpStatusMatchesErrorCode() {
        assertEquals(401, ResponseMessage.error(ErrorCode.TOKEN_INVALID, "x").httpStatus());
        assertEquals(404, ResponseMessage.error(ErrorCode.ACTION_NOT_FOUND, "x").httpStatus());
        assertEquals(400, ResponseMessage.error(ErrorCode.INVALID_PARAMS, "x").httpStatus());
        assertEquals(500, ResponseMessage.error(ErrorCode.NOT_IN_GAME, "x").httpStatus());
        assertEquals(500, ResponseMessage.error(ErrorCode.MAINTHREAD_FAILURE, "x").httpStatus());
        assertEquals(200, ResponseMessage.success().httpStatus());
        assertEquals(200, ResponseMessage.async("t").httpStatus());
    }

    @Test
    void asyncRejectsEmptyTaskId() {
        assertThrows(IllegalArgumentException.class, () -> ResponseMessage.async(""));
        assertThrows(IllegalArgumentException.class, () -> ResponseMessage.async(null));
    }

    @Test
    void errorRejectsNullCode() {
        assertThrows(IllegalArgumentException.class, () -> ResponseMessage.error(null, "x"));
    }
}
