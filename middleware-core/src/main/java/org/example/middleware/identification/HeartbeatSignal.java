package org.example.middleware.identification;

public record HeartbeatSignal(String serviceName, String host, int port) {}