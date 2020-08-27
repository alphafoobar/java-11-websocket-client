import ws.WebSocketClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Application {

    private static final CountDownLatch latch = new CountDownLatch(1);

    public static void main(String[] args) throws Exception {
        connect();
        latch.await();
    }

    private static boolean connect() {
        System.out.println("Connecting... ");
        WebSocket ws = HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .connectTimeout(Duration.of(5, ChronoUnit.SECONDS))
                .buildAsync(
                        URI.create("ws://localhost:8099/"),
                        new WebSocketClient(Application::onNext, Application::onError, Application::onClose))
                .orTimeout(5, TimeUnit.SECONDS)
                .join();

        ws.sendText("{\n" +
                "    \"type\": \"subscribe\",\n" +
                "    \"product_ids\": [\n" +
                "        \"ETH-USD\",\n" +
                "        \"ETH-EUR\"\n" +
                "    ],\n" +
                "    \"channels\": [\n" +
                "        \"level2\",\n" +
                "        \"heartbeat\",\n" +
                "        {\n" +
                "            \"name\": \"ticker\",\n" +
                "            \"product_ids\": [\n" +
                "                \"ETH-BTC\",\n" +
                "                \"ETH-USD\"\n" +
                "            ]\n" +
                "        }\n" +
                "    ]\n" +
                "}", true);
        return true;
    }

    private static void onNext(String message) {
        System.out.println("text received " + message);
    }

    private static void onError(Throwable error) {
        System.out.println("Bad day! " + error.toString());
    }

    private static void onClose(Integer status, String reason) {
        System.out.println("Close status=" + status + ", reason=" + reason);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        boolean connected = false;
        long backoff = 250;
        long maxBackoff = 120000;
        while (!connected) {
            try {
                connected = connect();
            } catch (Exception e) {
                onError(e);
            }

            try {
                long chosenBackoff = Math.min(backoff, maxBackoff);
                System.out.println("Waiting " + chosenBackoff);
                Thread.sleep(chosenBackoff);
                backoff *= 2;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
