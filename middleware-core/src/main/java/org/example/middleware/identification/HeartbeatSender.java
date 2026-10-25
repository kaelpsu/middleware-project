package org.example.middleware.identification;

import org.example.middleware.client.Requestor;
import org.example.middleware.transport.http.MethodHTTP;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HeartbeatSender {

    private final HeartbeatSignal signal;
    private final String gatewayHost;
    private final int gatewayPort;
    private final Requestor requestor = new Requestor();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public HeartbeatSender(String serviceName, String nodeHost, int nodePort, String gatewayHost, int gatewayPort) {
        this.signal = new HeartbeatSignal(serviceName, nodeHost, nodePort);
        this.gatewayHost = gatewayHost;
        this.gatewayPort = gatewayPort;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::sendHeartbeat, 0, 2, TimeUnit.SECONDS);
        System.out.printf("[HeartbeatSender] Active for %s@%s:%d -> Gateway at %s:%d%n",
                signal.serviceName(), signal.host(), signal.port(), gatewayHost, gatewayPort);
    }

    private void sendHeartbeat() {
        try {
            requestor.invoke(
                    "UDP",
                    gatewayHost,
                    gatewayPort,
                    "/gateway/heartbeat",
                    MethodHTTP.POST,
                    signal,
                    String.class
            );
        } catch (Exception e) {
            System.err.println("[HeartbeatSender] Failed to send heartbeat: " + e.getMessage());
        }
    }

    public void stop() {
        scheduler.shutdown();
    }
}