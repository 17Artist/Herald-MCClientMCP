package ai.herald.clientmod.protocol;

/**
 * The five fixed error codes from {@code S2_CLIENT_MOD_TECH.md §3.3}.
 * Wire format is the enum name verbatim (UPPER_SNAKE_CASE).
 */
public enum ErrorCode {
    /** Action id is not registered. HTTP 404. */
    ACTION_NOT_FOUND(404),
    /** Body parsing failed or required parameters missing. HTTP 400. */
    INVALID_PARAMS(400),
    /** {@code Minecraft.player} or {@code level} is null when action runs. HTTP 500. */
    NOT_IN_GAME(500),
    /** Action threw a non-IllegalArgumentException on the client thread. HTTP 500. */
    MAINTHREAD_FAILURE(500),
    /** Bearer/query token missing or mismatched. HTTP 401. */
    TOKEN_INVALID(401),
    /** Test assertion failed — expected condition was not met. HTTP 409. */
    ASSERTION_FAILED(409),
    /** Action or packet API not available on this Minecraft version. HTTP 501. */
    NOT_IMPLEMENTED(501);

    private final int httpStatus;

    ErrorCode(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public int httpStatus() {
        return httpStatus;
    }
}
