package ai.herald.clientmod.dispatcher;

import ai.herald.clientmod.protocol.ResponseMessage;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Pairs an in-flight command id with a {@link CompletableFuture} that the
 * HTTP request thread blocks on. Completed by the dispatcher (or async
 * actions) when the action finishes.
 */
public final class ResponseFutureRegistry {

    private final Map<String, CompletableFuture<ResponseMessage>> futures = new ConcurrentHashMap<>();

    public CompletableFuture<ResponseMessage> register(String commandId) {
        CompletableFuture<ResponseMessage> fut = new CompletableFuture<>();
        CompletableFuture<ResponseMessage> prev = futures.putIfAbsent(commandId, fut);
        return prev != null ? prev : fut;
    }

    public void complete(String commandId, ResponseMessage response) {
        CompletableFuture<ResponseMessage> fut = futures.remove(commandId);
        if (fut != null) {
            fut.complete(response);
        }
    }

    public void cancel(String commandId) {
        CompletableFuture<ResponseMessage> fut = futures.remove(commandId);
        if (fut != null) {
            fut.cancel(true);
        }
    }

    public ResponseMessage await(String commandId, long timeoutMs) throws TimeoutException, InterruptedException {
        CompletableFuture<ResponseMessage> fut = futures.get(commandId);
        if (fut == null) {
            throw new IllegalStateException("No pending future for commandId: " + commandId);
        }
        try {
            return fut.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            cancel(commandId);
            throw e;
        } catch (java.util.concurrent.ExecutionException e) {
            throw new RuntimeException("Future failed", e.getCause());
        }
    }

    public int pendingCount() {
        return futures.size();
    }
}
