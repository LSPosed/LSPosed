package com.swift.sandhook.wrapper;

public class HookErrorException extends Exception {
    public HookErrorException(String s) {
        super(s);
    }

    public HookErrorException(String message, Throwable cause) {
        super(message, cause);
    }
}
