package org.example.middleware.interceptor;

import java.util.Arrays;

public class LoggingInterceptor implements InvocationInterceptor {

    @Override
    public void beforeInvocation(InvocationContext context) {
        if (context.getTargetMethod().getName().contains("Heartbeat")) return; // avoids heartbeat log storm

        System.out.printf("[LoggingInterceptor] BEFORE -> Route: %s | Method: %s | ObjectID: %s | Arguments: %s%n",
                context.getHttpRequest().getPath(),
                context.getTargetMethod().getName(),
                context.getObjectID().value(),
                Arrays.toString(context.getArguments()));
    }

    @Override
    public void afterInvocation(InvocationContext context) {
        if (context.getTargetMethod().getName().contains("Heartbeat")) return; // avoids heartbeat log storm

        if (context.getException() != null) {
            System.err.printf("[LoggingInterceptor] ERROR  -> Route: %s | Exception: %s%n",
                    context.getHttpRequest().getPath(),
                    context.getException().getMessage());
        } else {
            System.out.printf("[LoggingInterceptor] AFTER   -> Route: %s | Success%n",
                    context.getHttpRequest().getPath());
        }
    }
}