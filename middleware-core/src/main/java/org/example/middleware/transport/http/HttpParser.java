package org.example.middleware.transport.http;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HttpParser {

    public static HttpRequest parse(InputStream inputStream) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        String requestLine = reader.readLine();
        if (requestLine == null || requestLine.isBlank()) {
            return null;
        }

        // ex: "GET /calc/sum?a=10&b=20 HTTP/1.1"
        String[] requestLineParts = requestLine.split(" ");
        if (requestLineParts.length < 2) {
            throw new IllegalArgumentException("Invalid HTTP request: " + requestLine);
        }

        MethodHTTP method = MethodHTTP.valueOf(requestLineParts[0].toUpperCase());
        String fullUri = requestLineParts[1];

        String path = fullUri;
        Map<String, String> queryParams = new HashMap<>();

        if (fullUri.contains("?")) {
            String[] uriParts = fullUri.split("\\?", 2);
            path = uriParts[0];
            parseQueryParams(uriParts[1], queryParams);
        }


        Map<String, String> headers = new HashMap<>();
        String headerLine;
        int contentLength = 0;

        while ((headerLine = reader.readLine()) != null && !headerLine.isBlank()) {
            int colonIndex = headerLine.indexOf(":");
            if (colonIndex > 0) {
                String headerName = headerLine.substring(0, colonIndex).trim();
                String headerValue = headerLine.substring(colonIndex + 1).trim();
                headers.put(headerName.toLowerCase(), headerValue);

                if ("content-length".equalsIgnoreCase(headerName)) {
                    contentLength = Integer.parseInt(headerValue);
                }
            }
        }

        StringBuilder bodyBuilder = new StringBuilder();
        if (contentLength > 0) {
            char[] bodyChars = new char[contentLength];
            int bytesRead = 0;
            while (bytesRead < contentLength) {
                int read = reader.read(bodyChars, bytesRead, contentLength - bytesRead);
                if (read == -1) break;
                bytesRead += read;
            }
            bodyBuilder.append(bodyChars, 0, bytesRead);
        }

        return new HttpRequest(method, path, queryParams, headers, bodyBuilder.toString());
    }

    private static void parseQueryParams(String queryString, Map<String, String> queryParams) {
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                String value = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                queryParams.put(key, value);
            } else if (keyValue.length == 1) {
                String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                queryParams.put(key, "");
            }
        }
    }
}