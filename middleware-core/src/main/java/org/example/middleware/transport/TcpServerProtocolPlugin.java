package org.example.middleware.transport;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class TcpServerProtocolPlugin implements ServerProtocolPlugin {

    private ServerSocket serverSocket;
    private volatile boolean running;
    
    private final ExecutorService threadPool;

    private static final int BUFFER_SIZE = 8192;

    public TcpServerProtocolPlugin(int maxConcurrentThreads) {
        this.threadPool = Executors.newFixedThreadPool(maxConcurrentThreads);
    }

    @Override
    public void start(int port, RequestHandler handler) throws Exception {
        if (running) return;
        
        this.serverSocket = new ServerSocket(port);
        this.running = true;

        System.out.println("[TcpServerPlugin] Listening on port: " + port);
        
        new Thread(() -> {
            while (running && !serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    threadPool.submit(() -> handleClient(clientSocket, handler));
                } catch (Exception e) {
                    if (running) {
                        System.err.println("[TcpServerPlugin] Fail to accept connection: " + e.getMessage());
                    }
                }
            }
        }, "Tcp-Acceptor-Thread").start();
    }

    private void handleClient(Socket socket, RequestHandler handler) {
        try (socket;
             InputStream in = socket.getInputStream();
             OutputStream out = socket.getOutputStream()) {
            
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead = in.read(buffer);
            
            if (bytesRead > 0) {
                byte[] requestBytes = Arrays.copyOf(buffer, bytesRead);
                byte[] responseBytes = handler.handle(requestBytes);
                
                if (responseBytes != null && responseBytes.length > 0) {
                    out.write(responseBytes);
                    out.flush();
                }
            }
        } catch (Exception e) {
            System.err.println("[TcpServerPlugin] Error while handling remote client: " + e.getMessage());
        }
    }

    @Override
    public void stop() {
        this.running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception e) {
            System.err.println("[TcpServerPlugin] Fail to close socket: " + e.getMessage());
        } finally {
            if (threadPool != null) {
                threadPool.shutdownNow();
            }
            System.out.println("[TcpServerPlugin] Server shutdown.");
        }
    }
}