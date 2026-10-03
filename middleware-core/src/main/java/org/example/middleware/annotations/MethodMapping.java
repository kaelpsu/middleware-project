package org.example.middleware.annotations;

import org.example.middleware.transport.http.MethodHTTP;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MethodMapping {

    MethodHTTP method() default MethodHTTP.GET;

    String path() default "";

}