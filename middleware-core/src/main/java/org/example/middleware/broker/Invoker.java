package org.example.middleware.broker;

import org.example.middleware.annotations.controller.Body;
import org.example.middleware.annotations.controller.MethodMapping;
import org.example.middleware.annotations.controller.Param;
import org.example.middleware.annotations.controller.RequestMapping;
import org.example.middleware.annotations.lifecycle.ClientDependentInstance;
import org.example.middleware.annotations.lifecycle.PerRequestInstance;
import org.example.middleware.annotations.lifecycle.StaticInstance;
import org.example.middleware.identification.ObjectID;
import org.example.middleware.interceptor.InvocationContext;
import org.example.middleware.interceptor.InvocationInterceptor;
import org.example.middleware.lifecycle.ClientDependentLifecycleManager;
import org.example.middleware.lifecycle.LifecycleManager;
import org.example.middleware.lifecycle.PerRequestLifecycleManager;
import org.example.middleware.lifecycle.StaticLifecycleManager;
import org.example.middleware.marshaller.Marshaller;
import org.example.middleware.transport.http.HttpRequest;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Invoker {

    private final Map<Class<?>, LifecycleManager> lifecycleRegistry = new ConcurrentHashMap<>();
    private final Map<String, Class<?>> routeToClassMap = new ConcurrentHashMap<>();
    private final List<InvocationInterceptor> interceptors = new CopyOnWriteArrayList<>();
    
    public void addInterceptor(InvocationInterceptor interceptor) {
        this.interceptors.add(interceptor);
    }

    public void registerService(Class<?> serviceClass) {
        if (serviceClass == null || lifecycleRegistry.containsKey(serviceClass)) {
            return;
        }

        String basePath = extractBasePath(serviceClass);

        LifecycleManager manager;

        if (serviceClass.isAnnotationPresent(ClientDependentInstance.class)) {
            ClientDependentInstance ann = serviceClass.getAnnotation(ClientDependentInstance.class);
            manager = new ClientDependentLifecycleManager(serviceClass, ann.leaseTimeoutMs());
            
        } else if (serviceClass.isAnnotationPresent(PerRequestInstance.class)) {
            PerRequestInstance ann = serviceClass.getAnnotation(PerRequestInstance.class);
            manager = new PerRequestLifecycleManager(serviceClass, ann.pooled(), ann.poolSize());
            
        } else {
            boolean isLazy = true; 
            if (serviceClass.isAnnotationPresent(StaticInstance.class)) {
                isLazy = serviceClass.getAnnotation(StaticInstance.class).lazy();
            }

            manager = new StaticLifecycleManager(serviceClass, isLazy);
        }

        lifecycleRegistry.put(serviceClass, manager);
        routeToClassMap.put(basePath, serviceClass);

        System.out.println("[Invoker] Class " + serviceClass.getSimpleName() + " registered with manager " + manager.getClass().getSimpleName());
    }

    public Object dispatch(HttpRequest request) throws Exception {
        String normalizedRequestPath = normalizePath(request.getPath());

        String[] pathParts = normalizedRequestPath.split("/");

        String basePath = pathParts.length > 1 ? "/" + pathParts[1] : normalizedRequestPath;

        Class<?> targetClass = routeToClassMap.get(basePath);

        if (targetClass == null) {
            throw new NoSuchMethodException("Couldn't find service for route: " + request.getMethod() + ":" + normalizedRequestPath);
        }

        LifecycleManager lifecycleManager = lifecycleRegistry.get(targetClass);

        String rawTargetID = request.getHeaders().get("X-Object-ID");
        ObjectID targetID = new ObjectID((rawTargetID != null && !rawTargetID.isBlank()) ? rawTargetID : basePath);

        for (Method method : targetClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(MethodMapping.class)) {
                MethodMapping mapping = method.getAnnotation(MethodMapping.class);

                if (mapping.method() == request.getMethod()) {
                    String fullPath = normalizePath(basePath + "/" + mapping.path());

                    if (fullPath.equals(normalizedRequestPath)) {
                        Object[] args = extractArguments(method, request.getQueryParams(), request.getBody());

                        InvocationContext context = new InvocationContext(targetID, method, args, request);
                    
                        for (InvocationInterceptor interceptor : interceptors) {
                            interceptor.beforeInvocation(context);
                        }
                        
                        Object targetInstance = lifecycleManager.acquire(targetID);
                                                
                        try {
                            method.setAccessible(true);
                        
                            Object result = method.invoke(targetInstance, args);
                            context.setResponseResult(result);

                        } catch (Exception e) {
                            context.setException(e);
                        } 
                        finally {
                            lifecycleManager.release(targetID, targetInstance);

                            for (int i = interceptors.size() - 1; i >= 0; i--) {
                                interceptors.get(i).afterInvocation(context);
                            }
                        }

                        if (context.getException() != null) {
                            throw new Exception(context.getException().getCause() != null ? context.getException().getCause() : context.getException());
                        }

                        return context.getResponseResult();
                    }
                }
            }
        }

        throw new NoSuchMethodException("Couldn't find any methods for route: " + request.getMethod() + ":" + normalizedRequestPath);
    }

    private Object[] extractArguments(Method method, Map<String, String> queryParams, String bodyPayload) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];

            if (parameter.isAnnotationPresent(Param.class)) {
                Param paramAnnotation = parameter.getAnnotation(Param.class);
                String paramName = paramAnnotation.name();
                String rawValue = queryParams != null ? queryParams.get(paramName) : null;

                args[i] = Marshaller.unmarshal(rawValue, parameter.getType());

            } else if (parameter.isAnnotationPresent(Body.class)) {
                args[i] = Marshaller.unmarshal(bodyPayload, parameter.getType());
            } else {
                args[i] = null;
            }
        }

        return args;
    }

    private String extractBasePath(Class<?> clazz) {
        if (clazz.isAnnotationPresent(RequestMapping.class)) {
            String path = clazz.getAnnotation(RequestMapping.class).path();
            return path.startsWith("/") ? path : "/" + path;
        }
        return "/" + clazz.getSimpleName().toLowerCase();
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        String clean = path.trim().replaceAll("//+", "/");
        if (!clean.startsWith("/")) {
            clean = "/" + clean;
        }
        if (clean.length() > 1 && clean.endsWith("/")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        return clean;
    }
}