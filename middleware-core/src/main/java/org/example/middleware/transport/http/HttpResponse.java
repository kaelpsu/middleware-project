package org.example.middleware.transport.http;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HttpResponse {

    private int statusCode;
    private String statusText;
    private final Map<String, String> headers = new HashMap<>();
    private String body;

    public HttpResponse(int statusCode, String statusText, String body) {
        this.statusCode = statusCode;
        this.statusText = statusText;
        this.body = body != null ? body : "";
        setHeader("Content-Type", "application/json; charset=UTF-8");
        setHeader("Connection", "close");
    }

    public static HttpResponse ok(String jsonBody) {
        return new HttpResponse(200, "OK", jsonBody);
    }

    public static HttpResponse notFound(String message) {
        return new HttpResponse(404, "Not Found", "{\"error\": \"" + message + "\"}");
    }

    public static HttpResponse internalServerError(String message) {
        return new HttpResponse(500, "Internal Server Error", "{\"error\": \"" + message + "\"}");
    }

    public void setHeader(String name, String value) {
        headers.put(name, value);
    }

    public byte[] getBytes() {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        setHeader("Content-Length", String.valueOf(bodyBytes.length));

        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 ").append(statusCode).append(" ").append(statusText).append("\r\n");

        for (Map.Entry<String, String> header : headers.entrySet()) {
            sb.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
        }
        sb.append("\r\n");

        byte[] headerBytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] completeResponse = new byte[headerBytes.length + bodyBytes.length];

        System.arraycopy(headerBytes, 0, completeResponse, 0, headerBytes.length);
        System.arraycopy(bodyBytes, 0, completeResponse, headerBytes.length, bodyBytes.length);

        return completeResponse;
    }
}