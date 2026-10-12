package org.example.middleware.transport.http;

import java.util.Collections;
import java.util.Map;

public class HttpRequest {

    private final MethodHTTP method;
    private final String path;
    private final Map<String, String> queryParams;
    private final Map<String, String> headers;
    private final String body;

    public HttpRequest(MethodHTTP method, String path, Map<String, String> queryParams, Map<String, String> headers, String body) {
        this.method = method;
        this.path = path;
        this.queryParams = queryParams != null ? queryParams : Collections.emptyMap();
        this.headers = headers != null ? headers : Collections.emptyMap();
        this.body = body != null ? body : "";
    }

    public MethodHTTP getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    @Override
    public String toString() {
        return "HttpRequest{" +
                "method=" + method +
                ", path='" + path + '\'' +
                ", queryParams=" + queryParams +
                ", body='" + body + '\'' +
                '}';
    }
}