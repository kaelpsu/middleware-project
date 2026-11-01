package org.example.app;

import org.example.app.gateway.GatewayService;
import org.example.app.service.OrderService;
import org.example.app.service.TaxService;
import org.example.middleware.broker.Invoker;
import org.example.middleware.identification.HeartbeatSender;
import org.example.middleware.interceptor.LoggingInterceptor;
import org.example.middleware.transport.ServerRequestHandler;
import org.example.middleware.transport.TcpServerProtocolPlugin;
import org.example.middleware.transport.UdpServerProtocolPlugin;

public class MainApp {

    public static void main(String[] args) {
        String nodeType = parseArg(args, "--node", "gateway");
        String gatewayHost = parseArg(args, "--gateway-host", "127.0.0.1");
        String hostIp = parseArg(args, "--host", "127.0.0.1");
        
        int tcpPort = 8080;
        int udpPort = 9090;

        try {
            switch (nodeType.toLowerCase()) {
                case "gateway" -> startGatewayNode(tcpPort, udpPort);
                case "order" -> startBackendNode("order", OrderService.class, hostIp, tcpPort, udpPort, gatewayHost);
                case "tax" -> startBackendNode("tax", TaxService.class, hostIp, tcpPort, udpPort, gatewayHost);
                default -> throw new IllegalArgumentException("Unknown node Type: " + nodeType);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void startGatewayNode(int tcpPort, int udpPort) throws Exception {
        Invoker invoker = createConfiguredInvoker();
        invoker.registerService(GatewayService.class);

        TcpServerProtocolPlugin tcpServer = new TcpServerProtocolPlugin(100);
        UdpServerProtocolPlugin udpServer = new UdpServerProtocolPlugin(100);

        ServerRequestHandler handler = new ServerRequestHandler(invoker);

        tcpServer.start(tcpPort, handler);
        udpServer.start(udpPort, handler);

        System.out.printf(">>> API Gateway active on TCP port %d and UDP port %d <<<%n", tcpPort, udpPort);
    }

    private static void startBackendNode(String serviceName, Class<?> serviceClass, String hostIp, int tcpPort, int udpPort, String gatewayHost) throws Exception {
        System.setProperty("gateway.host", gatewayHost);
        System.setProperty("gateway.tcp.port", Integer.toString(tcpPort));

        Invoker invoker = createConfiguredInvoker();
        invoker.registerService(serviceClass);

        TcpServerProtocolPlugin tcpServer = new TcpServerProtocolPlugin(100);
        UdpServerProtocolPlugin udpServer = new UdpServerProtocolPlugin(100);

        ServerRequestHandler handler = new ServerRequestHandler(invoker);

        tcpServer.start(tcpPort, handler);
        udpServer.start(udpPort, handler);
        System.out.printf(">>> Node [%s] active on %s (TCP: %d, UDP: %d) <<<%n", serviceName, hostIp, tcpPort, udpPort);

        HeartbeatSender heartbeatSender = new HeartbeatSender(serviceName, hostIp, tcpPort, gatewayHost, udpPort);
        heartbeatSender.start();
    }

    private static Invoker createConfiguredInvoker() {
        Invoker invoker = new Invoker();
        invoker.addInterceptor(new LoggingInterceptor());
        return invoker;
    }

    private static String parseArg(String[] args, String flag, String defaultValue) {
        for (String arg : args) {
            if (arg.startsWith(flag + "=")) {
                return arg.substring((flag + "=").length());
            }
        }
        return defaultValue;
    }
}