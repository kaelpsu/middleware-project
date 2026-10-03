package org.example.middleware.transport;

public interface ServerProtocolPlugin {
    void start(int port, RequestHandler handler) throws Exception;
    void stop();
}