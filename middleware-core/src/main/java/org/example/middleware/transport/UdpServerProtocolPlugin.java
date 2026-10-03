package org.example.middleware.transport;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UdpServerProtocolPlugin implements ServerProtocolPlugin {

    private DatagramSocket socket;

    // volatile ensures that changes to this variable are visible to all threads
    private volatile boolean running;
    
    private final ExecutorService threadPool;
    
    private static final int BUFFER_SIZE = 8192;

    public UdpServerProtocolPlugin(int maxConcurrentThreads) {
        this.threadPool = Executors.newFixedThreadPool(maxConcurrentThreads);
    }

    @Override
    public void start(int port, RequestHandler handler) throws Exception {
        if (running) return;
        
        this.socket = new DatagramSocket(port);
        this.running = true;

        System.out.println("[UdpServerPlugin] Listening on port: " + port);

        new Thread(() -> {
            while (running && !socket.isClosed()) {
                try {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    
                    socket.receive(packet);
                    
                    threadPool.submit(() -> handlePacket(packet, handler));
                    
                } catch (Exception e) {
                    if (running) {
                        System.err.println("[UdpServerPlugin] Failed to accept packet: " + e.getMessage());
                    }
                }
            }
        }, "Udp-Listener-Thread").start();
    }

    private void handlePacket(DatagramPacket packet, RequestHandler handler) {
        try {
            byte[] requestBytes = Arrays.copyOf(packet.getData(), packet.getLength());
            byte[] responseBytes = handler.handle(requestBytes);
            
            if (responseBytes != null && responseBytes.length > 0) {
                DatagramPacket responsePacket = new DatagramPacket(
                        responseBytes,
                        responseBytes.length,
                        packet.getAddress(),
                        packet.getPort()
                );
                
                socket.send(responsePacket);
            }
        } catch (Exception e) {
            System.err.println("[UdpServerPlugin] Error while handling remote datagram: " + e.getMessage());
        }
    }

    @Override
    public void stop() {
        this.running = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception e) {
            System.err.println("[UdpServerPlugin] Fail to close socket: " + e.getMessage());
        } finally {
            if (threadPool != null) {
                threadPool.shutdownNow();
            }
            System.out.println("[UdpServerPlugin] Server shutdown.");
        }
    }
}