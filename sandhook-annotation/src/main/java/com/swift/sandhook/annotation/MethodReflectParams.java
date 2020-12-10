package com.swift.sandhook.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD,ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MethodReflectParams {

    String BOOLEAN = "boolean";
    String BYTE = "byte";
    String CHAR = "char";
    String DOUBLE = "double";
    String FLOAT = "float";
    String INT = "int";
    String LONG = "long";
    String SHORT = "short";

    String[] value();
}