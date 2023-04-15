package com.binance.connector.futures.client.utils;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;

import java.util.concurrent.atomic.AtomicBoolean;

public class WebSocketConnectionWithReconnect extends WebSocketConnection {
    private static final int RECONNECT_DELAY_MS = 30_000;
    private final AtomicBoolean reconnecting = new AtomicBoolean(false);
    public WebSocketConnectionWithReconnect(
            WebSocketCallback onOpenCallback,
            WebSocketCallback onMessageCallback,
            WebSocketCallback onClosingCallback,
            WebSocketCallback onFailureCallback,
            Request request) {

        super(onOpenCallback, onMessageCallback, onClosingCallback, onFailureCallback, request);
    }

    public void reconnect() {
        if (reconnecting.compareAndSet(false, true)) {
            new Thread(() -> {
                try {
                    Thread.sleep(RECONNECT_DELAY_MS);
                    synchronized (mutex) {
                        if (webSocket != null) {
                            webSocket.cancel();
                            webSocket = null;
                        }
                    }
                    connect();
                } catch (InterruptedException e) {
                    logger.error("[Connection {}] Reconnect interrupted", connectionId, e);
                } finally {
                    reconnecting.set(false);
                }
            }).start();
        }
    }

    @Override
    public void onFailure(WebSocket ws, Throwable t, Response response) {
        super.onFailure(ws, t, response);
        reconnect();
    }
}
