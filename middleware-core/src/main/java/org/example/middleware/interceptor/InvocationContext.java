package org.example.middleware.interceptor;

import org.example.middleware.identification.ObjectID;
import org.example.middleware.transport.http.HttpRequest;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class InvocationContext {

    private final ObjectID objectID;
    private final Method targetMethod;
    private final Object[] arguments;
    private final HttpRequest httpRequest;
    private final Map<String, Object> attributes = new HashMap<>();
    private Object responseResult;
    private Throwable exception;

    public InvocationContext(ObjectID objectID, Method targetMethod, Object[] arguments, HttpRequest httpRequest) {
        this.objectID = objectID;
        this.targetMethod = targetMethod;
        this.arguments = arguments;
        this.httpRequest = httpRequest;
    }

    public ObjectID getObjectID() { return objectID; }
    public Method getTargetMethod() { return targetMethod; }
    public Object[] getArguments() { return arguments; }
    public HttpRequest getHttpRequest() { return httpRequest; }

    public Object getResponseResult() { return responseResult; }
    public void setResponseResult(Object responseResult) { this.responseResult = responseResult; }

    public Throwable getException() { return exception; }
    public void setException(Throwable exception) { this.exception = exception; }

    public void setAttribute(String key, Object value) { attributes.put(key, value); }
    public Object getAttribute(String key) { return attributes.get(key); }
}