package org.example.middleware.client;

import org.example.middleware.marshaller.Marshaller;
import org.example.middleware.transport.http.MethodHTTP;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Requestor {

    public <T> T invoke(String protocol, String host, int port, String path, MethodHTTP method, Object requestDTO, Class<T> responseClass) throws Exception {

        String jsonPayload = Marshaller.marshal(requestDTO);
        byte[] bodyBytes = jsonPayload.getBytes(StandardCharsets.UTF_8);

        String rawResponseJson = sendRawSocketRequest(protocol, host, port, path, method.name(), bodyBytes);

        return Marshaller.unmarshal(rawResponseJson, responseClass);
    }

    private String sendRawSocketRequest(String protocol, String host, int port, String path, String httpMethod, byte[] bodyBytes) throws Exception {
        String httpRequest = httpMethod + " " + path + " HTTP/1.1\r\n" +
                "Host: " + host + ":" + port + "\r\n" +
                "Content-Type: application/json\r\n" +
                "Content-Length: " + bodyBytes.length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n";
        byte[] headerBytes = httpRequest.getBytes(StandardCharsets.UTF_8); 
        byte[] fullRequestBytes = new byte[headerBytes.length + bodyBytes.length];

        System.arraycopy(headerBytes, 0, fullRequestBytes, 0, headerBytes.length);
        if (bodyBytes.length > 0) {
            System.arraycopy(bodyBytes, 0, fullRequestBytes, headerBytes.length, bodyBytes.length);
        }

        String rawResponse;

        if ("UDP".equalsIgnoreCase(protocol)) {
            rawResponse = sendUdpRequest(host, port, fullRequestBytes);
        } else {
            rawResponse = sendTcpRequest(host, port, fullRequestBytes);
        }

        return extractBody(rawResponse);
    }

    private String sendTcpRequest(String host, int port, byte[] requestBytes) throws Exception {
        try (Socket socket = new Socket(host, port);
             OutputStream os = socket.getOutputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            os.write(requestBytes);
            os.flush();

            StringBuilder responseBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line).append("\r\n");
            }

            return responseBuilder.toString();
        }
    }

    private String sendUdpRequest(String host, int port, byte[] requestBytes) throws Exception {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(3000);

            InetAddress address = InetAddress.getByName(host);
            DatagramPacket sendPacket = new DatagramPacket(requestBytes, requestBytes.length, address, port);
            socket.send(sendPacket);

            byte[] receiveBuffer = new byte[65535];
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            socket.receive(receivePacket);

            return new String(receivePacket.getData(), 0, receivePacket.getLength(), StandardCharsets.UTF_8);
        }
    }

    private String extractBody(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return "";
        }
        int bodyStartIndex = rawResponse.indexOf("\r\n\r\n");
        if (bodyStartIndex != -1) {
            return rawResponse.substring(bodyStartIndex + 4).trim();
        }
        return rawResponse.trim();
    }
}