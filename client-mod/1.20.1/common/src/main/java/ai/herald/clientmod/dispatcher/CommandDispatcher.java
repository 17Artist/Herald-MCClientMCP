package ai.herald.clientmod.dispatcher;

import ai.herald.clientmod.protocol.CommandMessage;
import ai.herald.clientmod.protocol.ErrorCode;
import ai.herald.clientmod.protocol.HeraldException;
import ai.herald.clientmod.protocol.ResponseMessage;
import ai.herald.clientmod.util.HeraldLogger;
import org.slf4j.Logger;

/**
 * Routes a {@link CommandMessage} from the HTTP thread to the Minecraft
 * client thread, invokes the matching {@link ActionExecutor}, catches
 * failures, and publishes a {@link ResponseMessage} onto the per-command
 * future.
 *
 * <p>Design mirrors BlackBoxPro {@code RuntimeCommandDispatcher.kt} but
 * simplified: no delay queue (use skill-engine for that), no safety
 * gate (Herald trusts its single caller).
 */
public final class CommandDispatcher {

    private static final Logger LOG = HeraldLogger.of(CommandDispatcher.class);

    private final ActionRegistry registry;
    private final ResponseFutureRegistry futures;
    private final MainThreadExecutor mainThread;

    public CommandDispatcher(ActionRegistry registry,
                             ResponseFutureRegistry futures,
                             MainThreadExecutor mainThread) {
        this.registry = registry;
        this.futures = futures;
        this.mainThread = mainThread;
    }

    /** Non-blocking: hands the command off to the client thread. */
    public void dispatch(CommandMessage cmd) {
        ActionExecutor executor = registry.find(cmd.action());
        if (executor == null) {
            futures.complete(cmd.id(),
                ResponseMessage.error(ErrorCode.ACTION_NOT_FOUND, "Unknown action: " + cmd.action()));
            return;
        }

        mainThread.execute(() -> runOnMainThread(cmd, executor));
    }

    /** Called by async actions to publish their deferred outcome. */
    public void publish(String commandId, ResponseMessage response) {
        futures.complete(commandId, response);
    }

    /**
     * Expose the registry for composite/batch actions that need to look up
     * peer executors and run them synchronously on the same client thread.
     */
    public ActionRegistry registry() {
        return registry;
    }

    private void runOnMainThread(CommandMessage cmd, ActionExecutor executor) {
        ResponseMessage response;
        try {
            ActionResult result = executor.execute(cmd.params());
            if (result == null) {
                response = ResponseMessage.error(ErrorCode.MAINTHREAD_FAILURE,
                    "Action " + cmd.action() + " returned null ActionResult");
            } else {
                switch (result.kind()) {
                    case SUCCESS:
                        response = ResponseMessage.success(result.data());
                        break;
                    case ASYNC:
                        response = ResponseMessage.async(result.taskId());
                        break;
                    case ERROR:
                        response = ResponseMessage.error(result.errorCode(), result.message());
                        break;
                    default:
                        response = ResponseMessage.error(ErrorCode.MAINTHREAD_FAILURE,
                            "Unknown ActionResult.Kind: " + result.kind());
                }
            }
        } catch (HeraldException e) {
            LOG.warn("Action {} raised HeraldException {}: {}", cmd.action(), e.code(), e.getMessage());
            response = ResponseMessage.error(e.code(), e.getMessage());
        } catch (IllegalArgumentException e) {
            LOG.warn("Action {} invalid params: {}", cmd.action(), e.getMessage());
            response = ResponseMessage.error(ErrorCode.INVALID_PARAMS,
                e.getMessage() != null ? e.getMessage() : "Invalid params");
        } catch (Throwable t) {
            LOG.error("Action {} threw on main thread", cmd.action(), t);
            response = ResponseMessage.error(ErrorCode.MAINTHREAD_FAILURE,
                t.getClass().getSimpleName() + ": " + t.getMessage());
        }
        futures.complete(cmd.id(), response);
    }
}
