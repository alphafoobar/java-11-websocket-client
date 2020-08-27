package ws;

import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class WebSocketClient implements WebSocket.Listener {

    private List<CharSequence> parts = new ArrayList<>();
    private CompletableFuture<?> accumulatedMessage = new CompletableFuture<>();

    private final BiConsumer<WebSocket, String> onNext;
    private final Consumer<Throwable> onError;
    private final BiConsumer<Integer, String> onClose;

    public WebSocketClient(BiConsumer<WebSocket, String> onNext, Consumer<Throwable> onError, BiConsumer<Integer, String> onClose) {
        this.onNext = onNext;
        this.onError = onError;
        this.onClose = onClose;
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        System.out.println("onOpen using subprotocol " + webSocket.getSubprotocol());
        WebSocket.Listener.super.onOpen(webSocket);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence message, boolean last) {
        parts.add(message);
        webSocket.request(1);
        if (last) {
            processWholeText(webSocket, parts);
            parts = new ArrayList<>();
            accumulatedMessage.complete(null);
            CompletionStage<?> cf = accumulatedMessage;
            accumulatedMessage = new CompletableFuture<>();
            return cf;
        }
        return accumulatedMessage;
    }

    private void processWholeText(WebSocket webSocket, List<CharSequence> parts) {
        StringBuilder sb = new StringBuilder();
        for (CharSequence part : parts) {
            sb.append(part);
        }
        onNext.accept(webSocket, sb.toString());
    }

    @Override
    public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
        System.out.println("onBinary received.");
        return WebSocket.Listener.super.onBinary(webSocket, data, last);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        onError.accept(error);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        onClose.accept(statusCode, reason);
        return null;
    }

    @Override
    public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
        System.out.println("Ping! " + webSocket.toString());
        return WebSocket.Listener.super.onPing(webSocket, message);
    }

    @Override
    public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
        return WebSocket.Listener.super.onPong(webSocket, message);
    }
}
