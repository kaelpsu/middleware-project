package org.example.middleware.interceptor;

public interface InvocationInterceptor {
    void beforeInvocation(InvocationContext context) throws Exception;
    void afterInvocation(InvocationContext context);
}