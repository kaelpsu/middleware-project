package org.example.middleware.transport;

@FunctionalInterface
public interface RequestHandler {
    byte[] handle(byte[] requestBytes);
}