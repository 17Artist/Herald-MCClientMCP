package ai.herald.clientmod.protocol;

/**
 * Runtime exception carrying a typed {@link ErrorCode}. Thrown by action
 * implementations and HTTP handlers when they want to short-circuit normal
 * dispatch and produce a structured error response. Caught by the dispatcher
 * and serialised via {@link ResponseMessage#error(ErrorCode, String)}.
 */
public class HeraldException extends RuntimeException {

    private final ErrorCode code;

    public HeraldException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public HeraldException(ErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
