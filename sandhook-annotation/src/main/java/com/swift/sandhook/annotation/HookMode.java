package com.swift.sandhook.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface HookMode {

    int AUTO = 0;
    int INLINE = 1;
    int REPLACE = 2;

    int value() default AUTO;
}
